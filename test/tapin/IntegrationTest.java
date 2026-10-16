package tapin;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import tapin.dao.*;
import tapin.model.*;
import tapin.service.*;
import tapin.util.*;

/** Real JDBC integration tests. Requires a fresh schema whose name starts tapin_test. */
public final class IntegrationTest {
    private static int passed;
    private static final ManagementService management=new ManagementService();
    private static final LocalDate DAY=LocalDate.of(2026,9,28);
    private static Account admin,teacher,other;
    private static int section,subject,schedule,student,student2,student3;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);System.out.println("PASS "+(++passed)+": "+label);}
    @FunctionalInterface interface Action{void run()throws Exception;}
    private static void rejects(String label,Action action)throws Exception{try{action.run();}catch(IllegalArgumentException|SQLException expected){check(true,label);return;}throw new AssertionError("Expected rejection: "+label);}
    private static AttendanceService at(String time){return new AttendanceService(Clock.fixed(DAY.atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant(),ZoneId.systemDefault()));}
    private static Map<String,Object> values(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    private static Map<String,Object> studentData(String no,String uid,String name){return values("student_no",no,"rfid_uid",uid,"first_name",name,"last_name","Test","section_id",section,"active",true);}
    private static Map<String,Object> scheduleData(int sec,int teacherId,String start,String end){return values("section_id",sec,"subject_id",subject,"teacher_id",teacherId,"day_of_week","MONDAY","start_time",start,"end_time",end,"grace_minutes",10);}
    public static void main(String[] args)throws Exception{
        try(Connection c=Database.open()){
            if(!c.getCatalog().startsWith("tapin_test"))throw new IllegalStateException("Use a dedicated tapin_test schema.");
            check(c.isValid(3),"database connection");
            if(Sql.id(Sql.one(c,"SELECT COUNT(*) n FROM accounts"),"n")!=0)throw new IllegalStateException("Use a fresh test schema.");
            int id=Sql.insert(c,"INSERT INTO accounts(username,password,full_name,role) VALUES ('admin',?,'Test Admin','ADMIN')",Passwords.store("AdminPass!".toCharArray()));admin=new Account(id,"Test Admin","ADMIN");
        }
        int teacherId=management.save(admin,Entity.ACCOUNTS,0,values("username","teacher","password","TeacherPass!".toCharArray(),"full_name","Test Teacher","role","TEACHER"));
        int otherId=management.save(admin,Entity.ACCOUNTS,0,values("username","other","password","OtherPass!".toCharArray(),"full_name","Other Teacher","role","TEACHER"));
        AuthService auth=new AuthService();
        teacher=auth.login("teacher","TeacherPass!".toCharArray(),"TEACHER");other=auth.login("other","OtherPass!".toCharArray(),"TEACHER");
        check(teacher.id()==teacherId,"teacher login");check(auth.login("admin","AdminPass!".toCharArray(),"ADMIN").admin(),"administrator login");
        rejects("wrong credentials",()->auth.login("teacher","wrong".toCharArray(),"TEACHER"));
        rejects("wrong role",()->auth.login("teacher","TeacherPass!".toCharArray(),"ADMIN"));
        rejects("empty username",()->auth.login("","TeacherPass!".toCharArray(),"TEACHER"));
        rejects("empty password",()->auth.login("teacher",new char[0],"TEACHER"));
        section=management.save(admin,Entity.SECTIONS,0,values("section_name","TEST-A","course","BSIT","year_level","2"));
        subject=management.save(admin,Entity.SUBJECTS,0,values("subject_code","TEST101","subject_name","Testing"));
        schedule=management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"08:00","09:00"));
        check(management.list(admin,Entity.SCHEDULES).size()==1,"section / subject / schedule create and read");
        rejects("schedule overlap",()->management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"08:30","09:30")));
        rejects("invalid time range",()->management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"09:00","08:00")));
        student=management.save(admin,Entity.STUDENTS,0,studentData("TEST-1","000111","Alex"));
        student2=management.save(admin,Entity.STUDENTS,0,studentData("TEST-2","000222","Jamie"));
        student3=management.save(admin,Entity.STUDENTS,0,studentData("TEST-3","000333","Sam"));
        check(management.list(admin,Entity.STUDENTS).size()==3,"student create and read");
        management.save(admin,Entity.STUDENTS,student,studentData("TEST-1","000111","Alex Updated"));
        check(management.list(admin,Entity.STUDENTS).stream().anyMatch(r->"Alex Updated".equals(r.get("first_name"))),"student update");
        rejects("duplicate RFID validation",()->management.save(admin,Entity.STUDENTS,0,studentData("TEST-4","000111","Duplicate")));
        rejects("duplicate student number",()->management.save(admin,Entity.STUDENTS,0,studentData("TEST-1","000444","Duplicate")));
        rejects("teacher cannot manage students",()->management.save(teacher,Entity.STUDENTS,0,studentData("TEST-4","000444","Denied")));
        rejects("unknown RFID",()->at("08:00").tap("999999"));
        rejects("tap with no active session",()->at("08:00").tap("000111"));
        rejects("wrong teacher cannot start",()->at("08:00").start(other,schedule,DAY));
        int session=at("08:00").start(teacher,schedule,DAY);check(session>0,"start session");
        rejects("already-open session",()->at("08:00").start(teacher,schedule,DAY));
        check(at("08:10:00").tap("000111").status().equals("ON_TIME"),"grace boundary is ON_TIME");
        check(at("08:10:01").tap("000111").action().contains("wait 3"),"duplicate HID scan debounce");
        check(at("08:10:01").tap("000222").status().equals("LATE"),"one second after grace is LATE");
        check(at("08:11").tap("000111").action().contains("Time Out"),"second valid tap Time Out");
        check(at("08:12").tap("000111").action().contains("already completed"),"completed tap does not mutate record");
        rejects("wrong teacher cannot correct",()->at("08:20").manual(other,session,student2,DAY.atTime(8,5),null,"Missed tap"));
        at("08:20").manual(teacher,session,student2,DAY.atTime(8,5),null,"Card unreadable");
        try(Connection c=Database.open()){var r=Sql.one(c,"SELECT * FROM attendance_records WHERE session_id=? AND student_id=?",session,student2);check(r.get("source").equals("MANUAL")&&r.get("status").equals("ON_TIME"),"manual correction source, status and remarks");}
        rejects("invalid manual Time Out",()->at("08:20").manual(teacher,session,student2,DAY.atTime(8,5),DAY.atTime(8,0),"Invalid"));
        rejects("future correction",()->at("08:20").manual(teacher,session,student2,DAY.atTime(9,0),null,"Future"));
        rejects("correction requires reason",()->at("08:20").manual(teacher,session,student2,DAY.atTime(8,5),null,""));
        rejects("wrong teacher cannot close",()->at("09:00").close(other,session));
        at("09:00").close(teacher,session);
        try(Connection c=Database.open()){
            check(Sql.one(c,"SELECT status,closed_at FROM attendance_sessions WHERE session_id=?",session).get("closed_at")!=null,"close timestamp saved");
            check(Sql.one(c,"SELECT status FROM attendance_records WHERE session_id=? AND student_id=?",session,student3).get("status").equals("ABSENT"),"automatic ABSENT creation");
            check(Sql.id(Sql.one(c,"SELECT COUNT(*) n FROM attendance_records WHERE session_id=?",session),"n")==3,"one attendance record per student per session");
        }
        rejects("tap into closed session",()->at("09:01").tap("000333"));rejects("session already closed",()->at("09:01").close(teacher,session));
        at("09:10").manual(teacher,session,student3,DAY.atTime(8,2),DAY.atTime(9,0),"Teacher verified missed scan");check(true,"closed-session manual correction");
        AttendanceDao dao=new AttendanceDao();check(dao.reports(teacher,DAY,section,subject,"Alex").size()==1,"date / section / subject / student filter");
        check(dao.reports(other,null,0,0,"").isEmpty(),"reports restricted to assigned teacher");
        check(dao.reports(admin,null,0,0,"").size()==3,"administrator reports across system");
        rejects("historical schedule protected",()->management.save(admin,Entity.SCHEDULES,schedule,scheduleData(section,teacherId,"09:00","10:00")));
        management.delete(admin,Entity.STUDENTS,student3);check(management.list(admin,Entity.STUDENTS).stream().anyMatch(r->Sql.id(r,"student_id")==student3&&(Boolean.FALSE.equals(r.get("active"))||"0".equals(r.get("active").toString()))),"student safely deactivated");
        check(dao.reports(admin,DAY,section,subject,"Sam").size()==1,"history retained after deactivation");
        check(dao.roster(teacher,schedule,session).size()==3,"closed roster retains deactivated student's historical attendance");
        int addedLater=management.save(admin,Entity.STUDENTS,0,studentData("TEST-LATER","009999","New enrollment"));
        check(dao.roster(teacher,schedule,session).size()==3,"new enrollment does not appear as pending in old closed class");
        management.delete(admin,Entity.STUDENTS,addedLater);
        // Independent classes can be open together; RFID must route by section.
        int sec2=management.save(admin,Entity.SECTIONS,0,values("section_name","TEST-B","course","BSIT","year_level","2"));
        int sch2=management.save(admin,Entity.SCHEDULES,0,scheduleData(sec2,otherId,"08:00","09:00"));
        int sch3=management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"09:00","10:00"));
        int s2=at("09:15").start(other,sch2,DAY),s3=at("09:15").start(teacher,sch3,DAY);
        check(at("09:16").tap("000111").action().contains("Time In"),"RFID routes to student's section with two open classes");
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            var tasks=java.util.List.<Callable<AttendanceService.TapResult>>of(()->at("09:17").tap("000222"),()->at("09:17").tap("000222"));
            for(Future<?> f:pool.invokeAll(tasks))f.get();
            try(Connection c=Database.open()){check(Sql.id(Sql.one(c,"SELECT COUNT(*) n FROM attendance_records WHERE session_id=? AND student_id=?",s3,student2),"n")==1,"concurrent taps cannot duplicate records");}
        }finally{pool.shutdown();}
        at("10:00").close(teacher,s3);at("10:00").close(other,s2);
        check(dao.roster(teacher,schedule,s2).isEmpty(),"roster rejects mismatched schedule and session");
        int concurrentA=management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"10:00","11:00"));
        int concurrentB=management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"11:00","12:00"));
        ExecutorService starts=Executors.newFixedThreadPool(2);
        try {
            java.util.List<Callable<Integer>> attempts=java.util.List.of(()->{try{return at("10:10").start(teacher,concurrentA,DAY);}catch(SQLException|IllegalArgumentException e){return 0;}},()->{try{return at("10:10").start(teacher,concurrentB,DAY);}catch(SQLException|IllegalArgumentException e){return 0;}});
            int opened=0,openedId=0;for(Future<Integer> result:starts.invokeAll(attempts)){int id=result.get();if(id>0){opened++;openedId=id;}}
            check(opened==1,"concurrent starts allow only one open session per section");at("12:00").close(teacher,openedId);
        } finally {starts.shutdown();}
        int disposable=management.save(admin,Entity.SUBJECTS,0,values("subject_code","DELETE","subject_name","Disposable"));management.save(admin,Entity.SUBJECTS,disposable,values("subject_code","EDITED","subject_name","Edited"));management.delete(admin,Entity.SUBJECTS,disposable);check(true,"subject update and delete");
        int spare=management.save(admin,Entity.SECTIONS,0,values("section_name","SPARE","course","BSIT","year_level","2"));management.save(admin,Entity.SECTIONS,spare,values("section_name","SPARE EDIT","course","BSIT","year_level","3"));management.delete(admin,Entity.SECTIONS,spare);check(true,"section update and delete");
        int temp=management.save(admin,Entity.ACCOUNTS,0,values("username","temp","password","TempPass!".toCharArray(),"full_name","Temp","role","TEACHER"));management.save(admin,Entity.ACCOUNTS,temp,values("username","temp","password",new char[0],"full_name","Temp Edited","role","TEACHER"));check(auth.login("temp","TempPass!".toCharArray(),"TEACHER").fullName().equals("Temp Edited"),"account update retains blank password");management.delete(admin,Entity.ACCOUNTS,temp);check(true,"account delete");
        int spareSchedule=management.save(admin,Entity.SCHEDULES,0,scheduleData(section,teacherId,"12:00","13:00"));management.save(admin,Entity.SCHEDULES,spareSchedule,scheduleData(section,teacherId,"13:00","14:00"));management.delete(admin,Entity.SCHEDULES,spareSchedule);check(true,"unused schedule update and delete");
        rejects("foreign key protects history",()->management.delete(admin,Entity.SUBJECTS,subject));
        Path csv=Path.of("build/test-export.csv");Csv.write(csv,List.of("Name","Remarks"),List.of(Arrays.asList("Reyes, Alex","A \"quote\"\nnext line"),Arrays.asList("=1+1","Unicode: JosÃƒÂ©")));
        String content=Files.readString(csv);check(content.contains("\"Reyes, Alex\"")&&content.contains("\"\"quote\"\"")&&content.contains("'=1+1")&&content.contains("JosÃƒÂ©"),"UTF-8 CSV escaping and formula protection");
        check(Passwords.store("SamePassword!".toCharArray()).equals("SamePassword!"),"password stored as editable plain text");
        try(Connection c=Database.open()) {Sql.update(c,"UPDATE accounts SET username=?,password=? WHERE account_id=?","renamed_teacher","EditedInPhp!",teacher.id());}
        check(auth.login("renamed_teacher","EditedInPhp!".toCharArray(),"TEACHER").id()==teacher.id(),"direct database username and password edit works");
        rejects("password comparison is case sensitive",()->auth.login("renamed_teacher","editedinphp!".toCharArray(),"TEACHER"));
        System.out.println("ALL "+passed+" INTEGRATION CHECKS PASSED");
    }
}

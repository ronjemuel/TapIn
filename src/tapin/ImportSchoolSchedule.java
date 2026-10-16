package tapin;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import tapin.dao.Sql;
import tapin.util.Database;

/** Imports only the timetable transcribed from the supplied registration PDF. */
public final class ImportSchoolSchedule {
    public static void main(String[] args) throws Exception {
        List<String[]> meetings = Files.readAllLines(Path.of("data/bsit21007-schedule.tsv"))
                .stream().skip(1).filter(s -> !s.isBlank()).map(s -> s.split("\t", -1)).toList();
        if (!Arrays.asList(args).contains("--apply")) {
            for (String[] m : meetings) System.out.println(m[0]+" | "+m[3]+" | "+m[4]+" "+m[5]+"-"+m[6]);
            System.out.println("Preview only. Use --apply to import. Close TapIn before applying.");
            return;
        }
        boolean replaceDemo = Arrays.asList(args).contains("--replace-unused-demo");
        Database.transaction(c -> {
            Map<String,Object> section = Sql.one(c,"SELECT * FROM sections WHERE section_name='BSIT 21007' FOR UPDATE");
            int sectionId = section == null ? Sql.insert(c,"INSERT INTO sections(section_name,course,year_level) VALUES ('BSIT 21007','BSIT','2')") : Sql.id(section,"section_id");
            if (Sql.one(c,"SELECT a.session_id FROM attendance_sessions a JOIN class_schedules s ON s.schedule_id=a.schedule_id WHERE s.section_id=? AND a.status='OPEN'",sectionId) != null)
                throw new IllegalArgumentException("Close this section's open attendance sessions before importing.");

            Map<String,Integer> teachers = new HashMap<>(), subjects = new HashMap<>();
            int createdTeachers=0,createdSubjects=0,insertedMeetings=0,reusedMeetings=0;
            for (String[] m : meetings) {
                if (m.length != 9) throw new IllegalArgumentException("Invalid timetable row.");
                if (!teachers.containsKey(m[2])) {
                    Map<String,Object> teacher = Sql.one(c,"SELECT account_id,username,full_name,role FROM accounts WHERE username=? OR full_name=? ORDER BY account_id",m[2],m[3]);
                    if (teacher != null && (!"TEACHER".equals(teacher.get("role")) ||
                            !(m[3].equals(teacher.get("full_name")) || (m[2].equals("teacher") && Set.of("Romel Cabiling","Demo Teacher").contains(teacher.get("full_name"))))))
                        throw new IllegalArgumentException("Username already belongs to another person: "+m[2]);
                    int teacherId;
                    if (teacher == null) {
                        teacherId=Sql.insert(c,"INSERT INTO accounts(username,password,full_name,role) VALUES (?,'',?,'TEACHER')",m[2],m[3]);
                        createdTeachers++;
                    } else teacherId=Sql.id(teacher,"account_id");
                    teachers.put(m[2],teacherId);
                }
                if (!subjects.containsKey(m[0])) {
                    Map<String,Object> subject=Sql.one(c,"SELECT subject_id FROM subjects WHERE subject_code=?",m[0]);
                    int subjectId;
                    if (subject==null) {subjectId=Sql.insert(c,"INSERT INTO subjects(subject_code,subject_name) VALUES (?,?)",m[0],m[1]);createdSubjects++;}
                    else {subjectId=Sql.id(subject,"subject_id");Sql.update(c,"UPDATE subjects SET subject_name=? WHERE subject_id=?",m[1],subjectId);}
                    subjects.put(m[0],subjectId);
                }
            }

            // Only the observed unused prototype rows can be repurposed. History is excluded.
            List<Integer> reusable = new ArrayList<>();
            if (replaceDemo) {
                for (Map<String,Object> row : Sql.rows(c,"SELECT s.schedule_id FROM class_schedules s JOIN subjects sub ON sub.subject_id=s.subject_id JOIN accounts ac ON ac.account_id=s.teacher_id WHERE s.section_id=? AND sub.subject_code='IPT101' AND ac.username='teacher' AND ((s.start_time='17:03:00' AND s.end_time='23:59:00') OR (s.day_of_week='MONDAY' AND s.start_time='06:00:00' AND s.end_time='07:59:00')) AND NOT EXISTS (SELECT 1 FROM attendance_sessions a WHERE a.schedule_id=s.schedule_id) ORDER BY s.schedule_id FOR UPDATE",sectionId))
                    reusable.add(Sql.id(row,"schedule_id"));
            }
            Set<Integer> matched=new HashSet<>();
            for (String[] m : meetings) {
                int subjectId=subjects.get(m[0]),teacherId=teachers.get(m[2]);
                Time start=Time.valueOf(LocalTime.parse(m[5])),end=Time.valueOf(LocalTime.parse(m[6]));
                DayOfWeek.valueOf(m[4]);
                if (!end.after(start)) throw new IllegalArgumentException("Invalid meeting time: "+m[0]);
                Map<String,Object> existing=Sql.one(c,"SELECT schedule_id FROM class_schedules WHERE section_id=? AND subject_id=? AND teacher_id=? AND day_of_week=? AND start_time=? AND end_time=?",sectionId,subjectId,teacherId,m[4],start,end);
                if (existing!=null) {matched.add(Sql.id(existing,"schedule_id"));continue;}
                if (reusable.isEmpty()) {
                    matched.add(Sql.insert(c,"INSERT INTO class_schedules(section_id,subject_id,teacher_id,day_of_week,start_time,end_time,grace_minutes) VALUES (?,?,?,?,?,?,10)",sectionId,subjectId,teacherId,m[4],start,end));
                    insertedMeetings++;
                } else {
                    int id=reusable.remove(0);
                    Sql.update(c,"UPDATE class_schedules SET subject_id=?,teacher_id=?,day_of_week=?,start_time=?,end_time=?,grace_minutes=10 WHERE schedule_id=?",subjectId,teacherId,m[4],start,end,id);
                    matched.add(id);reusedMeetings++;
                }
            }
            // Abort rather than introduce conflicting meetings. Adjacent meetings are allowed.
            for(Map<String,Object> pair:Sql.rows(c,"SELECT a.schedule_id AS a_id,b.schedule_id AS b_id FROM class_schedules a JOIN class_schedules b ON a.schedule_id<b.schedule_id AND a.day_of_week=b.day_of_week AND (a.section_id=b.section_id OR a.teacher_id=b.teacher_id) AND a.start_time<b.end_time AND a.end_time>b.start_time")) {
                if(matched.contains(Sql.id(pair,"a_id"))||matched.contains(Sql.id(pair,"b_id")))
                    throw new IllegalArgumentException("An existing timetable conflicts with this import. Review schedule IDs "+pair.get("a_id")+" and "+pair.get("b_id")+".");
            }
            if(matched.size()!=15)throw new IllegalStateException("Expected 15 distinct weekly meetings.");
            System.out.println("Validated 15 meetings / 8 subjects / 8 teachers for BSIT 21007.");
            System.out.println("Created teachers: "+createdTeachers+"; created subjects: "+createdSubjects+"; new meetings: "+insertedMeetings+"; repurposed unused demo meetings: "+reusedMeetings);
            return null;
        });
        System.out.println("Import committed. Existing passwords and attendance history were preserved. Set passwords for new teacher accounts before login.");
    }
}

package tapin;

import java.io.Console;
import java.sql.*;
import java.time.*;
import java.util.Arrays;
import tapin.dao.Sql;
import tapin.util.*;

/** Run once after importing SQL; refuses to seed a database with existing accounts. */
public final class Setup {
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !(args[0].equals("--demo")||args[0].equals("--admin"))) {
            System.out.println("Usage: java -cp \"dist/TapIn.jar;lib/*\" tapin.Setup --demo OR --admin");return;
        }
        boolean demo=args[0].equals("--demo");String username="admin",name="TapIn Administrator";char[] password;
        if(demo) password="TapInAdmin!2026".toCharArray();
        else {
            Console console=System.console();if(console==null)throw new IllegalStateException("Run --admin in a terminal so the password can be entered securely.");
            username=console.readLine("Admin username: ").trim();name=console.readLine("Full name: ").trim();password=console.readPassword("Password (8+ characters): ");
        }
        if(username.isBlank()||username.length()>30||name.isBlank()||name.length()>100)throw new IllegalArgumentException("Invalid username or full name.");
        String storedPassword;try{storedPassword=Passwords.store(password);}finally{Arrays.fill(password,'\0');}
        String user=username,fullName=name;
        Database.transaction(c->{
            if(Sql.id(Sql.one(c,"SELECT COUNT(*) AS n FROM accounts"),"n")!=0)throw new IllegalArgumentException("Setup only works on a database with no accounts. Use Manage Accounts instead.");
            Sql.insert(c,"INSERT INTO accounts(username,password,full_name,role) VALUES (?,?,?,'ADMIN')",user,storedPassword,fullName);
            if(demo) {
                int teacher=Sql.insert(c,"INSERT INTO accounts(username,password,full_name,role) VALUES (?,?,?,'TEACHER')","teacher",Passwords.store("TapInTeacher!2026".toCharArray()),"Demo Teacher");
                int section=Sql.insert(c,"INSERT INTO sections(section_name,course,year_level) VALUES ('BSIT 21007','BSIT','2')");
                int subject=Sql.insert(c,"INSERT INTO subjects(subject_code,subject_name) VALUES ('IPT101','Integrative Programming and Technologies')");
                String[][] students={{"DEMO-001","0001234567","Alex","Reyes"},{"DEMO-002","0001234568","Jamie","Santos"},{"DEMO-003","0001234569","Sam","Cruz"},{"DEMO-004","0001234570","Taylor","Garcia"}};
                for(String[] s:students)Sql.insert(c,"INSERT INTO students(student_no,rfid_uid,first_name,last_name,section_id,active) VALUES (?,?,?,?,?,TRUE)",s[0],s[1],s[2],s[3],section);
                LocalTime start=LocalTime.now().withSecond(0).withNano(0);if(start.isAfter(LocalTime.of(23,30)))start=LocalTime.of(23,30);
                for(DayOfWeek day:DayOfWeek.values())Sql.insert(c,"INSERT INTO class_schedules(section_id,subject_id,teacher_id,day_of_week,start_time,end_time,grace_minutes) VALUES (?,?,?,?,?,?,10)",section,subject,teacher,day.name(),Time.valueOf(start),Time.valueOf(LocalTime.of(23,59)));
            }
            return null;
        });
        System.out.println(demo?"Demo data created. Accounts: admin / TapInAdmin!2026; teacher / TapInTeacher!2026. Change these demo passwords before real use.":"Administrator created. Log in and create your teachers and class data.");
    }
}

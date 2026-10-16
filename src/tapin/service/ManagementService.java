package tapin.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import tapin.dao.Sql;
import tapin.model.*;
import tapin.util.*;

public final class ManagementService {
    public List<Map<String,Object>> list(Account actor, Entity e) throws SQLException {
        AuthService.requireAdmin(actor);
        try (Connection c = Database.open()) {
            String columns = e == Entity.ACCOUNTS ? "account_id,username,full_name,role" : "*";
            return Sql.rows(c,"SELECT " + columns + " FROM " + e.table + " ORDER BY " + e.id);
        }
    }
    public List<Choice> choices(String field) throws SQLException {
        String sql = switch(field) {
            case "section_id" -> "SELECT section_id AS id,section_name AS label FROM sections ORDER BY section_name";
            case "subject_id" -> "SELECT subject_id AS id,CONCAT(subject_code,' - ',subject_name) AS label FROM subjects ORDER BY subject_code";
            case "teacher_id" -> "SELECT account_id AS id,full_name AS label FROM accounts WHERE role='TEACHER' ORDER BY full_name";
            default -> throw new IllegalArgumentException("Invalid lookup field.");
        };
        try (Connection c = Database.open()) {
            List<Choice> items = new ArrayList<>();
            for (Map<String,Object> r : Sql.rows(c,sql)) items.add(new Choice(Sql.id(r,"id"),Sql.str(r,"label")));
            return items;
        }
    }
    public int save(Account actor, Entity e, int id, Map<String,Object> input) throws SQLException {
        AuthService.requireAdmin(actor);
        Map<String,Object> values = new LinkedHashMap<>();
        for (int i=0;i<e.fields.length;i++) {
            String key=e.fields[i]; Object value=input.get(key);
            if (key.equals("password")) {
                char[] password = value instanceof char[] ? (char[])value : new char[0];
                try {
                    if (id == 0 || password.length > 0) values.put(key,Passwords.store(password));
                } finally { Arrays.fill(password,'\0'); }
                continue;
            }
            if (value == null || value.toString().trim().isEmpty()) throw new IllegalArgumentException(e.labels[i] + " is required.");
            if (value instanceof String) value=value.toString().trim();
            if (e.lengths[i]>0 && value.toString().length()>e.lengths[i]) throw new IllegalArgumentException(e.labels[i]+" is too long (maximum "+e.lengths[i]+").");
            values.put(key,value);
        }
        if(e==Entity.STUDENTS) values.put("rfid_uid",AttendanceService.normalizeUid(values.get("rfid_uid").toString()));
        if(e==Entity.ACCOUNTS && !Set.of("ADMIN","TEACHER").contains(values.get("role"))) throw new IllegalArgumentException("Choose a valid role.");
        if(e==Entity.SCHEDULES) {
            try {
                DayOfWeek.valueOf(values.get("day_of_week").toString());
                LocalTime start=LocalTime.parse(values.get("start_time").toString());
                LocalTime end=LocalTime.parse(values.get("end_time").toString());
                int grace=Integer.parseInt(values.get("grace_minutes").toString());
                if(!end.isAfter(start) || grace<0 || grace>180) throw new IllegalArgumentException();
                values.put("start_time",Time.valueOf(start));values.put("end_time",Time.valueOf(end));values.put("grace_minutes",grace);
            } catch(Exception ex) { throw new IllegalArgumentException("Use valid HH:mm times, end after start, and grace between 0 and 180 minutes. Overnight classes are not supported."); }
        }
        return Database.transaction(c -> {
            if (id>0 && Sql.one(c,"SELECT " + e.id + " FROM " + e.table + " WHERE " + e.id + "=? FOR UPDATE",id)==null) throw new IllegalArgumentException("Record no longer exists. Refresh the list.");
            if(e==Entity.ACCOUNTS && id==actor.id() && !"ADMIN".equals(values.get("role"))) throw new IllegalArgumentException("You cannot remove your own administrator role.");
            if(e==Entity.ACCOUNTS && id>0 && !"TEACHER".equals(values.get("role")) && Sql.one(c,"SELECT schedule_id FROM class_schedules WHERE teacher_id=?",id)!=null) throw new IllegalArgumentException("This teacher has schedules. Keep the teacher role.");
            if(e==Entity.SCHEDULES) {
                Map<String,Object> teacher=Sql.one(c,"SELECT role FROM accounts WHERE account_id=?",values.get("teacher_id"));
                if(teacher==null || !"TEACHER".equals(teacher.get("role"))) throw new IllegalArgumentException("Select a teacher account.");
                if(id>0 && Sql.one(c,"SELECT session_id FROM attendance_sessions WHERE schedule_id=?",id)!=null) throw new IllegalArgumentException("This schedule has attendance history. Create a new schedule to preserve historical reports.");
                if(Sql.one(c,"SELECT schedule_id FROM class_schedules WHERE schedule_id<>? AND day_of_week=? AND (section_id=? OR teacher_id=?) AND start_time<? AND end_time>?",id,values.get("day_of_week"),values.get("section_id"),values.get("teacher_id"),values.get("end_time"),values.get("start_time"))!=null) throw new IllegalArgumentException("This time overlaps another schedule for the section or teacher.");
            }
            if(e==Entity.STUDENTS && id>0) {
                Map<String,Object> old=Sql.one(c,"SELECT section_id,active FROM students WHERE student_id=?",id);
                if((!old.get("section_id").toString().equals(values.get("section_id").toString()) || !Objects.equals(old.get("active"),values.get("active"))) && Sql.one(c,"SELECT a.session_id FROM attendance_sessions a JOIN class_schedules s ON s.schedule_id=a.schedule_id WHERE a.status='OPEN' AND s.section_id IN (?,?)",old.get("section_id"),values.get("section_id"))!=null)
                    throw new IllegalArgumentException("Close open sessions before changing a student's section or active status.");
            }
            List<Object> args=new ArrayList<>(values.values());
            if(id==0) return Sql.insert(c,"INSERT INTO "+e.table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")",args.toArray());
            args.add(id);
            Sql.update(c,"UPDATE "+e.table+" SET "+String.join(",",values.keySet().stream().map(k->k+"=?").toList())+" WHERE "+e.id+"=?",args.toArray());
            return id;
        });
    }
    public void delete(Account actor, Entity e, int id) throws SQLException {
        AuthService.requireAdmin(actor);
        if(e==Entity.ACCOUNTS && id==actor.id()) throw new IllegalArgumentException("You cannot delete your own account.");
        Database.transaction(c -> {
            if(e==Entity.STUDENTS) {
                if(Sql.one(c,"SELECT a.session_id FROM attendance_sessions a JOIN class_schedules s ON s.schedule_id=a.schedule_id JOIN students st ON st.section_id=s.section_id WHERE st.student_id=? AND a.status='OPEN'",id)!=null) throw new IllegalArgumentException("Close this section's sessions before deactivating a student.");
                Sql.update(c,"UPDATE students SET active=FALSE WHERE student_id=?",id);
            } else Sql.update(c,"DELETE FROM "+e.table+" WHERE "+e.id+"=?",id);
            return null;
        });
    }
}

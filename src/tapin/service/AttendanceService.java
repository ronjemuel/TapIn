package tapin.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import tapin.dao.Sql;
import tapin.model.Account;
import tapin.util.Database;

/** Transactions serialize competing taps and close/manual operations on a session. */
public final class AttendanceService {
    private final Clock clock;
    public AttendanceService() { this(Clock.systemDefaultZone()); }
    public AttendanceService(Clock clock) { this.clock = clock; }
    public static String classify(LocalDate date, LocalTime start, int grace, LocalDateTime tap) {
        return tap.isAfter(date.atTime(start).plusMinutes(grace)) ? "LATE" : "ON_TIME";
    }
    public int start(Account actor, int scheduleId, LocalDate date) throws SQLException {
        if (!date.equals(LocalDate.now(clock))) throw new IllegalArgumentException("Start sessions for today's date only.");
        return Database.transaction(c -> {
            Map<String,Object> schedule = Sql.one(c,"SELECT * FROM class_schedules WHERE schedule_id=?", scheduleId);
            if (schedule == null) throw new IllegalArgumentException("Select a valid class schedule.");
            AuthService.requireTeacher(actor, Sql.id(schedule,"teacher_id"));
            // The section row is a shared lock point for starts on different schedules.
            Sql.one(c,"SELECT section_id FROM sections WHERE section_id=? FOR UPDATE", schedule.get("section_id"));
            if (!date.getDayOfWeek().name().equals(Sql.str(schedule,"day_of_week"))) throw new IllegalArgumentException("This schedule does not meet on " + date.getDayOfWeek() + ".");
            if (Sql.one(c,"SELECT a.session_id FROM attendance_sessions a JOIN class_schedules s ON s.schedule_id=a.schedule_id WHERE s.section_id=? AND a.status='OPEN' FOR UPDATE", schedule.get("section_id")) != null)
                throw new IllegalArgumentException("A session is already open for this section. Close it first.");
            if (Sql.one(c,"SELECT session_id FROM attendance_sessions WHERE schedule_id=? AND session_date=? FOR UPDATE",scheduleId,date) != null)
                throw new IllegalArgumentException("This class already has a session for this date. Select it to view or correct attendance.");
            return Sql.insert(c,"INSERT INTO attendance_sessions(schedule_id,session_date,opened_at,status,opened_by) VALUES (?,?,?,'OPEN',?)",scheduleId,date,LocalDateTime.now(clock),actor.id());
        });
    }
    private Map<String,Object> lockSession(Connection c, int id) throws SQLException {
        Map<String,Object> session = Sql.one(c,"SELECT * FROM attendance_sessions WHERE session_id=? FOR UPDATE", id);
        if (session == null) throw new IllegalArgumentException("No attendance session selected.");
        Map<String,Object> schedule = Sql.one(c,"SELECT section_id,teacher_id,start_time,grace_minutes FROM class_schedules WHERE schedule_id=?",session.get("schedule_id"));
        session.putAll(schedule);
        return session;
    }
    public record TapResult(String student, String action, LocalDateTime time, String status) {
        @Override public String toString() { return student + " | " + action + " | " + time.toLocalTime().withNano(0) + " | " + status; }
    }
    public TapResult tap(String uid) throws SQLException {
        String card = normalizeUid(uid);
        return Database.transaction(c -> {
            Map<String,Object> student = Sql.one(c,"SELECT * FROM students WHERE rfid_uid=? AND active=TRUE", card);
            if (student == null) throw new IllegalArgumentException("Unknown RFID. Ask your administrator to register this card.");
            List<Map<String,Object>> sessions = Sql.rows(c,"SELECT a.session_id FROM attendance_sessions a JOIN class_schedules s ON s.schedule_id=a.schedule_id WHERE s.section_id=? AND a.status='OPEN' AND a.session_date=?",student.get("section_id"),LocalDate.now(clock));
            if (sessions.isEmpty()) throw new IllegalArgumentException("No active session for your section today.");
            if (sessions.size() != 1) throw new IllegalArgumentException("Multiple active sessions. Ask your teacher to close the extra session.");
            int sessionId = Sql.id(sessions.get(0),"session_id");
            Map<String,Object> session = lockSession(c, sessionId);
            if (!"OPEN".equals(session.get("status"))) throw new IllegalArgumentException("No active session. This session is closed.");
            int studentId = Sql.id(student,"student_id");
            // Recheck active membership after obtaining the session lock.
            student = Sql.one(c,"SELECT * FROM students WHERE student_id=? FOR UPDATE",studentId);
            if (!isActive(student) || Sql.id(student,"section_id") != Sql.id(session,"section_id")) throw new IllegalArgumentException("Student is not active in this section.");
            Map<String,Object> record = Sql.one(c,"SELECT * FROM attendance_records WHERE session_id=? AND student_id=? FOR UPDATE",sessionId,studentId);
            LocalDateTime now = LocalDateTime.now(clock).withNano(0);
            String name = Sql.str(student,"first_name") + " " + Sql.str(student,"last_name");
            if (record == null || record.get("time_in") == null) {
                String status = classify(((java.sql.Date)session.get("session_date")).toLocalDate(), ((Time)session.get("start_time")).toLocalTime(), Sql.id(session,"grace_minutes"),now);
                Sql.update(c,"INSERT INTO attendance_records(session_id,student_id,time_in,status,source) VALUES (?,?,?,?,'RFID') ON DUPLICATE KEY UPDATE time_in=VALUES(time_in),time_out=NULL,status=VALUES(status),source='RFID',remarks=NULL",sessionId,studentId,now,status);
                return new TapResult(name,"Time In recorded",now,status);
            }
            if (record.get("time_out") != null) return new TapResult(name,"Attendance already completed",now,Sql.str(record,"status"));
            LocalDateTime in = ((Timestamp)record.get("time_in")).toLocalDateTime();
            if (now.isBefore(in)) throw new IllegalArgumentException("Computer clock is earlier than Time In. Ask your teacher.");
            // HID readers can send the same scan twice. Do not convert bounce into Time Out.
            if (Duration.between(in,now).getSeconds() < 3) return new TapResult(name,"Tap received; wait 3 seconds before Time Out",now,Sql.str(record,"status"));
            Sql.update(c,"UPDATE attendance_records SET time_out=? WHERE attendance_id=?",now,record.get("attendance_id"));
            return new TapResult(name,"Time Out recorded",now,Sql.str(record,"status"));
        });
    }
    private static boolean isActive(Map<String,Object> s) {
        return Boolean.TRUE.equals(s.get("active")) || "1".equals(String.valueOf(s.get("active")));
    }
    public static String normalizeUid(String uid) {
        String card = uid.trim().toUpperCase(Locale.ROOT);
        if (!card.matches("[A-Z0-9]{1,30}")) throw new IllegalArgumentException("RFID UID must contain 1 to 30 letters or digits.");
        return card;
    }
    public void close(Account actor, int sessionId) throws SQLException {
        Database.transaction(c -> {
            Map<String,Object> s = lockSession(c,sessionId);
            AuthService.requireTeacher(actor,Sql.id(s,"teacher_id"));
            if (!"OPEN".equals(s.get("status"))) throw new IllegalArgumentException("Session already closed.");
            Sql.update(c,"INSERT INTO attendance_records(session_id,student_id,status,source,remarks) SELECT ?,student_id,'ABSENT','MANUAL','Automatically marked absent when session closed' FROM students WHERE section_id=? AND active=TRUE AND student_id NOT IN (SELECT student_id FROM attendance_records WHERE session_id=?)",sessionId,s.get("section_id"),sessionId);
            Sql.update(c,"UPDATE attendance_records SET status='ABSENT',time_out=NULL WHERE session_id=? AND time_in IS NULL",sessionId);
            Sql.update(c,"UPDATE attendance_sessions SET status='CLOSED',closed_at=? WHERE session_id=?",LocalDateTime.now(clock).withNano(0),sessionId);
            return null;
        });
    }
    public void manual(Account actor, int sessionId, int studentId, LocalDateTime in, LocalDateTime out, String remarks) throws SQLException {
        if (remarks == null || remarks.trim().isEmpty() || remarks.length() > 255) throw new IllegalArgumentException("Enter correction remarks (1 to 255 characters).");
        if (out != null && (in == null || out.isBefore(in))) throw new IllegalArgumentException("Time Out must be on or after Time In.");
        Database.transaction(c -> {
            Map<String,Object> s = lockSession(c,sessionId);
            AuthService.requireTeacher(actor,Sql.id(s,"teacher_id"));
            Map<String,Object> student = Sql.one(c,"SELECT * FROM students WHERE student_id=? FOR UPDATE",studentId);
            boolean existing = Sql.one(c,"SELECT attendance_id FROM attendance_records WHERE session_id=? AND student_id=?",sessionId,studentId) != null;
            if (student == null || (!existing && (!isActive(student) || Sql.id(student,"section_id") != Sql.id(s,"section_id")))) throw new IllegalArgumentException("Choose a student belonging to this session.");
            LocalDate date = ((java.sql.Date)s.get("session_date")).toLocalDate();
            if ((in != null && !in.toLocalDate().equals(date)) || (out != null && !out.toLocalDate().equals(date))) throw new IllegalArgumentException("Times must be on the session date.");
            if ((in != null && in.isAfter(LocalDateTime.now(clock))) || (out != null && out.isAfter(LocalDateTime.now(clock)))) throw new IllegalArgumentException("Attendance times cannot be in the future.");
            String status = in == null ? "ABSENT" : classify(date,((Time)s.get("start_time")).toLocalTime(),Sql.id(s,"grace_minutes"),in);
            Sql.update(c,"INSERT INTO attendance_records(session_id,student_id,time_in,time_out,status,source,remarks) VALUES (?,?,?,?,?,'MANUAL',?) ON DUPLICATE KEY UPDATE time_in=VALUES(time_in),time_out=VALUES(time_out),status=VALUES(status),source='MANUAL',remarks=VALUES(remarks)",sessionId,studentId,in,out,status,remarks.trim());
            return null;
        });
    }
}

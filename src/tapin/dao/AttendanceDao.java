package tapin.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import tapin.model.Account;
import tapin.util.Database;

public final class AttendanceDao {
    public List<Map<String,Object>> schedules(Account a) throws SQLException {
        try (Connection c = Database.open()) {
            return Sql.rows(c,"SELECT s.*, sec.section_name, sub.subject_code, sub.subject_name, ac.full_name FROM class_schedules s JOIN sections sec ON sec.section_id=s.section_id JOIN subjects sub ON sub.subject_id=s.subject_id JOIN accounts ac ON ac.account_id=s.teacher_id WHERE (?=TRUE OR s.teacher_id=?) ORDER BY sec.section_name, sub.subject_code, s.day_of_week, s.start_time",a.admin(),a.id());
        }
    }
    public Map<String,Object> session(Account a, int scheduleId, LocalDate date) throws SQLException {
        try (Connection c = Database.open()) {
            return Sql.one(c,"SELECT ats.* FROM attendance_sessions ats JOIN class_schedules s ON s.schedule_id=ats.schedule_id WHERE ats.schedule_id=? AND ats.session_date=? AND (?=TRUE OR s.teacher_id=?)",scheduleId,date,a.admin(),a.id());
        }
    }
    public List<Map<String,Object>> roster(Account a, int scheduleId, int sessionId) throws SQLException {
        try (Connection c = Database.open()) {
            return Sql.rows(c,"SELECT st.student_id, CONCAT(st.last_name, ', ', st.first_name) AS student_name, st.rfid_uid, r.time_in, r.time_out, COALESCE(r.status,'PENDING') AS status, r.remarks, r.source FROM class_schedules s JOIN students st ON (st.section_id=s.section_id AND st.active=TRUE AND (?=0 OR EXISTS (SELECT 1 FROM attendance_sessions active_session WHERE active_session.session_id=? AND active_session.status='OPEN'))) OR EXISTS (SELECT 1 FROM attendance_records old WHERE old.session_id=? AND old.student_id=st.student_id) LEFT JOIN attendance_records r ON r.student_id=st.student_id AND r.session_id=? WHERE s.schedule_id=? AND (?=TRUE OR s.teacher_id=?) AND (?=0 OR EXISTS (SELECT 1 FROM attendance_sessions matching WHERE matching.session_id=? AND matching.schedule_id=s.schedule_id)) ORDER BY st.last_name,st.first_name",sessionId,sessionId,sessionId,sessionId,scheduleId,a.admin(),a.id(),sessionId,sessionId);
        }
    }
    public List<Map<String,Object>> reports(Account a, LocalDate date, int section, int subject, String student) throws SQLException {
        try (Connection c = Database.open()) {
            return Sql.rows(c,"SELECT ats.session_date, sec.section_name, sub.subject_code, st.student_no, CONCAT(st.last_name, ', ', st.first_name) AS student_name, st.rfid_uid, r.time_in, r.time_out, r.status, r.source, r.remarks FROM attendance_records r JOIN students st ON st.student_id=r.student_id JOIN attendance_sessions ats ON ats.session_id=r.session_id JOIN class_schedules s ON s.schedule_id=ats.schedule_id JOIN sections sec ON sec.section_id=s.section_id JOIN subjects sub ON sub.subject_id=s.subject_id WHERE (?=TRUE OR s.teacher_id=?) AND (? IS NULL OR ats.session_date=?) AND (?=0 OR s.section_id=?) AND (?=0 OR s.subject_id=?) AND (LOCATE(?,CONCAT(st.first_name,' ',st.last_name,' ',st.student_no))>0) ORDER BY ats.session_date,sec.section_name,sub.subject_code,st.last_name",a.admin(),a.id(),date,date,section,section,subject,subject,student.trim());
        }
    }
    public int openCount() throws SQLException {
        try (Connection c = Database.open()) { return Sql.id(Sql.one(c,"SELECT COUNT(*) AS n FROM attendance_sessions WHERE status='OPEN' AND session_date=CURDATE()"),"n"); }
    }
}

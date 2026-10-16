# Defense demonstration

## Prepare before the presentation

Start MySQL in XAMPP, check `config/database.properties`, build the project, import `sql/setup.sql` into a new installation, and run `Setup --demo`. Bring a compatible 125 kHz HID reader and registered card if available. Keep keyboard UID entry ready as a clearly identified simulation.

Log in as administrator. In Manage Schedules, edit today's **unused** meeting so its start time is one minute before the presentation and its grace period is two minutes. Leave another schedule for a different, non-overlapping time if you want to demonstrate more than one subject. A schedule that already has attendance history cannot be edited; create a new unused schedule for another demonstration. Do not change the Windows clock during the demo.

Register the physical card's exact output in Manage Students if using hardware. The supplied demo UIDs are examples, not guaranteed outputs of your cards.

## Suggested 10–12 minute sequence

1. **Explain the purpose.** “TapIn records attendance per actual class session. It prevents mixing the same student's attendance across different subjects.”
2. **Show invalid login**, then select Administrator and use the demo administrator account.
3. **Demonstrate student CRUD.** Create a temporary student, find it in the table, edit the name, and deactivate it. Try reusing an existing RFID UID and show the duplicate validation. Do this before opening the section's session.
4. **Show linked management.** Open Sections, Subjects, Accounts, and Schedules. Show that schedules select a teacher, section, subject, weekday, times, and grace period. Delete an unused temporary subject after confirming.
5. **Log out and log in as Teacher.** Show only that teacher's assigned schedules. Select today's meeting and Start Session.
6. **Open Attendance / Kiosk.** Scan `0001234567`, or type it and press Enter. Before the grace cutoff, the result is ON_TIME. Show the dashboard automatically refreshing.
7. **Wait at least three seconds, then scan the same card again.** Explain Time Out. Scan again to show “Attendance already completed.”
8. **After the two-minute grace cutoff**, scan `0001234568` to demonstrate LATE. Explain that the cutoff uses scheduled start plus grace, not the moment the teacher opened the session.
9. **Show errors.** Scan an unregistered UID. Explain that the card must be registered and belong to a section with an open session.
10. **Manual correction.** Select one student, click Manual Attendance, enter a valid past Time In and a reason such as “Card unreadable; teacher verified arrival.” Show source MANUAL in Reports. Do not use future timestamps.
11. **Close Session.** Leave at least one student without Time In. Confirm closing and show automatic ABSENT records. Scan again to show no active session.
12. **Reports and export.** Filter by date, section, subject, and student. Export CSV, then open the file in a spreadsheet or text editor. Finish by logging out.

If the demonstration must be shorter, explain LATE using the tested grace-boundary examples, or prepare an unused schedule whose start/grace threshold has already passed. Do not claim a live tap is late unless it actually is.

## Questions and simple answers

| Question | Answer |
|---|---|
| How does the reader communicate with Java? | It acts like a USB keyboard. A card produces its UID and Enter. The JTextField action listener processes the completed input. |
| Do we need Arduino? | No. This version uses a keyboard/HID reader directly. |
| Why seven tables? | Each table represents a distinct thing: accounts, sections, students, subjects, schedules, sessions, and attendance records. The relationships avoid duplicated class data. |
| Why separate schedules from sessions? | A schedule is the recurring timetable. A session is one actual meeting on one date. |
| Why use IDs? | Names can change or be repeated. Numeric primary keys identify records and foreign keys link them reliably. |
| What prevents two students sharing a card? | The unique RFID UID constraint in MySQL, plus a readable duplicate message in the application. |
| What prevents duplicate attendance? | A unique student/session pair and a transaction with a session-row lock. |
| What is JDBC? | Java's standard API for connecting to a database, sending SQL, and reading results. Connector/J is the MySQL driver. |
| What does Connection do? | It represents a database connection and controls commit or rollback for a transaction. |
| Why PreparedStatement? | It binds values separately from SQL, avoiding unsafe concatenation of user input. |
| executeQuery versus executeUpdate? | executeQuery returns a ResultSet for SELECT. executeUpdate returns the affected-row count for INSERT, UPDATE, or DELETE. |
| What is a ResultSet? | The rows returned by a query. We move through them with next() and read their columns. |
| What is a transaction? | A group of changes that either all commit or all roll back. Closing a session and creating absences must succeed together. |
| How is lateness calculated? | Compare Time In to the session date at scheduled start plus grace minutes. Exactly at the cutoff is on time. |
| What happens to students who never tap? | Closing the session creates ABSENT records for active students in that section who have no Time In. |
| Can teachers modify another teacher's class? | No. Queries and attendance services check the logged-in teacher against the schedule owner. |
| Why do we deactivate students? | It prevents future taps while preserving old attendance records and foreign-key relationships. |
| How are passwords stored? | As plain text in accounts.password, as requested for direct phpMyAdmin editing. Anyone who can read that table can read the passwords. |
| Why does the screen stay responsive? | Database operations use SwingWorker. Swing's event-dispatch thread remains available for the interface. |
| Why are there .form files? | NetBeans uses them to maintain editable component layouts and regenerate initialization code. |
| What happens if MySQL is stopped? | The UI reports a connection problem. No attendance is falsely shown as successfully saved. |
| Can the app prove the reader is plugged in? | No. HID input resembles normal keyboard input. The kiosk reports its input/session state, not USB hardware detection. |
| What did you test? | 59 real-database assertions, native NetBeans form loading, offscreen Swing checks, and 13 desktop interaction checks. Physical reader and printer checks remain separate. |
| What are the limits? | One meeting per schedule/date, same-day classes, local database configuration, and no immutable correction-history table. |

## Suggested individual preparation

Every member should be able to trace one complete flow from button/Enter event → service validation → prepared SQL → committed result → displayed feedback. Split the explanation among login/security, management CRUD, attendance/session logic, Swing/forms, and reports/testing, but let every member practice the JDBC and table questions.

# Architecture

The source follows the documentation's presentation → application → JDBC/data structure. It uses small classes with standard JDK APIs.

| Folder / class | Responsibility |
|---|---|
| `tapin.App` | Starts Swing on the event-dispatch thread; optional `--kiosk` startup. |
| `tapin.Setup` | One-time administrator or demo-data creation. |
| `model` | Account identity, combo-box ID/name choices, fixed CRUD field metadata. |
| `ui` | Six JFrame Forms, two shared dialogs, appearance, background workers, CSV file chooser. |
| `service/AuthService` | Validates fields, compares passwords and verifies the selected role. |
| `service/ManagementService` | Administrator authorization, field validation, management CRUD. |
| `service/AttendanceService` | Session ownership, RFID routing, timestamps, status, closing, manual corrections. |
| `dao/AttendanceDao` | Teacher-scoped schedules, session lookup, rosters, and report queries. |
| `dao/Sql` | PreparedStatement binding, ResultSet reading, updates, and generated IDs. |
| `util/Database` | Reads external configuration; opens JDBC connections and owns transactions. |
| `util/Passwords` | Plain-text password validation and exact comparison. |
| `util/Csv` | UTF-8 CSV output, escaping, and formula-prefix protection. |

Management uses a fixed `Entity` enum to choose table and column names. User values are always bound as parameters; users cannot supply SQL identifiers. This shares basic CRUD machinery while keeping the project small enough to explain.

## Seven tables

| Table | Purpose and relationships |
|---|---|
| `accounts` | Unique username, plain-text password, full name, TEACHER/ADMIN role. Its ID is referenced by schedules and session opener. |
| `sections` | Section name, course, year level. Referenced by students and schedules. |
| `students` | Unique student number and RFID UID, names, section ID, active flag. Referenced by attendance records. |
| `subjects` | Subject code and name. Referenced by schedules. |
| `class_schedules` | Section, subject, assigned teacher, weekday, start/end time, grace minutes. Referenced by sessions. |
| `attendance_sessions` | One actual meeting of a schedule on a date; opening/closing timestamps, OPEN/CLOSED state, opener. Referenced by records. |
| `attendance_records` | One student's Time In/Out, ON_TIME/LATE/ABSENT state, RFID/MANUAL source, and remarks for one session. |

The SQL matches the documented field names and sizes. It additionally makes section names and subject codes unique, restricts a schedule to one session per date, and checks basic time/status consistency. Foreign keys use restrictive deletion to preserve history. Student deletion is represented by safe deactivation, as allowed by the prompt.

## Attendance transaction

1. Validate UID as a string, preserving leading zeroes; find an active student.
2. Find today's OPEN session for that student's section.
3. Lock the session row with `SELECT ... FOR UPDATE`; recheck its state.
4. Recheck the student under a row lock, then inspect the unique student/session record.
5. Save Time In and calculate status, or save Time Out, or return “already completed.”
6. Commit. On a database or validation error, roll back.

Closing and manual correction lock the same session row. Consequently they cannot modify it simultaneously with a tap. Starting locks the section row and uses current locking reads to prevent two open sessions in the same section. The unique `(session_id, student_id)` constraint is a second protection against duplicate attendance.

The exact grace cutoff is ON_TIME; one second after it is LATE. `AttendanceService` accepts a `Clock`, allowing deterministic tests without altering the Windows clock. Normal application construction uses the real local clock.

Closed-session rosters come from recorded students, so deactivated or transferred students retain their history and newly enrolled students do not appear as PENDING in an old class. Reports use the schedule's section rather than a student's current section.

## Swing and resources

Each major screen has a normal `.form` and guarded `initComponents` method. BorderLayout, GridLayout, and FlowLayout keep the controls editable without third-party layout libraries. Business logic stays outside generated initialization. A shared dialog builds management inputs from fixed field metadata and uses dropdowns for foreign keys.

`SwingWorker` performs JDBC operations off the event-dispatch thread; its completion method updates the controls. A Swing timer refreshes the dashboard every three seconds. All timers stop when their windows close. Logout disposes child staff windows.

The original logo is stored unchanged at `src/resources/images/tapin_logo.png`. `Style` loads it from the classpath and displays the central logo area without the large black outer margins. The icon is never redrawn or substituted. The small RFID card symbol is a separate Java2D icon.

## Explicit scope choices

- Teacher attendance writes are limited to that teacher's schedules. Administrators manage data and view reports, following the final use-case diagram.
- One meeting per schedule/date; no overnight classes or automatic reopening.
- No history migration from the old prototype and no SQLite.
- No student login, photos, hours calculation, online portal, or third-party report framework.
- Manual corrections store their source and reason. The documented seven-table schema does not provide an immutable, multi-version audit log.
- This local desktop application's database credentials belong to the trusted staff PC. It is not designed as a public network service.

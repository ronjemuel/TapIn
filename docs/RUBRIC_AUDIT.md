# Rubric audit

This maps the rubric to delivered code and evidence. It does not assign a guaranteed grade; presentation and individual understanding are assessed by the instructor.

| Criterion | Points | Implementation | Evidence / remaining check |
|---|---:|---|---|
| System Functionality | 25 | `AttendanceService`, all six screens: sessions, UID scans, Time In/Out, status, manual correction, closing, reports, logout. | Integration and desktop interaction checks passed. Physical reader check remains. |
| JDBC & Database Integration | 20 | `Database`, `Sql`, `AttendanceDao`, service queries, Connector/J, `sql/setup.sql`. Uses Connection, PreparedStatement, ResultSet, executeQuery, executeUpdate, generated keys, transactions and foreign keys. | Real XAMPP MariaDB integration suite; no in-memory database substitute. |
| CRUD Operations | 15 | `ManagementService`, `ManagementFrame`, `ManagementEditor`: students, accounts, sections, subjects, schedules. Student delete is safe deactivation; unused related entities support physical delete. | Student CRUD and duplicate validation; each management entity's create/read/update/delete paths tested. |
| Java Swing GUI | 10 | Six `.java`/`.form` pairs; standard controls, editable layouts, official logo, readable table, background DB tasks, child disposal. | All forms loaded through NetBeans 31's native persistence manager without form-loading errors. Swing renders inspected; child disposal checked. |
| Input Validation & Error Handling | 10 | Required fields, lengths, password rules, role/ownership checks, duplicate identifiers, time ordering, overlapping schedules, date/weekday restrictions, meaningful SQL errors and confirmations. | Invalid-login, duplicate, invalid-time, authorization, completed-tap and closed-session tests passed. |
| Code Organization | 5 | `model`, `dao`, `service`, `ui`, `util`; one connection helper; parameter binding; small shared CRUD editor; no framework dependency. | Java 17 build and NetBeans Ant build passed; architecture guide supplied. |
| Presentation & Demonstration | 10 | Portable project, SQL, setup/run instructions, demo accounts/cards, CSV output, defense sequence. | `README.md` and `DEFENSE.md`. The group must rehearse on its presentation PC. |
| Individual Technical Understanding | 5 | JDBC/table explanations, transaction walkthrough, and common questions. | `ARCHITECTURE.md` and `DEFENSE.md`. Each member must explain the code personally. |

## Requirement coverage

| Requirement | Location |
|---|---|
| MySQL account-based login, role match, show/hide, clear | `StaffLogin`, `AuthService`, `Passwords` |
| Official logo in login/dashboard/kiosk | `Style`, `src/resources/images/tapin_logo.png` |
| Section, subject, date, schedule, Start/Close | `TeacherDashboard`, `AttendanceService` |
| Total/Present/Late/Absent, OPEN/CLOSED, grace | `TeacherDashboard.refresh` |
| Student search, Name/UID/Time In/Time Out/Status/Remarks | `TeacherDashboard`, `AttendanceDao.roster` |
| Keyboard/HID UID + Enter, feedback/reset | `AttendanceKiosk.scan`, reset timer |
| Correct class routing | `AttendanceService.tap` matches student section and today's open session |
| Duplicate scans and completed attendance | Session locking, unique constraint, three-second debounce |
| Unknown RFID / no active session | `AttendanceService.tap` validation |
| Automatic ABSENT on closing | `AttendanceService.close` transaction |
| Manual correction and remarks | `ManualAttendanceDialog`, `AttendanceService.manual` |
| Student/section/subject/account/schedule management | `Entity`, `ManagementService`, management window/editor |
| Date/section/subject/student report filters | `ReportsFrame`, `AttendanceDao.reports` |
| CSV export, optional print | `Csv`, `Style.export`, `ReportsFrame` print button |
| Portable resources and connector | Classpath logo, relative library/config paths, `build.ps1`, `run.bat` |
| Destructive-action confirmations | Management delete/deactivate and session-close handlers |
| Child windows do not terminate dashboard | DISPOSE_ON_CLOSE; tested |

There are no placeholder management buttons. Teacher Students/Schedules views are read-only, as the final documentation assigns management to administrators.

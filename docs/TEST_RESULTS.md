# Verification record

## Plain-text password update

The user requested removal of password hashing. The current build uses `accounts.password`. All 59 integration checks passed after this change, including direct database username/password edits and case-sensitive login. The migration was tested on an isolated legacy accounts table: the account ID, username, name and role were preserved, and its old hash was cleared for reset. The earlier desktop interaction, form-loader, and visual checks below describe the original build; they were not repeated for this password-storage update.

Tested on 2026-09-27 in the supplied Windows workspace.

## Build and environment

- Oracle JDK 17.0.12; compiled with Java 17 source/target.
- Supplied MySQL Connector/J 26.7.0.
- XAMPP MariaDB 10.4.32, isolated data directory under `review/mysql-test`, bound to `127.0.0.1:3307`.
- `build.ps1`: passed; runnable `dist/TapIn.jar` produced.
- NetBeans 31 Ant build: passed. `build-impl.xml` was generated from the installed NetBeans template for this project's metadata.
- The official logo resource's SHA-256 matches the supplied PNG exactly.
- After shutting down only the isolated test server, `ConnectionFailureTest` verified the application's XAMPP/configuration error guidance. [Failure-path log](connection-failure-test.log).

The test server is separate from the normal XAMPP installation on port 3306 and was shut down after verification. Production configuration remains in `config/database.properties`; test credentials/data were not imported into the old project database.

## Real-database tests: 59 passed

See [integration-test.log](integration-test.log) and `test/tapin/IntegrationTest.java`.

Coverage includes both roles, wrong credentials/role, empty login fields, student CRUD, duplicate RFID/student number, each management entity's operations, overlap/time validation, teacher authorization, session start, duplicate start, ON_TIME grace boundary, LATE immediately after it, debounce, Time Out, completed tap, unknown card, no session, manual correction and validation, automatic absence creation, close timestamp, closed-session rejection, report filters, retained student history, historical-roster membership, concurrent taps, concurrent starts, foreign keys, UTF-8 CSV quoting, plain-text storage, direct database account edits, and case-sensitive password comparison.

A test-runner issue involving Connector/J statements retaining their original catalog was fixed before the final suite: test schema DDL now uses a new connection whose catalog is explicitly checked. No existing database table was replaced.

## NetBeans form loading

All six `.form` files were loaded by the installed NetBeans 31 `GandalfPersistenceManager` with zero form-loading errors:

| Form | Components |
|---|---:|
| AdminDashboard | 14 |
| AttendanceKiosk | 13 |
| ManagementFrame | 13 |
| ReportsFrame | 22 |
| StaffLogin | 20 |
| TeacherDashboard | 42 |

This exercised the actual form parser and component model, including layout constraints and property editors. The standalone harness disabled IDE look-and-feel switching because it was outside the full IDE module launcher. A manual edit/save round-trip in the NetBeans Design tab was not performed.

## Swing smoke checks

`UiSmokeTest` constructed the real screens, loaded demo data asynchronously, checked password visibility/clear, searched the roster, and verified that disposing a kiosk preserves the dashboard. All checks passed. [Smoke log](ui-smoke-test.log).

Offscreen renders of actual Swing controls were visually inspected:

- [Staff login](screenshots/login.png)
- [Teacher dashboard](screenshots/dashboard.png)
- [RFID kiosk](screenshots/kiosk.png)
- [Administrator](screenshots/admin.png)
- [Students management](screenshots/students.png)
- [Reports](screenshots/reports.png)

These are component renders without native window borders, not the supplied mockups.

## Desktop interaction checks: 13 passed

`UiInteractionTest` opened real application windows against isolated demo data and exercised:

1. Incorrect credentials through the Log In button.
2. Teacher login opening the dashboard.
3. Today's schedule and the database roster loading.
4. Start Session opening the meeting.
5. UID + Enter recording Time In.
6. A second UID + Enter recording Time Out.
7. Closing the kiosk without terminating the dashboard.
8. Close Session committing the meeting.
9. Live summary displaying three automatic absences.
10. Teacher logout returning to login.
11. Administrator role selection and login.
12. Manage Students opening its CRUD window.
13. Administrator logout disposing its management child window.

## Remaining external checks

- No physical RFID reader was available for an end-to-end hardware scan. Text-field Enter events exercised the HID input path. Check reader frequency, card compatibility, exact UID format, focus, and Enter suffix before the defense.
- Printing is implemented with JTable's print dialog but no physical printer output was verified.
- A second Windows PC was not available. Paths, resources, and connector references are portable, but perform the README setup on that PC.
- Oracle MySQL Server was not installed separately; integration was verified on the MariaDB server supplied by XAMPP.

## Reproduce

Run `test.ps1` against a configured working database account with CREATE DATABASE permission. It creates a fresh `tapin_test_*` schema and retains it for inspection. For the GUI interaction test, use another fresh schema with `Setup --demo`, because the test deliberately closes today's session. Test-only configurations are not required by the delivered application.

## School timetable import (2026-09-27)

Java 17 build passed. Applied the supplied 15 meetings to the local database: 7 new teacher accounts, 7 new subjects, 9 inserted schedules, and 6 repurposed unused demo schedules. Readback confirmed the subject/teacher/day/time assignments and preserved schedule 7 with its closed session dated 2026-09-27. The importer checked all 15 distinct meetings and rejected any overlapping section/teacher times. A second import committed with zero new teachers, subjects, or schedules and zero repurposed rows, confirming repeatability. The existing integration and GUI suites were not rerun for this data import.

# Files created and reused

The original DOCX files, PNG mockups, `MASTER_PROMPT.txt`, and `Old-reference` were not changed. All application work is in `TapIn/`.

| Path | Content |
|---|---|
| `src/tapin/App.java`, `Setup.java` | Application entry point and first-run account/demo setup. |
| `src/tapin/model/` | `Account`, `Choice`, `Entity`. |
| `src/tapin/dao/` | `Sql`, `AttendanceDao`. |
| `src/tapin/service/` | `AuthService`, `AttendanceService`, `ManagementService`. |
| `src/tapin/util/` | `Database`, `Passwords`, `Csv`. |
| `src/tapin/ui/` | Six major JFrame `.java`/`.form` pairs, shared editor/correction dialogs, `Style`, and `RfidIcon`. |
| `src/resources/images/tapin_logo.png` | Byte-for-byte copy of the supplied official logo. |
| `sql/setup.sql` | Complete seven-table MySQL/MariaDB schema. |
| `sql/migrate_plain_passwords.sql` | One-time migration for existing hashed-password installations; requires new passwords afterward. |
| `config/database.properties` | Portable local MySQL settings; no machine-specific path. |
| `lib/` | Supplied Connector/J binary and its original license. |
| `nbproject/project.xml`, `project.properties` | NetBeans Java project metadata and relative driver reference. |
| `nbproject/build-impl.xml` | Build implementation generated using the installed NetBeans template. |
| `build.xml`, `manifest.mf` | Ant entry point and JAR manifest. |
| `build.ps1`, `test.ps1`, `run.bat` | Build, integration-test, and launch scripts. |
| `test/tapin/` | Real JDBC tests, isolated-schema runner, Swing smoke/interaction tests, connection-failure check. |
| `dist/TapIn.jar`, `dist/lib/` | Compiled runnable application and runtime driver. |
| `README.md`, `docs/` | Setup/run guide, architecture, defense, rubric audit, verification record, logs and actual UI renders. |
| `.gitignore` | Excludes local build products, private IDE metadata and test configurations. |

`FILE_MANIFEST.txt` lists every file in the packaged deliverable. `TapIn-NetBeans.zip` contains the portable project, sources, forms, SQL, guides, and built JAR. Local test configurations and build intermediates are excluded from that ZIP.

The workspace's separate `review/` folder contains the initial file inventory, extracted documentation images used for inspection, an isolated test database directory, and temporary NetBeans verification helpers. It is not required to build/run TapIn and is not included in the portable ZIP.

No old attendance implementation was carried over. The original connector and logo were reused; the NetBeans build script was regenerated for the new project. The old hardcoded paths, global-latest-session routing, and missing administrator separation were replaced. The subsequent user-requested password change uses plain-text accounts.password instead of hashes.

School timetable additions: `src/tapin/ImportSchoolSchedule.java` imports `data/bsit21007-schedule.tsv`; `docs/SCHOOL_SCHEDULE.md` documents the source, accounts, and retained historical schedule.

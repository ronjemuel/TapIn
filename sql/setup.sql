-- Import into a NEW database. Does not delete or overwrite the old tapin_db.
CREATE DATABASE IF NOT EXISTS tapin_rebuild CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tapin_rebuild;
CREATE TABLE accounts (
 account_id INT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(30) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL,
 full_name VARCHAR(100) NOT NULL,
 role ENUM('TEACHER','ADMIN') NOT NULL
) ENGINE=InnoDB;
CREATE TABLE sections (
 section_id INT PRIMARY KEY AUTO_INCREMENT,
 section_name VARCHAR(30) NOT NULL UNIQUE,
 course VARCHAR(50) NOT NULL,
 year_level VARCHAR(10) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE students (
 student_id INT PRIMARY KEY AUTO_INCREMENT,
 student_no VARCHAR(30) NOT NULL UNIQUE,
 rfid_uid VARCHAR(30) NOT NULL UNIQUE,
 first_name VARCHAR(50) NOT NULL,
 last_name VARCHAR(50) NOT NULL,
 section_id INT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY (section_id) REFERENCES sections(section_id)
) ENGINE=InnoDB;
CREATE TABLE subjects (
 subject_id INT PRIMARY KEY AUTO_INCREMENT,
 subject_code VARCHAR(20) NOT NULL UNIQUE,
 subject_name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE class_schedules (
 schedule_id INT PRIMARY KEY AUTO_INCREMENT,
 section_id INT NOT NULL,
 subject_id INT NOT NULL,
 teacher_id INT NOT NULL,
 day_of_week VARCHAR(10) NOT NULL,
 start_time TIME NOT NULL,
 end_time TIME NOT NULL,
 grace_minutes INT NOT NULL DEFAULT 10,
 FOREIGN KEY (section_id) REFERENCES sections(section_id),
 FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
 FOREIGN KEY (teacher_id) REFERENCES accounts(account_id),
 CHECK (end_time > start_time),
 CHECK (grace_minutes BETWEEN 0 AND 180),
 CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
) ENGINE=InnoDB;
CREATE TABLE attendance_sessions (
 session_id INT PRIMARY KEY AUTO_INCREMENT,
 schedule_id INT NOT NULL,
 session_date DATE NOT NULL,
 opened_at DATETIME NOT NULL,
 closed_at DATETIME NULL,
 status ENUM('OPEN','CLOSED') NOT NULL,
 opened_by INT NOT NULL,
 UNIQUE (schedule_id, session_date),
 FOREIGN KEY (schedule_id) REFERENCES class_schedules(schedule_id),
 FOREIGN KEY (opened_by) REFERENCES accounts(account_id)
) ENGINE=InnoDB;
CREATE TABLE attendance_records (
 attendance_id INT PRIMARY KEY AUTO_INCREMENT,
 session_id INT NOT NULL,
 student_id INT NOT NULL,
 time_in DATETIME NULL,
 time_out DATETIME NULL,
 status ENUM('ON_TIME','LATE','ABSENT') NOT NULL,
 source ENUM('RFID','MANUAL') NOT NULL,
 remarks VARCHAR(255) NULL,
 UNIQUE (session_id, student_id),
 FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id),
 FOREIGN KEY (student_id) REFERENCES students(student_id),
 CHECK ((status = 'ABSENT' AND time_in IS NULL AND time_out IS NULL) OR
        (status IN ('ON_TIME','LATE') AND time_in IS NOT NULL)),
 CHECK (time_out IS NULL OR time_out >= time_in)
) ENGINE=InnoDB;

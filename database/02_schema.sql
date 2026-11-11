-- Smart Healthcare Management System - reference schema (MySQL 8+)
-- OPTIONAL: by default Hibernate creates/updates these tables automatically (DDL_AUTO=update).
-- Use this file only if you prefer to create the schema yourself and then start the app with DDL_AUTO=none.
-- It mirrors the JPA entities in src/main/java/com/smarthealth/hms/domain.

USE smart_healthcare;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,           -- BCrypt hash, never plain text
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL,           -- ADMIN | DOCTOR | RECEPTIONIST | PATIENT
  enabled       BIT          NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','DOCTOR','RECEPTIONIST','PATIENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS departments (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  PRIMARY KEY (id),
  UNIQUE KEY uk_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS patients (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  patient_code      VARCHAR(20)  NOT NULL,
  first_name        VARCHAR(50)  NOT NULL,
  last_name         VARCHAR(50)  NOT NULL,
  gender            VARCHAR(10)  NOT NULL,
  date_of_birth     DATE         NOT NULL,
  phone             VARCHAR(20)  NOT NULL,
  email             VARCHAR(100),
  address           VARCHAR(255),
  blood_group       VARCHAR(5),
  allergies         VARCHAR(255),
  emergency_contact VARCHAR(100),
  user_id           BIGINT,
  created_at        DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_patients_code (patient_code),
  UNIQUE KEY uk_patients_user (user_id),
  CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doctors (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  user_id          BIGINT        NOT NULL,
  department_id    BIGINT        NOT NULL,
  specialization   VARCHAR(100)  NOT NULL,
  qualification    VARCHAR(150),
  phone            VARCHAR(20)   NOT NULL,
  experience_years INT           NOT NULL,
  consultation_fee DECIMAL(10,2) NOT NULL,
  available_from   TIME(6)       NOT NULL,
  available_to     TIME(6)       NOT NULL,
  active           BIT           NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doctors_user (user_id),
  CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_doctors_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS doctor_available_days (
  doctor_id   BIGINT      NOT NULL,
  day_of_week VARCHAR(12) NOT NULL,              -- MONDAY ... SUNDAY
  CONSTRAINT fk_available_days_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS staff (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  full_name     VARCHAR(100) NOT NULL,
  designation   VARCHAR(100) NOT NULL,
  department_id BIGINT,
  phone         VARCHAR(20)  NOT NULL,
  email         VARCHAR(100),
  joined_date   DATE         NOT NULL,
  active        BIT          NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_staff_department FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointments (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  patient_id       BIGINT       NOT NULL,
  doctor_id        BIGINT       NOT NULL,
  appointment_date DATE         NOT NULL,
  start_time       TIME(6)      NOT NULL,
  end_time         TIME(6)      NOT NULL,
  reason           VARCHAR(255) NOT NULL,
  notes            VARCHAR(500),
  status           VARCHAR(15)  NOT NULL,        -- SCHEDULED | COMPLETED | CANCELLED
  created_at       DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_appt_doctor_date (doctor_id, appointment_date),
  CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
  CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
  CONSTRAINT chk_appt_times CHECK (end_time > start_time),
  CONSTRAINT chk_appt_status CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS diagnostic_tests (
  id                     BIGINT        NOT NULL AUTO_INCREMENT,
  patient_id             BIGINT        NOT NULL,
  requested_by_doctor_id BIGINT        NOT NULL,
  test_name              VARCHAR(100)  NOT NULL,
  category               VARCHAR(50)   NOT NULL,
  clinical_notes         VARCHAR(500),
  status                 VARCHAR(20)   NOT NULL, -- REQUESTED | SAMPLE_COLLECTED | IN_PROGRESS | COMPLETED | CANCELLED
  result_summary         VARCHAR(2000),
  result_entered_by      VARCHAR(100),
  requested_at           DATETIME(6)   NOT NULL,
  completed_at           DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_tests_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
  CONSTRAINT fk_tests_doctor FOREIGN KEY (requested_by_doctor_id) REFERENCES doctors (id),
  CONSTRAINT chk_tests_status CHECK (status IN ('REQUESTED','SAMPLE_COLLECTED','IN_PROGRESS','COMPLETED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET NAMES utf8mb4;
USE report_check_system;

INSERT IGNORE INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '管理员', '系统管理员'),
(2, 'TEACHER', '教师', '课程教师'),
(3, 'STUDENT', '学生', '学生用户');

UPDATE sys_user SET email = NULL WHERE email IS NOT NULL AND TRIM(email) = '';
UPDATE sys_user SET student_no = NULL WHERE student_no IS NOT NULL AND TRIM(student_no) = '';
UPDATE sys_user SET teacher_no = NULL WHERE teacher_no IS NOT NULL AND TRIM(teacher_no) = '';

DELIMITER //

CREATE PROCEDURE add_auth_indexes_if_needed()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'uk_user_email'
  ) THEN
    ALTER TABLE sys_user ADD UNIQUE KEY uk_user_email (email);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'uk_user_student_no'
  ) THEN
    ALTER TABLE sys_user ADD UNIQUE KEY uk_user_student_no (student_no);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'uk_user_teacher_no'
  ) THEN
    ALTER TABLE sys_user ADD UNIQUE KEY uk_user_teacher_no (teacher_no);
  END IF;
END//

DELIMITER ;

CALL add_auth_indexes_if_needed();
DROP PROCEDURE add_auth_indexes_if_needed;

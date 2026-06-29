SET NAMES utf8mb4;
USE report_check_system;

UPDATE sys_user
SET password = '123456'
WHERE username IN (
  'admin',
  'teacher01',
  'teacher02',
  'teacher03',
  'student01',
  'student02',
  'student03',
  'student04',
  'student05',
  'student06',
  'student07',
  'student08',
  'student09',
  'student10',
  'student11',
  'student12'
);

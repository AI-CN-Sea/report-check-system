SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS report_check_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE report_check_system;

DROP TABLE IF EXISTS similar_sentence;
DROP TABLE IF EXISTS check_result;
DROP TABLE IF EXISTS check_task;
DROP TABLE IF EXISTS report_file;
DROP TABLE IF EXISTS experiment_task;
DROP TABLE IF EXISTS student_class;
DROP TABLE IF EXISTS class_info;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
  role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '角色说明',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '系统角色表';

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  student_no VARCHAR(64) NULL COMMENT '学号',
  teacher_no VARCHAR(64) NULL COMMENT '工号',
  phone VARCHAR(32) NULL COMMENT '手机号',
  email VARCHAR(128) NULL COMMENT '邮箱',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_student_no (student_no),
  UNIQUE KEY uk_user_teacher_no (teacher_no),
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) COMMENT '系统用户表';

CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
  course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
  course_code VARCHAR(64) NOT NULL UNIQUE COMMENT '课程编码',
  teacher_id BIGINT NOT NULL COMMENT '任课教师ID',
  description TEXT NULL COMMENT '课程说明',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) COMMENT '课程表';

CREATE TABLE class_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
  class_name VARCHAR(128) NOT NULL COMMENT '班级名称',
  grade VARCHAR(32) NULL COMMENT '年级',
  major VARCHAR(128) NULL COMMENT '专业',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '班级表';

CREATE TABLE student_class (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_student_class (student_id, class_id),
  CONSTRAINT fk_student_class_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  CONSTRAINT fk_student_class_class FOREIGN KEY (class_id) REFERENCES class_info(id)
) COMMENT '学生班级关联表';

CREATE TABLE experiment_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验任务ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  title VARCHAR(128) NOT NULL COMMENT '实验任务标题',
  description TEXT NULL COMMENT '任务说明',
  deadline DATETIME NULL COMMENT '截止时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1进行中，2已截止，3已归档',
  created_by BIGINT NOT NULL COMMENT '创建人ID',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_task_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_task_class FOREIGN KEY (class_id) REFERENCES class_info(id),
  CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) COMMENT '实验任务表';

CREATE TABLE report_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
  task_id BIGINT NOT NULL COMMENT '实验任务ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
  file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
  file_type VARCHAR(32) NOT NULL COMMENT '文件类型',
  file_size BIGINT NOT NULL COMMENT '文件大小',
  parsed_text LONGTEXT NULL COMMENT '解析后的文本',
  word_count INT NOT NULL DEFAULT 0 COMMENT '文本字数',
  parse_status TINYINT NOT NULL DEFAULT 0 COMMENT '解析状态：0待解析，1成功，2失败',
  parse_message VARCHAR(500) NULL COMMENT '解析说明',
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  UNIQUE KEY uk_task_student (task_id, student_id),
  CONSTRAINT fk_report_task FOREIGN KEY (task_id) REFERENCES experiment_task(id),
  CONSTRAINT fk_report_student FOREIGN KEY (student_id) REFERENCES sys_user(id)
) COMMENT '实验报告表';

CREATE TABLE check_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '查重任务ID',
  experiment_task_id BIGINT NOT NULL COMMENT '实验任务ID',
  algorithm VARCHAR(64) NOT NULL DEFAULT 'TFIDF_COSINE' COMMENT '算法类型',
  report_count INT NOT NULL DEFAULT 0 COMMENT '参与报告数量',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待执行，1执行中，2已完成，3失败',
  start_time DATETIME NULL COMMENT '开始时间',
  end_time DATETIME NULL COMMENT '结束时间',
  created_by BIGINT NOT NULL COMMENT '创建人ID',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_check_task_experiment FOREIGN KEY (experiment_task_id) REFERENCES experiment_task(id),
  CONSTRAINT fk_check_task_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) COMMENT '查重任务表';

CREATE TABLE check_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '查重结果ID',
  check_task_id BIGINT NOT NULL COMMENT '查重任务ID',
  source_report_id BIGINT NOT NULL COMMENT '源报告ID',
  target_report_id BIGINT NOT NULL COMMENT '对比报告ID',
  cosine_similarity DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT '余弦相似度',
  simhash_similarity DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT 'SimHash相似度',
  final_similarity DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT '综合相似度',
  risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_result_check_task FOREIGN KEY (check_task_id) REFERENCES check_task(id),
  CONSTRAINT fk_result_source_report FOREIGN KEY (source_report_id) REFERENCES report_file(id),
  CONSTRAINT fk_result_target_report FOREIGN KEY (target_report_id) REFERENCES report_file(id)
) COMMENT '查重结果表';

CREATE TABLE similar_sentence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '相似句子ID',
  check_result_id BIGINT NOT NULL COMMENT '查重结果ID',
  source_sentence TEXT NOT NULL COMMENT '源报告句子',
  target_sentence TEXT NOT NULL COMMENT '对比报告句子',
  sentence_similarity DECIMAL(6,4) NOT NULL DEFAULT 0 COMMENT '句子相似度',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_sentence_result FOREIGN KEY (check_result_id) REFERENCES check_result(id)
) COMMENT '相似句子表';

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT NULL COMMENT '操作用户ID',
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  operation_content VARCHAR(500) NOT NULL COMMENT '操作内容',
  ip_address VARCHAR(64) NULL COMMENT 'IP地址',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT '操作日志表';

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '管理员', '系统管理员'),
(2, 'TEACHER', '教师', '课程教师'),
(3, 'STUDENT', '学生', '学生用户');

INSERT INTO sys_user (id, username, password, real_name, role_id, teacher_no, student_no, email) VALUES
(1, 'admin', '123456', '系统管理员', 1, NULL, NULL, 'admin@example.com'),
(2, 'teacher01', '123456', '张老师', 2, 'T2026001', NULL, 'teacher01@example.com'),
(3, 'teacher02', '123456', '刘老师', 2, 'T2026002', NULL, 'teacher02@example.com'),
(4, 'teacher03', '123456', '陈老师', 2, 'T2026003', NULL, 'teacher03@example.com'),
(101, 'student01', '123456', '李明', 3, NULL, '20260001', 'student01@example.com'),
(102, 'student02', '123456', '王芳', 3, NULL, '20260002', 'student02@example.com'),
(103, 'student03', '123456', '赵强', 3, NULL, '20260003', 'student03@example.com'),
(104, 'student04', '123456', '陈晨', 3, NULL, '20260004', 'student04@example.com'),
(105, 'student05', '123456', '孙悦', 3, NULL, '20260005', 'student05@example.com'),
(106, 'student06', '123456', '周杰', 3, NULL, '20260006', 'student06@example.com'),
(107, 'student07', '123456', '吴敏', 3, NULL, '20260007', 'student07@example.com'),
(108, 'student08', '123456', '郑磊', 3, NULL, '20260008', 'student08@example.com'),
(109, 'student09', '123456', '许阳', 3, NULL, '20260009', 'student09@example.com'),
(110, 'student10', '123456', '唐宁', 3, NULL, '20260010', 'student10@example.com'),
(111, 'student11', '123456', '何欣', 3, NULL, '20260011', 'student11@example.com'),
(112, 'student12', '123456', '罗杰', 3, NULL, '20260012', 'student12@example.com');

INSERT INTO class_info (class_name, grade, major) VALUES
('计算机科学与技术1班', '2023级', '计算机科学与技术'),
('软件工程1班', '2023级', '软件工程'),
('人工智能1班', '2023级', '人工智能');

INSERT INTO course (id, course_name, course_code, teacher_id, description) VALUES
(1, '计算机综合课程设计', 'CS-CD-2026', 2, '计算机综合课程设计实验课程'),
(2, 'Java Web应用开发', 'CS-JAVA-WEB-2026', 3, '基于 Spring Boot 和 Vue 的 Web 应用开发课程'),
(3, '软件工程实践', 'SE-PRACTICE-2026', 4, '面向需求分析、系统设计和测试实践的课程');

INSERT INTO student_class (student_id, class_id) VALUES
(101, 1),
(102, 1),
(103, 1),
(104, 1),
(105, 1),
(106, 1),
(107, 1),
(108, 1),
(109, 1),
(110, 1),
(111, 1),
(112, 1),
(101, 2),
(102, 2),
(103, 2),
(104, 2),
(105, 2),
(106, 2),
(107, 2),
(108, 2),
(109, 2),
(110, 2),
(111, 2),
(112, 2),
(101, 3),
(102, 3),
(103, 3),
(104, 3),
(105, 3),
(106, 3),
(107, 3),
(108, 3),
(109, 3),
(110, 3),
(111, 3),
(112, 3);

INSERT INTO experiment_task (id, course_id, class_id, title, description, deadline, created_by) VALUES
(1, 1, 1, '实验一：Web系统设计与实现', '提交一份包含需求分析、系统设计、核心代码说明和运行截图的实验报告，主演示格式为 txt。', '2026-06-15 23:59:59', 2),
(2, 2, 2, '实验二：文本相似度算法分析', '提交一份说明 TF-IDF、余弦相似度和 SimHash 算法流程的实验报告，主演示格式为 docx。', '2026-06-20 23:59:59', 3),
(3, 3, 3, '实验三：软件测试与质量分析', '围绕系统测试用例、缺陷分析和质量评价完成实验报告，主演示格式为 pdf。', '2026-06-22 23:59:59', 4);

SET FOREIGN_KEY_CHECKS = 1;

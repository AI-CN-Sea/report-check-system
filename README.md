# 实验报告智能查重与分析系统

本项目是面向高校实验教学场景，实现实验报告上传、文本解析、相似度计算、风险分级、相似句展示和统计分析等功能。

## 技术栈

- 后端：Spring Boot 3、Java 17、Maven、MyBatis-Plus、MySQL
- 前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios、ECharts
- 文档解析：Apache POI、PDFBox
- 相似度算法：TF-IDF 余弦相似度、SimHash 文本指纹、加权融合评分

## 目录结构

```text
report-check-system/
|-- backend/      Spring Boot 后端服务
|-- frontend/     Vue 3 前端项目
|-- database/     数据库初始化与迁移脚本
|-- docs/         需求、接口、部署、测试和演示文档
|-- samples/      演示用实验报告样例
|-- tools/        文档与演示数据生成脚本
```

## 安全说明

仓库不包含本地数据库密码、真实 API Key、运行日志、上传文件、构建产物或依赖目录。运行时配置请通过环境变量提供，可参考 `.env.example`。

关键环境变量：

```text
DB_URL=jdbc:mysql://localhost:3306/report_check_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=root
DB_PASSWORD=你的数据库密码
REPORT_CHECK_JWT_SECRET=请替换为至少 32 字节的随机密钥
REPORT_CHECK_UPLOAD_DIR=uploads
```

## 初始化数据库

1. 创建 MySQL 数据库：`report_check_system`
2. 执行初始化脚本：

```text
database/init.sql
```

如需升级注册、找回密码等功能，可继续执行：

```text
database/2026-05-30-auth-register-forgot-migration.sql
```

如需演示查重结果，可执行：

```text
database/demo_seed.sql
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```text
http://localhost:8080/api/health
```

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 构建与测试

后端测试：

```bash
cd backend
mvn test
```

前端生产构建：

```bash
cd frontend
npm run build
```

## 默认演示账号

初始化脚本内包含演示账号，默认密码仅用于本地课程设计演示，公开部署前请修改。

```text
admin / 123456
teacher01 / 123456
student01 / 123456
```

## 主要功能

- 用户登录、注册、找回密码和角色鉴权
- 管理员、教师、学生多角色页面
- 课程、班级、用户和实验任务管理
- txt、docx、pdf 实验报告上传与文本解析
- TF-IDF 余弦相似度与 SimHash 结合的查重算法
- 相似句详情、风险等级和统计图表展示
- 演示数据、接口文档、部署说明和测试用例

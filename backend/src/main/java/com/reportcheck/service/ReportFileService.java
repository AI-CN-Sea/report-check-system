package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.common.AuthContext;
import com.reportcheck.config.ReportCheckProperties;
import com.reportcheck.entity.CheckResult;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.entity.StudentClass;
import com.reportcheck.entity.SysRole;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.CheckResultMapper;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.StudentClassMapper;
import com.reportcheck.mapper.SysRoleMapper;
import com.reportcheck.mapper.SysUserMapper;
import com.reportcheck.util.TextExtractUtil;
import com.reportcheck.vo.ReportFileDetailVO;
import com.reportcheck.vo.ReportFileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportFileService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final List<String> ALLOWED_FILE_TYPES = List.of("txt", "docx", "pdf");

    private final ReportFileMapper reportFileMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final CourseMapper courseMapper;
    private final ExperimentTaskMapper experimentTaskMapper;
    private final StudentClassMapper studentClassMapper;
    private final CheckResultMapper checkResultMapper;
    private final TextExtractUtil textExtractUtil;
    private final ReportCheckProperties properties;

    public ReportFileService(
            ReportFileMapper reportFileMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            CourseMapper courseMapper,
            ExperimentTaskMapper experimentTaskMapper,
            StudentClassMapper studentClassMapper,
            CheckResultMapper checkResultMapper,
            TextExtractUtil textExtractUtil,
            ReportCheckProperties properties
    ) {
        this.reportFileMapper = reportFileMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.courseMapper = courseMapper;
        this.experimentTaskMapper = experimentTaskMapper;
        this.studentClassMapper = studentClassMapper;
        this.checkResultMapper = checkResultMapper;
        this.textExtractUtil = textExtractUtil;
        this.properties = properties;
    }

    public List<ReportFileVO> listReports(Long taskId) {
        LambdaQueryWrapper<ReportFile> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(ReportFile::getTaskId, taskId);
        }
        if (AuthContext.hasRole("TEACHER")) {
            List<Long> taskIds = accessibleExperimentTaskIds();
            if (taskIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(ReportFile::getTaskId, taskIds);
        }
        if (AuthContext.hasRole("STUDENT")) {
            wrapper.eq(ReportFile::getStudentId, AuthContext.currentUserId());
        }
        wrapper.orderByDesc(ReportFile::getUploadTime);
        return toVOList(reportFileMapper.selectList(wrapper));
    }

    public ReportFileDetailVO getReportDetail(Long id) {
        ReportFile report = reportFileMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在");
        }
        if (AuthContext.hasRole("STUDENT") && !AuthContext.currentUserId().equals(report.getStudentId())) {
            throw new IllegalArgumentException("只能查看自己的报告");
        }
        ensureTaskAccessible(report.getTaskId());
        ReportFileDetailVO vo = toDetailVO(report, userMapper.selectById(report.getStudentId()), experimentTaskMapper.selectById(report.getTaskId()));
        vo.setParsedText(report.getParsedText());
        return vo;
    }

    @Transactional
    public ReportFileVO upload(Long taskId, Long studentId, MultipartFile file) throws IOException {
        if (AuthContext.hasRole("STUDENT")) {
            studentId = AuthContext.currentUserId();
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件不能超过 20MB");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String fileType = getFileType(originalName);
        if (!ALLOWED_FILE_TYPES.contains(fileType)) {
            throw new IllegalArgumentException("只支持 txt、docx、pdf 文件");
        }
        ExperimentTask task = experimentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("实验任务不存在");
        }
        ensureTaskAccessible(task.getId());
        if (task.getStatus() != null && task.getStatus() != 1) {
            throw new IllegalArgumentException("该实验任务当前不可提交");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该实验任务已过截止时间");
        }
        SysUser student = userMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        SysRole role = roleMapper.selectById(student.getRoleId());
        if (role == null || !"STUDENT".equals(role.getRoleCode())) {
            throw new IllegalArgumentException("只能为学生用户上传报告");
        }
        ensureStudentBelongsToTaskClass(studentId, task);

        Path uploadRoot = Path.of(properties.getUploadDir()).toAbsolutePath();
        Path taskDir = uploadRoot.resolve(String.valueOf(taskId));
        Files.createDirectories(taskDir);
        String storedName = UUID.randomUUID() + "." + fileType;
        Path storedPath = taskDir.resolve(storedName);
        file.transferTo(storedPath);

        ReportFile report = new ReportFile();
        report.setTaskId(taskId);
        report.setStudentId(studentId);
        report.setOriginalName(originalName);
        report.setStoredName(storedName);
        report.setFilePath(storedPath.toString());
        report.setFileType(fileType);
        report.setFileSize(file.getSize());
        report.setUploadTime(LocalDateTime.now());

        try {
            String parsedText = textExtractUtil.extract(storedPath, fileType);
            if (parsedText == null || parsedText.isBlank()) {
                throw new IllegalArgumentException("未能提取到有效文本");
            }
            report.setParsedText(parsedText);
            report.setWordCount(countWords(parsedText));
            report.setParseStatus(1);
            report.setParseMessage(buildParseMessage(parsedText));
        } catch (Exception exception) {
            report.setParsedText("");
            report.setWordCount(0);
            report.setParseStatus(2);
            report.setParseMessage("解析失败：" + exception.getMessage());
        }

        ReportFile existing = reportFileMapper.selectOne(new LambdaQueryWrapper<ReportFile>()
                .eq(ReportFile::getTaskId, taskId)
                .eq(ReportFile::getStudentId, studentId));
        if (existing == null) {
            reportFileMapper.insert(report);
        } else {
            deletePhysicalFile(existing.getFilePath());
            report.setId(existing.getId());
            reportFileMapper.updateById(report);
        }
        return toVO(reportFileMapper.selectById(report.getId()), student, task);
    }

    @Transactional
    public List<ReportFileVO> batchUpload(Long taskId, MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("批量上传文件不能为空");
        }
        if (files.length > 20) {
            throw new IllegalArgumentException("一次最多上传 20 个文件");
        }
        if (AuthContext.hasRole("STUDENT")) {
            throw new IllegalArgumentException("学生端不支持批量上传");
        }
        List<ReportFileVO> uploaded = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            Long studentId = inferStudentId(file.getOriginalFilename());
            uploaded.add(upload(taskId, studentId, file));
        }
        return uploaded;
    }

    public void deleteReport(Long id) {
        ReportFile report = reportFileMapper.selectById(id);
        if (report != null) {
            ensureTaskAccessible(report.getTaskId());
            Long resultCount = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .eq(CheckResult::getSourceReportId, id)
                    .or()
                    .eq(CheckResult::getTargetReportId, id));
            if (resultCount > 0) {
                throw new IllegalArgumentException("该报告已参与查重，不能直接删除，请先处理相关查重结果");
            }
            deletePhysicalFile(report.getFilePath());
            reportFileMapper.deleteById(id);
        }
    }

    private List<ReportFileVO> toVOList(List<ReportFile> reports) {
        Map<Long, SysUser> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, ExperimentTask> taskMap = experimentTaskMapper.selectList(null).stream()
                .collect(Collectors.toMap(ExperimentTask::getId, Function.identity()));
        return reports.stream()
                .map(report -> toVO(report, userMap.get(report.getStudentId()), taskMap.get(report.getTaskId())))
                .toList();
    }

    private ReportFileVO toVO(ReportFile report, SysUser student, ExperimentTask task) {
        ReportFileVO vo = new ReportFileVO();
        vo.setId(report.getId());
        vo.setTaskId(report.getTaskId());
        vo.setTaskTitle(task == null ? "" : task.getTitle());
        vo.setStudentId(report.getStudentId());
        vo.setStudentName(student == null ? "" : student.getRealName());
        vo.setStudentNo(student == null ? "" : student.getStudentNo());
        vo.setOriginalName(report.getOriginalName());
        vo.setFileType(report.getFileType());
        vo.setFileSize(report.getFileSize());
        vo.setWordCount(report.getWordCount());
        vo.setParseStatus(report.getParseStatus());
        vo.setParseMessage(report.getParseMessage());
        vo.setUploadTime(report.getUploadTime());
        return vo;
    }

    private ReportFileDetailVO toDetailVO(ReportFile report, SysUser student, ExperimentTask task) {
        ReportFileVO base = toVO(report, student, task);
        ReportFileDetailVO vo = new ReportFileDetailVO();
        vo.setId(base.getId());
        vo.setTaskId(base.getTaskId());
        vo.setTaskTitle(base.getTaskTitle());
        vo.setStudentId(base.getStudentId());
        vo.setStudentName(base.getStudentName());
        vo.setStudentNo(base.getStudentNo());
        vo.setOriginalName(base.getOriginalName());
        vo.setFileType(base.getFileType());
        vo.setFileSize(base.getFileSize());
        vo.setWordCount(base.getWordCount());
        vo.setParseStatus(base.getParseStatus());
        vo.setParseMessage(base.getParseMessage());
        vo.setUploadTime(base.getUploadTime());
        return vo;
    }

    private String getFileType(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return filename.substring(index + 1).toLowerCase();
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
    }

    private String buildParseMessage(String text) {
        int wordCount = countWords(text);
        int sentenceCount = countSentences(text);
        if (wordCount < 100) {
            return "解析成功，文本偏短，字数：" + wordCount + "，句子数：" + sentenceCount + "，建议教师复核";
        }
        if (sentenceCount < 3) {
            return "解析成功，句子较少，字数：" + wordCount + "，句子数：" + sentenceCount + "，建议教师复核";
        }
        return "解析成功，字数：" + wordCount + "，句子数：" + sentenceCount;
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] parts = text.split("[。！？!?；;\\n]+");
        int count = 0;
        for (String part : parts) {
            if (part.trim().length() >= 8) {
                count++;
            }
        }
        return count;
    }

    private Long inferStudentId(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("批量上传文件名不能为空");
        }
        List<SysUser> users = userMapper.selectList(null);
        for (SysUser user : users) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role == null || !"STUDENT".equals(role.getRoleCode())) {
                continue;
            }
            if (contains(filename, user.getStudentNo())
                    || contains(filename, user.getUsername())
                    || contains(filename, user.getRealName())) {
                return user.getId();
            }
        }
        throw new IllegalArgumentException("无法从文件名识别学生：" + filename + "，请在文件名中包含学生姓名、用户名或学号");
    }

    private void ensureStudentBelongsToTaskClass(Long studentId, ExperimentTask task) {
        Long relationCount = studentClassMapper.selectCount(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, studentId)
                .eq(StudentClass::getClassId, task.getClassId()));
        if (relationCount == 0) {
            throw new IllegalArgumentException("该学生不属于当前实验任务对应班级，不能上传到该任务");
        }
    }

    private void ensureTaskAccessible(Long taskId) {
        if (!AuthContext.hasRole("TEACHER")) {
            return;
        }
        ExperimentTask task = experimentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("实验任务不存在");
        }
        Course course = courseMapper.selectById(task.getCourseId());
        boolean ownedByCreator = AuthContext.currentUserId().equals(task.getCreatedBy());
        boolean ownedByCourse = course != null && AuthContext.currentUserId().equals(course.getTeacherId());
        if (!ownedByCreator && !ownedByCourse) {
            throw new IllegalArgumentException("只能查看或操作自己课程下的报告");
        }
    }

    private List<Long> accessibleExperimentTaskIds() {
        Long teacherId = AuthContext.currentUserId();
        List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getTeacherId, teacherId))
                .stream()
                .map(Course::getId)
                .toList();
        LambdaQueryWrapper<ExperimentTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> {
            condition.eq(ExperimentTask::getCreatedBy, teacherId);
            if (!courseIds.isEmpty()) {
                condition.or().in(ExperimentTask::getCourseId, courseIds);
            }
        });
        return experimentTaskMapper.selectList(wrapper)
                .stream()
                .map(ExperimentTask::getId)
                .toList();
    }

    private boolean contains(String source, String target) {
        return target != null && !target.isBlank() && source.contains(target);
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // 删除旧文件失败不影响数据库记录更新。
        }
    }
}

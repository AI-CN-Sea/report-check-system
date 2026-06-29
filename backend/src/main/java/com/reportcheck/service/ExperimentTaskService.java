package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.dto.ExperimentTaskRequest;
import com.reportcheck.common.AuthContext;
import com.reportcheck.entity.CheckTask;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.entity.StudentClass;
import com.reportcheck.mapper.CheckTaskMapper;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.StudentClassMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExperimentTaskService {

    private final ExperimentTaskMapper experimentTaskMapper;
    private final CourseMapper courseMapper;
    private final StudentClassMapper studentClassMapper;
    private final ReportFileMapper reportFileMapper;
    private final CheckTaskMapper checkTaskMapper;

    public ExperimentTaskService(
            ExperimentTaskMapper experimentTaskMapper,
            CourseMapper courseMapper,
            StudentClassMapper studentClassMapper,
            ReportFileMapper reportFileMapper,
            CheckTaskMapper checkTaskMapper
    ) {
        this.experimentTaskMapper = experimentTaskMapper;
        this.courseMapper = courseMapper;
        this.studentClassMapper = studentClassMapper;
        this.reportFileMapper = reportFileMapper;
        this.checkTaskMapper = checkTaskMapper;
    }

    public List<ExperimentTask> listTasks() {
        LambdaQueryWrapper<ExperimentTask> wrapper = new LambdaQueryWrapper<>();
        if (AuthContext.hasRole("TEACHER")) {
            Long teacherId = AuthContext.currentUserId();
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                            .eq(Course::getTeacherId, teacherId))
                    .stream()
                    .map(Course::getId)
                    .toList();
            wrapper.and(condition -> {
                condition.eq(ExperimentTask::getCreatedBy, teacherId);
                if (!courseIds.isEmpty()) {
                    condition.or().in(ExperimentTask::getCourseId, courseIds);
                }
            });
        }
        if (AuthContext.hasRole("STUDENT")) {
            Set<Long> classIds = studentClassMapper.selectList(new LambdaQueryWrapper<StudentClass>()
                            .eq(StudentClass::getStudentId, AuthContext.currentUserId()))
                    .stream()
                    .map(StudentClass::getClassId)
                    .collect(Collectors.toSet());
            if (classIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(ExperimentTask::getClassId, classIds);
        }
        wrapper.orderByDesc(ExperimentTask::getCreatedTime);
        return experimentTaskMapper.selectList(wrapper);
    }

    public ExperimentTask createTask(ExperimentTaskRequest request) {
        ensureTeacherOwnsCourse(request.getCourseId());
        ExperimentTask task = toEntity(request);
        experimentTaskMapper.insert(task);
        return task;
    }

    public ExperimentTask updateTask(Long id, ExperimentTaskRequest request) {
        ensureTaskAccessible(id);
        ensureTeacherOwnsCourse(request.getCourseId());
        ExperimentTask task = toEntity(request);
        task.setId(id);
        experimentTaskMapper.updateById(task);
        return experimentTaskMapper.selectById(id);
    }

    public void deleteTask(Long id) {
        ExperimentTask task = experimentTaskMapper.selectById(id);
        if (task == null) {
            return;
        }
        ensureTaskAccessible(id);
        Long reportCount = reportFileMapper.selectCount(new LambdaQueryWrapper<ReportFile>()
                .eq(ReportFile::getTaskId, id));
        if (reportCount > 0) {
            throw new IllegalArgumentException("该实验任务已有报告，不能删除，建议将任务状态改为已归档");
        }
        Long checkTaskCount = checkTaskMapper.selectCount(new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getExperimentTaskId, id));
        if (checkTaskCount > 0) {
            throw new IllegalArgumentException("该实验任务已有查重记录，不能删除，建议将任务状态改为已归档");
        }
        experimentTaskMapper.deleteById(id);
    }

    private ExperimentTask toEntity(ExperimentTaskRequest request) {
        ExperimentTask task = new ExperimentTask();
        task.setCourseId(request.getCourseId());
        task.setClassId(request.getClassId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        task.setCreatedBy(resolveCreatorId(request.getCreatedBy()));
        return task;
    }

    private Long resolveCreatorId(Long requestedCreatorId) {
        Long currentUserId = AuthContext.currentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        return requestedCreatorId == null ? 2L : requestedCreatorId;
    }

    private void ensureTeacherOwnsCourse(Long courseId) {
        if (!AuthContext.hasRole("TEACHER")) {
            return;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || !AuthContext.currentUserId().equals(course.getTeacherId())) {
            throw new IllegalArgumentException("只能为自己任课的课程创建或修改实验任务");
        }
    }

    private void ensureTaskAccessible(Long taskId) {
        if (!AuthContext.hasRole("TEACHER")) {
            return;
        }
        ExperimentTask task = experimentTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        Course course = courseMapper.selectById(task.getCourseId());
        boolean ownedByCreator = AuthContext.currentUserId().equals(task.getCreatedBy());
        boolean ownedByCourse = course != null && AuthContext.currentUserId().equals(course.getTeacherId());
        if (!ownedByCreator && !ownedByCourse) {
            throw new IllegalArgumentException("只能操作自己课程下的实验任务");
        }
    }
}

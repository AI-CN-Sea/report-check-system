package com.reportcheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.common.AuthContext;
import com.reportcheck.common.Result;
import com.reportcheck.entity.CheckResult;
import com.reportcheck.entity.CheckTask;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.mapper.CheckResultMapper;
import com.reportcheck.mapper.CheckTaskMapper;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final CourseMapper courseMapper;
    private final ExperimentTaskMapper experimentTaskMapper;
    private final SysUserMapper userMapper;
    private final ReportFileMapper reportFileMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final CheckResultMapper checkResultMapper;

    public StatisticsController(
            CourseMapper courseMapper,
            ExperimentTaskMapper experimentTaskMapper,
            SysUserMapper userMapper,
            ReportFileMapper reportFileMapper,
            CheckTaskMapper checkTaskMapper,
            CheckResultMapper checkResultMapper
    ) {
        this.courseMapper = courseMapper;
        this.experimentTaskMapper = experimentTaskMapper;
        this.userMapper = userMapper;
        this.reportFileMapper = reportFileMapper;
        this.checkTaskMapper = checkTaskMapper;
        this.checkResultMapper = checkResultMapper;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (AuthContext.hasRole("TEACHER")) {
            List<Long> experimentTaskIds = accessibleExperimentTaskIds();
            List<Long> checkTaskIds = accessibleCheckTaskIds(experimentTaskIds);
            data.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, AuthContext.currentUserId())));
            data.put("taskCount", experimentTaskIds.size());
            data.put("userCount", userMapper.selectCount(null));
            data.put("reportCount", experimentTaskIds.isEmpty() ? 0 : reportFileMapper.selectCount(new LambdaQueryWrapper<ReportFile>()
                    .in(ReportFile::getTaskId, experimentTaskIds)));
            data.put("checkTaskCount", checkTaskIds.size());
            data.put("highRiskCount", checkTaskIds.isEmpty() ? 0 : checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .in(CheckResult::getCheckTaskId, checkTaskIds)
                    .eq(CheckResult::getRiskLevel, "高风险")));
        } else {
            data.put("courseCount", courseMapper.selectCount(null));
            data.put("taskCount", experimentTaskMapper.selectCount(null));
            data.put("userCount", userMapper.selectCount(null));
            data.put("reportCount", reportFileMapper.selectCount(null));
            data.put("checkTaskCount", checkTaskMapper.selectCount(null));
            data.put("highRiskCount", checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .eq(CheckResult::getRiskLevel, "高风险")));
        }
        return Result.ok(data);
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

    private List<Long> accessibleCheckTaskIds(List<Long> experimentTaskIds) {
        if (experimentTaskIds.isEmpty()) {
            return List.of();
        }
        return checkTaskMapper.selectList(new LambdaQueryWrapper<CheckTask>()
                        .in(CheckTask::getExperimentTaskId, experimentTaskIds))
                .stream()
                .map(CheckTask::getId)
                .toList();
    }
}

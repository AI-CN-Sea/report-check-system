package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.common.AuthContext;
import com.reportcheck.entity.CheckResult;
import com.reportcheck.entity.CheckTask;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.entity.SimilarSentence;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.CheckResultMapper;
import com.reportcheck.mapper.CheckTaskMapper;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.SimilarSentenceMapper;
import com.reportcheck.mapper.SysUserMapper;
import com.reportcheck.vo.CheckResultDetailVO;
import com.reportcheck.vo.CheckResultVO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckResultService {

    private final CheckResultMapper checkResultMapper;
    private final SimilarSentenceMapper similarSentenceMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final CourseMapper courseMapper;
    private final ExperimentTaskMapper experimentTaskMapper;
    private final ReportFileMapper reportFileMapper;
    private final SysUserMapper userMapper;

    public CheckResultService(
            CheckResultMapper checkResultMapper,
            SimilarSentenceMapper similarSentenceMapper,
            CheckTaskMapper checkTaskMapper,
            CourseMapper courseMapper,
            ExperimentTaskMapper experimentTaskMapper,
            ReportFileMapper reportFileMapper,
            SysUserMapper userMapper
    ) {
        this.checkResultMapper = checkResultMapper;
        this.similarSentenceMapper = similarSentenceMapper;
        this.checkTaskMapper = checkTaskMapper;
        this.courseMapper = courseMapper;
        this.experimentTaskMapper = experimentTaskMapper;
        this.reportFileMapper = reportFileMapper;
        this.userMapper = userMapper;
    }

    public List<CheckResultVO> listResults(Long checkTaskId) {
        LambdaQueryWrapper<CheckResult> wrapper = new LambdaQueryWrapper<>();
        if (checkTaskId != null) {
            wrapper.eq(CheckResult::getCheckTaskId, checkTaskId);
        }
        if (AuthContext.hasRole("TEACHER")) {
            List<Long> checkTaskIds = accessibleCheckTaskIds();
            if (checkTaskIds.isEmpty()) {
                return List.of();
            }
            if (checkTaskId != null && !checkTaskIds.contains(checkTaskId)) {
                return List.of();
            }
            wrapper.in(CheckResult::getCheckTaskId, checkTaskIds);
        }
        wrapper.orderByDesc(CheckResult::getFinalSimilarity);
        return toVOList(checkResultMapper.selectList(wrapper));
    }

    public CheckResultDetailVO getDetail(Long id) {
        CheckResult result = checkResultMapper.selectById(id);
        if (result == null) {
            throw new IllegalArgumentException("查重结果不存在");
        }
        ensureCheckTaskAccessible(result.getCheckTaskId());
        CheckResultDetailVO detail = new CheckResultDetailVO();
        List<SimilarSentence> sentences = similarSentenceMapper.selectList(new LambdaQueryWrapper<SimilarSentence>()
                .eq(SimilarSentence::getCheckResultId, id)
                .orderByDesc(SimilarSentence::getSentenceSimilarity));
        detail.setResult(toVO(result, reportFileMapper.selectById(result.getSourceReportId()), reportFileMapper.selectById(result.getTargetReportId())));
        detail.setSimilarSentences(sentences);
        detail.setSimilarSentenceCount(sentences.size());
        if (sentences.isEmpty()) {
            detail.setDetailMessage("当前结果未生成相似句，可能是报告句子过短、句子级相似度未达到阈值，或该结果来自旧演示数据。请重新上传完整报告并发起查重。");
        }
        return detail;
    }

    public byte[] exportCsv(Long checkTaskId) {
        List<CheckResultVO> results = listResults(checkTaskId);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("结果ID,查重任务ID,报告A学生,报告A文件,报告B学生,报告B文件,余弦相似度,SimHash相似度,综合相似度,风险等级,创建时间\n");
        for (CheckResultVO result : results) {
            builder.append(csv(result.getId())).append(',')
                    .append(csv(result.getCheckTaskId())).append(',')
                    .append(csv(result.getSourceStudentName())).append(',')
                    .append(csv(result.getSourceReportName())).append(',')
                    .append(csv(result.getTargetStudentName())).append(',')
                    .append(csv(result.getTargetReportName())).append(',')
                    .append(csv(result.getCosineSimilarity())).append(',')
                    .append(csv(result.getSimhashSimilarity())).append(',')
                    .append(csv(result.getFinalSimilarity())).append(',')
                    .append(csv(result.getRiskLevel())).append(',')
                    .append(csv(result.getCreatedTime()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<CheckResultVO> toVOList(List<CheckResult> results) {
        return results.stream()
                .map(result -> toVO(result, reportFileMapper.selectById(result.getSourceReportId()), reportFileMapper.selectById(result.getTargetReportId())))
                .toList();
    }

    private CheckResultVO toVO(CheckResult result, ReportFile sourceReport, ReportFile targetReport) {
        Map<Long, SysUser> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        SysUser sourceUser = sourceReport == null ? null : userMap.get(sourceReport.getStudentId());
        SysUser targetUser = targetReport == null ? null : userMap.get(targetReport.getStudentId());

        CheckResultVO vo = new CheckResultVO();
        vo.setId(result.getId());
        vo.setCheckTaskId(result.getCheckTaskId());
        vo.setSourceStudentName(sourceUser == null ? "" : sourceUser.getRealName());
        vo.setSourceReportName(sourceReport == null ? "" : sourceReport.getOriginalName());
        vo.setTargetStudentName(targetUser == null ? "" : targetUser.getRealName());
        vo.setTargetReportName(targetReport == null ? "" : targetReport.getOriginalName());
        vo.setCosineSimilarity(result.getCosineSimilarity());
        vo.setSimhashSimilarity(result.getSimhashSimilarity());
        vo.setFinalSimilarity(result.getFinalSimilarity());
        vo.setRiskLevel(result.getRiskLevel());
        vo.setCreatedTime(result.getCreatedTime());
        return vo;
    }

    private void ensureCheckTaskAccessible(Long checkTaskId) {
        if (!AuthContext.hasRole("TEACHER")) {
            return;
        }
        if (!accessibleCheckTaskIds().contains(checkTaskId)) {
            throw new IllegalArgumentException("只能查看自己课程下的查重结果");
        }
    }

    private List<Long> accessibleCheckTaskIds() {
        List<Long> experimentTaskIds = accessibleExperimentTaskIds();
        if (experimentTaskIds.isEmpty()) {
            return List.of();
        }
        return checkTaskMapper.selectList(new LambdaQueryWrapper<CheckTask>()
                        .in(CheckTask::getExperimentTaskId, experimentTaskIds))
                .stream()
                .map(CheckTask::getId)
                .toList();
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

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}

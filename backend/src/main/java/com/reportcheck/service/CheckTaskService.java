package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.algorithm.SimilarityCalculator;
import com.reportcheck.common.AuthContext;
import com.reportcheck.dto.CheckTaskRequest;
import com.reportcheck.entity.CheckResult;
import com.reportcheck.entity.CheckTask;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.entity.SimilarSentence;
import com.reportcheck.mapper.CheckResultMapper;
import com.reportcheck.mapper.CheckTaskMapper;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.SimilarSentenceMapper;
import com.reportcheck.vo.CheckTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckTaskService {

    private final CheckTaskMapper checkTaskMapper;
    private final CheckResultMapper checkResultMapper;
    private final SimilarSentenceMapper similarSentenceMapper;
    private final CourseMapper courseMapper;
    private final ReportFileMapper reportFileMapper;
    private final ExperimentTaskMapper experimentTaskMapper;
    private final SimilarityCalculator similarityCalculator;

    public CheckTaskService(
            CheckTaskMapper checkTaskMapper,
            CheckResultMapper checkResultMapper,
            SimilarSentenceMapper similarSentenceMapper,
            CourseMapper courseMapper,
            ReportFileMapper reportFileMapper,
            ExperimentTaskMapper experimentTaskMapper,
            SimilarityCalculator similarityCalculator
    ) {
        this.checkTaskMapper = checkTaskMapper;
        this.checkResultMapper = checkResultMapper;
        this.similarSentenceMapper = similarSentenceMapper;
        this.courseMapper = courseMapper;
        this.reportFileMapper = reportFileMapper;
        this.experimentTaskMapper = experimentTaskMapper;
        this.similarityCalculator = similarityCalculator;
    }

    public List<CheckTaskVO> listCheckTasks() {
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        if (AuthContext.hasRole("TEACHER")) {
            List<Long> experimentTaskIds = accessibleExperimentTaskIds();
            if (experimentTaskIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(CheckTask::getExperimentTaskId, experimentTaskIds);
        }
        wrapper.orderByDesc(CheckTask::getCreatedTime);
        List<CheckTask> tasks = checkTaskMapper.selectList(wrapper);
        return toVOList(tasks);
    }

    @Transactional
    public CheckTaskVO startCheck(CheckTaskRequest request) {
        ExperimentTask experimentTask = experimentTaskMapper.selectById(request.getExperimentTaskId());
        if (experimentTask == null) {
            throw new IllegalArgumentException("实验任务不存在");
        }
        ensureTaskAccessible(experimentTask);
        List<ReportFile> reports = reportFileMapper.selectList(new LambdaQueryWrapper<ReportFile>()
                .eq(ReportFile::getTaskId, request.getExperimentTaskId())
                .eq(ReportFile::getParseStatus, 1));
        if (reports.size() < 2) {
            throw new IllegalArgumentException("至少需要 2 份解析成功的报告才能查重");
        }

        CheckTask checkTask = new CheckTask();
        checkTask.setExperimentTaskId(request.getExperimentTaskId());
        checkTask.setAlgorithm("TFIDF_COSINE_SIMHASH");
        checkTask.setReportCount(reports.size());
        checkTask.setStatus(1);
        checkTask.setStartTime(LocalDateTime.now());
        checkTask.setCreatedBy(resolveCreatorId(request.getCreatedBy()));
        checkTaskMapper.insert(checkTask);

        for (int i = 0; i < reports.size(); i++) {
            for (int j = i + 1; j < reports.size(); j++) {
                createResult(checkTask.getId(), reports.get(i), reports.get(j), reports);
            }
        }

        checkTask.setStatus(2);
        checkTask.setEndTime(LocalDateTime.now());
        checkTaskMapper.updateById(checkTask);
        return toVO(checkTaskMapper.selectById(checkTask.getId()), experimentTask);
    }

    private void createResult(Long checkTaskId, ReportFile source, ReportFile target, List<ReportFile> reports) {
        List<String> corpusTexts = reports.stream()
                .map(ReportFile::getParsedText)
                .toList();
        SimilarityCalculator.SimilarityScore score = similarityCalculator.calculateWithCorpus(
                source.getParsedText(),
                target.getParsedText(),
                corpusTexts
        );
        CheckResult result = new CheckResult();
        result.setCheckTaskId(checkTaskId);
        result.setSourceReportId(source.getId());
        result.setTargetReportId(target.getId());
        result.setCosineSimilarity(BigDecimal.valueOf(score.getCosineSimilarity()));
        result.setSimhashSimilarity(BigDecimal.valueOf(score.getSimhashSimilarity()));
        result.setFinalSimilarity(BigDecimal.valueOf(score.getFinalSimilarity()));
        result.setRiskLevel(similarityCalculator.riskLevel(score.getFinalSimilarity()));
        checkResultMapper.insert(result);

        List<SimilarityCalculator.SimilarSentenceMatch> matches = similarityCalculator.findSimilarSentences(
                source.getParsedText(),
                target.getParsedText()
        );
        for (SimilarityCalculator.SimilarSentenceMatch match : matches) {
            SimilarSentence sentence = new SimilarSentence();
            sentence.setCheckResultId(result.getId());
            sentence.setSourceSentence(match.getSourceSentence());
            sentence.setTargetSentence(match.getTargetSentence());
            sentence.setSentenceSimilarity(BigDecimal.valueOf(match.getSimilarity()));
            similarSentenceMapper.insert(sentence);
        }
    }

    private Long resolveCreatorId(Long requestedCreatorId) {
        Long currentUserId = AuthContext.currentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        return requestedCreatorId == null ? 2L : requestedCreatorId;
    }

    private List<CheckTaskVO> toVOList(List<CheckTask> tasks) {
        Map<Long, ExperimentTask> experimentTaskMap = experimentTaskMapper.selectList(null).stream()
                .collect(Collectors.toMap(ExperimentTask::getId, Function.identity()));
        return tasks.stream()
                .map(task -> toVO(task, experimentTaskMap.get(task.getExperimentTaskId())))
                .toList();
    }

    private CheckTaskVO toVO(CheckTask task, ExperimentTask experimentTask) {
        CheckTaskVO vo = new CheckTaskVO();
        vo.setId(task.getId());
        vo.setExperimentTaskId(task.getExperimentTaskId());
        vo.setExperimentTaskTitle(experimentTask == null ? "" : experimentTask.getTitle());
        vo.setAlgorithm(task.getAlgorithm());
        vo.setReportCount(task.getReportCount());
        vo.setParsedReportCount(countParsedReports(task.getExperimentTaskId()));
        vo.setResultCount(countResults(task.getId()));
        vo.setStatus(task.getStatus());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setCreatedTime(task.getCreatedTime());
        return vo;
    }

    private Integer countParsedReports(Long experimentTaskId) {
        if (experimentTaskId == null) {
            return 0;
        }
        return reportFileMapper.selectCount(new LambdaQueryWrapper<ReportFile>()
                .eq(ReportFile::getTaskId, experimentTaskId)
                .eq(ReportFile::getParseStatus, 1)).intValue();
    }

    private Integer countResults(Long checkTaskId) {
        if (checkTaskId == null) {
            return 0;
        }
        return checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                .eq(CheckResult::getCheckTaskId, checkTaskId)).intValue();
    }

    private void ensureTaskAccessible(ExperimentTask task) {
        if (!AuthContext.hasRole("TEACHER")) {
            return;
        }
        Course course = courseMapper.selectById(task.getCourseId());
        boolean ownedByCreator = AuthContext.currentUserId().equals(task.getCreatedBy());
        boolean ownedByCourse = course != null && AuthContext.currentUserId().equals(course.getTeacherId());
        if (!ownedByCreator && !ownedByCourse) {
            throw new IllegalArgumentException("只能对自己课程下的实验任务发起查重");
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
}

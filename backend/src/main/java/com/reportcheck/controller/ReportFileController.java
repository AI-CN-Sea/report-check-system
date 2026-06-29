package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.service.ReportFileService;
import com.reportcheck.vo.ReportFileDetailVO;
import com.reportcheck.vo.ReportFileVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportFileController {

    private final ReportFileService reportFileService;

    public ReportFileController(ReportFileService reportFileService) {
        this.reportFileService = reportFileService;
    }

    @GetMapping
    public Result<List<ReportFileVO>> listReports(@RequestParam(required = false) Long taskId) {
        return Result.ok(reportFileService.listReports(taskId));
    }

    @GetMapping("/{id}")
    public Result<ReportFileDetailVO> getReportDetail(@PathVariable Long id) {
        return Result.ok(reportFileService.getReportDetail(id));
    }

    @PostMapping("/upload")
    public Result<ReportFileVO> upload(
            @RequestParam Long taskId,
            @RequestParam Long studentId,
            @RequestParam MultipartFile file
    ) throws IOException {
        return Result.ok(reportFileService.upload(taskId, studentId, file));
    }

    @PostMapping("/batch-upload")
    public Result<List<ReportFileVO>> batchUpload(
            @RequestParam Long taskId,
            @RequestParam MultipartFile[] files
    ) throws IOException {
        return Result.ok(reportFileService.batchUpload(taskId, files));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReport(@PathVariable Long id) {
        reportFileService.deleteReport(id);
        return Result.ok();
    }
}

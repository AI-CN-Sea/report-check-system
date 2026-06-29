package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.service.CheckResultService;
import com.reportcheck.vo.CheckResultDetailVO;
import com.reportcheck.vo.CheckResultVO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check-results")
public class CheckResultController {

    private final CheckResultService checkResultService;

    public CheckResultController(CheckResultService checkResultService) {
        this.checkResultService = checkResultService;
    }

    @GetMapping
    public Result<List<CheckResultVO>> listResults(@RequestParam(required = false) Long checkTaskId) {
        return Result.ok(checkResultService.listResults(checkTaskId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportResults(@RequestParam(required = false) Long checkTaskId) {
        byte[] csv = checkResultService.exportCsv(checkTaskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("check-results.csv")
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/{id}")
    public Result<CheckResultDetailVO> getDetail(@PathVariable Long id) {
        return Result.ok(checkResultService.getDetail(id));
    }
}

package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.CheckTaskRequest;
import com.reportcheck.service.CheckTaskService;
import com.reportcheck.vo.CheckTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check-tasks")
public class CheckTaskController {

    private final CheckTaskService checkTaskService;

    public CheckTaskController(CheckTaskService checkTaskService) {
        this.checkTaskService = checkTaskService;
    }

    @GetMapping
    public Result<List<CheckTaskVO>> listCheckTasks() {
        return Result.ok(checkTaskService.listCheckTasks());
    }

    @PostMapping
    public Result<CheckTaskVO> startCheck(@Valid @RequestBody CheckTaskRequest request) {
        return Result.ok(checkTaskService.startCheck(request));
    }
}

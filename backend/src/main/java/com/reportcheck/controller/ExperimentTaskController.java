package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.ExperimentTaskRequest;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.service.ExperimentTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/experiment-tasks")
public class ExperimentTaskController {

    private final ExperimentTaskService experimentTaskService;

    public ExperimentTaskController(ExperimentTaskService experimentTaskService) {
        this.experimentTaskService = experimentTaskService;
    }

    @GetMapping
    public Result<List<ExperimentTask>> listExperimentTasks() {
        return Result.ok(experimentTaskService.listTasks());
    }

    @PostMapping
    public Result<ExperimentTask> createExperimentTask(@Valid @RequestBody ExperimentTaskRequest request) {
        return Result.ok(experimentTaskService.createTask(request));
    }

    @PutMapping("/{id}")
    public Result<ExperimentTask> updateExperimentTask(@PathVariable Long id, @Valid @RequestBody ExperimentTaskRequest request) {
        return Result.ok(experimentTaskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteExperimentTask(@PathVariable Long id) {
        experimentTaskService.deleteTask(id);
        return Result.ok();
    }
}

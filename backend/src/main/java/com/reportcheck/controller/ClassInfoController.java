package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.ClassInfoRequest;
import com.reportcheck.entity.ClassInfo;
import com.reportcheck.service.ClassInfoService;
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
@RequestMapping("/api/classes")
public class ClassInfoController {

    private final ClassInfoService classInfoService;

    public ClassInfoController(ClassInfoService classInfoService) {
        this.classInfoService = classInfoService;
    }

    @GetMapping
    public Result<List<ClassInfo>> listClasses() {
        return Result.ok(classInfoService.listClasses());
    }

    @PostMapping
    public Result<ClassInfo> createClass(@Valid @RequestBody ClassInfoRequest request) {
        return Result.ok(classInfoService.createClass(request));
    }

    @PutMapping("/{id}")
    public Result<ClassInfo> updateClass(@PathVariable Long id, @Valid @RequestBody ClassInfoRequest request) {
        return Result.ok(classInfoService.updateClass(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        classInfoService.deleteClass(id);
        return Result.ok();
    }
}

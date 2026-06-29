package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.StudentClassRequest;
import com.reportcheck.service.StudentClassService;
import com.reportcheck.vo.StudentClassVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student-classes")
public class StudentClassController {

    private final StudentClassService studentClassService;

    public StudentClassController(StudentClassService studentClassService) {
        this.studentClassService = studentClassService;
    }

    @GetMapping
    public Result<List<StudentClassVO>> list(@RequestParam(required = false) Long classId) {
        return Result.ok(studentClassService.list(classId));
    }

    @PostMapping
    public Result<StudentClassVO> add(@Valid @RequestBody StudentClassRequest request) {
        return Result.ok(studentClassService.add(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        studentClassService.remove(id);
        return Result.ok();
    }
}

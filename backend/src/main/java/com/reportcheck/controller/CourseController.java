package com.reportcheck.controller;

import com.reportcheck.common.Result;
import com.reportcheck.dto.CourseRequest;
import com.reportcheck.entity.Course;
import com.reportcheck.service.CourseService;
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
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Result<List<Course>> listCourses() {
        return Result.ok(courseService.listCourses());
    }

    @PostMapping
    public Result<Course> createCourse(@Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    public Result<Course> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.ok();
    }
}

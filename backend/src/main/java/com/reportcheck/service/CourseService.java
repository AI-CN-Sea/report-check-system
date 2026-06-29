package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.common.AuthContext;
import com.reportcheck.dto.CourseRequest;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.SysRole;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.SysRoleMapper;
import com.reportcheck.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final ExperimentTaskMapper experimentTaskMapper;

    public CourseService(
            CourseMapper courseMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            ExperimentTaskMapper experimentTaskMapper
    ) {
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.experimentTaskMapper = experimentTaskMapper;
    }

    public List<Course> listCourses() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (AuthContext.hasRole("TEACHER")) {
            wrapper.eq(Course::getTeacherId, AuthContext.currentUserId());
        }
        wrapper.orderByDesc(Course::getCreatedTime);
        return courseMapper.selectList(wrapper);
    }

    public Course createCourse(CourseRequest request) {
        validateTeacher(request.getTeacherId());
        ensureCourseCodeUnique(request.getCourseCode(), null);
        Course course = toEntity(request);
        courseMapper.insert(course);
        return course;
    }

    public Course updateCourse(Long id, CourseRequest request) {
        if (courseMapper.selectById(id) == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        validateTeacher(request.getTeacherId());
        ensureCourseCodeUnique(request.getCourseCode(), id);
        Course course = toEntity(request);
        course.setId(id);
        courseMapper.updateById(course);
        return courseMapper.selectById(id);
    }

    public void deleteCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return;
        }
        Long taskCount = experimentTaskMapper.selectCount(new LambdaQueryWrapper<ExperimentTask>()
                .eq(ExperimentTask::getCourseId, id));
        if (taskCount > 0) {
            throw new IllegalArgumentException("该课程下已有实验任务，不能删除，请先删除或归档相关实验任务");
        }
        courseMapper.deleteById(id);
    }

    private Course toEntity(CourseRequest request) {
        Course course = new Course();
        course.setCourseName(request.getCourseName());
        course.setCourseCode(request.getCourseCode());
        course.setTeacherId(request.getTeacherId());
        course.setDescription(request.getDescription());
        return course;
    }

    private void validateTeacher(Long teacherId) {
        SysUser user = userMapper.selectById(teacherId);
        if (user == null) {
            throw new IllegalArgumentException("教师用户不存在");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null || !"TEACHER".equals(role.getRoleCode())) {
            throw new IllegalArgumentException("课程只能关联教师用户");
        }
    }

    private void ensureCourseCodeUnique(String courseCode, Long excludeId) {
        Course existing = courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseCode, courseCode));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("课程编码已存在");
        }
    }
}

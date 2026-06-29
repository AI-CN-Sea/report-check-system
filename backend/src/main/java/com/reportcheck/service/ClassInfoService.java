package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.dto.ClassInfoRequest;
import com.reportcheck.entity.ClassInfo;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.StudentClass;
import com.reportcheck.mapper.ClassInfoMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.StudentClassMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassInfoService {

    private final ClassInfoMapper classInfoMapper;
    private final StudentClassMapper studentClassMapper;
    private final ExperimentTaskMapper experimentTaskMapper;

    public ClassInfoService(
            ClassInfoMapper classInfoMapper,
            StudentClassMapper studentClassMapper,
            ExperimentTaskMapper experimentTaskMapper
    ) {
        this.classInfoMapper = classInfoMapper;
        this.studentClassMapper = studentClassMapper;
        this.experimentTaskMapper = experimentTaskMapper;
    }

    public List<ClassInfo> listClasses() {
        return classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>().orderByDesc(ClassInfo::getCreatedTime));
    }

    public ClassInfo createClass(ClassInfoRequest request) {
        ClassInfo classInfo = toEntity(request);
        classInfoMapper.insert(classInfo);
        return classInfo;
    }

    public ClassInfo updateClass(Long id, ClassInfoRequest request) {
        if (classInfoMapper.selectById(id) == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        ClassInfo classInfo = toEntity(request);
        classInfo.setId(id);
        classInfoMapper.updateById(classInfo);
        return classInfoMapper.selectById(id);
    }

    public void deleteClass(Long id) {
        ClassInfo classInfo = classInfoMapper.selectById(id);
        if (classInfo == null) {
            return;
        }
        Long studentCount = studentClassMapper.selectCount(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getClassId, id));
        if (studentCount > 0) {
            throw new IllegalArgumentException("该班级已有学生，不能删除，请先移除班级学生关系");
        }
        Long taskCount = experimentTaskMapper.selectCount(new LambdaQueryWrapper<ExperimentTask>()
                .eq(ExperimentTask::getClassId, id));
        if (taskCount > 0) {
            throw new IllegalArgumentException("该班级已有实验任务，不能删除，请先删除或归档相关实验任务");
        }
        classInfoMapper.deleteById(id);
    }

    private ClassInfo toEntity(ClassInfoRequest request) {
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(request.getClassName());
        classInfo.setGrade(request.getGrade());
        classInfo.setMajor(request.getMajor());
        return classInfo;
    }
}

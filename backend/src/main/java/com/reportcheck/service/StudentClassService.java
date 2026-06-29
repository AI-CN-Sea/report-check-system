package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.dto.StudentClassRequest;
import com.reportcheck.entity.ClassInfo;
import com.reportcheck.entity.StudentClass;
import com.reportcheck.entity.SysRole;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.ClassInfoMapper;
import com.reportcheck.mapper.StudentClassMapper;
import com.reportcheck.mapper.SysRoleMapper;
import com.reportcheck.mapper.SysUserMapper;
import com.reportcheck.vo.StudentClassVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentClassService {

    private final StudentClassMapper studentClassMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final ClassInfoMapper classInfoMapper;

    public StudentClassService(
            StudentClassMapper studentClassMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            ClassInfoMapper classInfoMapper
    ) {
        this.studentClassMapper = studentClassMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.classInfoMapper = classInfoMapper;
    }

    public List<StudentClassVO> list(Long classId) {
        LambdaQueryWrapper<StudentClass> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            wrapper.eq(StudentClass::getClassId, classId);
        }
        wrapper.orderByDesc(StudentClass::getCreatedTime);
        return toVOList(studentClassMapper.selectList(wrapper));
    }

    public StudentClassVO add(StudentClassRequest request) {
        validateStudent(request.getStudentId());
        if (classInfoMapper.selectById(request.getClassId()) == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        StudentClass existing = studentClassMapper.selectOne(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, request.getStudentId())
                .eq(StudentClass::getClassId, request.getClassId()));
        if (existing != null) {
            throw new IllegalArgumentException("该学生已在此班级中");
        }
        StudentClass studentClass = new StudentClass();
        studentClass.setStudentId(request.getStudentId());
        studentClass.setClassId(request.getClassId());
        studentClassMapper.insert(studentClass);
        return toVO(studentClassMapper.selectById(studentClass.getId()), userMapper.selectById(request.getStudentId()), classInfoMapper.selectById(request.getClassId()));
    }

    public void remove(Long id) {
        studentClassMapper.deleteById(id);
    }

    private void validateStudent(Long studentId) {
        SysUser user = userMapper.selectById(studentId);
        if (user == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null || !"STUDENT".equals(role.getRoleCode())) {
            throw new IllegalArgumentException("只能添加学生角色到班级");
        }
    }

    private List<StudentClassVO> toVOList(List<StudentClass> list) {
        Map<Long, SysUser> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, ClassInfo> classMap = classInfoMapper.selectList(null).stream()
                .collect(Collectors.toMap(ClassInfo::getId, Function.identity()));
        return list.stream()
                .map(item -> toVO(item, userMap.get(item.getStudentId()), classMap.get(item.getClassId())))
                .toList();
    }

    private StudentClassVO toVO(StudentClass item, SysUser user, ClassInfo classInfo) {
        StudentClassVO vo = new StudentClassVO();
        vo.setId(item.getId());
        vo.setStudentId(item.getStudentId());
        vo.setStudentName(user == null ? "" : user.getRealName());
        vo.setStudentNo(user == null ? "" : user.getStudentNo());
        vo.setClassId(item.getClassId());
        vo.setClassName(classInfo == null ? "" : classInfo.getClassName());
        vo.setCreatedTime(item.getCreatedTime());
        return vo;
    }
}

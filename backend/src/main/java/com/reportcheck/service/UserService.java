package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.dto.PasswordResetRequest;
import com.reportcheck.dto.UserRequest;
import com.reportcheck.entity.Course;
import com.reportcheck.entity.ExperimentTask;
import com.reportcheck.entity.ReportFile;
import com.reportcheck.entity.StudentClass;
import com.reportcheck.entity.SysRole;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.CourseMapper;
import com.reportcheck.mapper.ExperimentTaskMapper;
import com.reportcheck.mapper.ReportFileMapper;
import com.reportcheck.mapper.StudentClassMapper;
import com.reportcheck.mapper.SysRoleMapper;
import com.reportcheck.mapper.SysUserMapper;
import com.reportcheck.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final CourseMapper courseMapper;
    private final ExperimentTaskMapper experimentTaskMapper;
    private final StudentClassMapper studentClassMapper;
    private final ReportFileMapper reportFileMapper;

    public UserService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            CourseMapper courseMapper,
            ExperimentTaskMapper experimentTaskMapper,
            StudentClassMapper studentClassMapper,
            ReportFileMapper reportFileMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.courseMapper = courseMapper;
        this.experimentTaskMapper = experimentTaskMapper;
        this.studentClassMapper = studentClassMapper;
        this.reportFileMapper = reportFileMapper;
    }

    public List<UserVO> listUsers() {
        Map<Long, SysRole> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        return userMapper.selectList(null).stream()
                .map(user -> toUserVO(user, roleMap.get(user.getRoleId())))
                .toList();
    }

    public List<SysRole> listRoles() {
        return roleMapper.selectList(null);
    }

    public UserVO createUser(UserRequest request) {
        ensureUsernameUnique(request.getUsername(), null);
        ensureEmailUnique(request.getEmail(), null);
        ensureStudentNoUnique(request.getStudentNo(), null);
        ensureTeacherNoUnique(request.getTeacherNo(), null);
        validateRole(request.getRoleId());
        SysUser user = toEntity(request);
        if (request.getPassword() == null || request.getPassword().isBlank() || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("新增用户必须设置至少6位初始密码");
        }
        user.setPassword(request.getPassword());
        userMapper.insert(user);
        return toUserVO(userMapper.selectById(user.getId()), roleMapper.selectById(user.getRoleId()));
    }

    public UserVO updateUser(Long id, UserRequest request) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        ensureUsernameUnique(request.getUsername(), id);
        ensureEmailUnique(request.getEmail(), id);
        ensureStudentNoUnique(request.getStudentNo(), id);
        ensureTeacherNoUnique(request.getTeacherNo(), id);
        validateRole(request.getRoleId());
        SysUser user = toEntity(request);
        user.setId(id);
        user.setPassword(existing.getPassword());
        userMapper.updateById(user);
        return toUserVO(userMapper.selectById(id), roleMapper.selectById(request.getRoleId()));
    }

    public void resetPassword(Long id, PasswordResetRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(request.getPassword());
        userMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return;
        }
        if ("admin".equals(user.getUsername())) {
            throw new IllegalArgumentException("默认管理员不能删除");
        }
        Long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, id));
        Long taskCount = experimentTaskMapper.selectCount(new LambdaQueryWrapper<ExperimentTask>()
                .eq(ExperimentTask::getCreatedBy, id));
        Long classCount = studentClassMapper.selectCount(new LambdaQueryWrapper<StudentClass>()
                .eq(StudentClass::getStudentId, id));
        Long reportCount = reportFileMapper.selectCount(new LambdaQueryWrapper<ReportFile>()
                .eq(ReportFile::getStudentId, id));
        if (courseCount + taskCount + classCount + reportCount > 0) {
            throw new IllegalArgumentException("该用户已关联课程、班级、实验任务或报告数据，不能删除，建议改为禁用");
        }
        userMapper.deleteById(id);
    }

    private SysUser toEntity(UserRequest request) {
        SysUser user = new SysUser();
        user.setUsername(normalize(request.getUsername()));
        user.setRealName(normalize(request.getRealName()));
        user.setRoleId(request.getRoleId());
        user.setStudentNo(normalize(request.getStudentNo()));
        user.setTeacherNo(normalize(request.getTeacherNo()));
        user.setPhone(normalize(request.getPhone()));
        user.setEmail(normalize(request.getEmail()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        return user;
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void ensureEmailUnique(String email, Long excludeId) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return;
        }
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, normalizedEmail));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("邮箱已被其他账号使用");
        }
    }

    private void ensureStudentNoUnique(String studentNo, Long excludeId) {
        String normalizedStudentNo = normalize(studentNo);
        if (normalizedStudentNo == null) {
            return;
        }
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStudentNo, normalizedStudentNo));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("学号已被其他账号使用");
        }
    }

    private void ensureTeacherNoUnique(String teacherNo, Long excludeId) {
        String normalizedTeacherNo = normalize(teacherNo);
        if (normalizedTeacherNo == null) {
            return;
        }
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTeacherNo, normalizedTeacherNo));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("工号已被其他账号使用");
        }
    }

    private void validateRole(Long roleId) {
        if (roleMapper.selectById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
    }

    private UserVO toUserVO(SysUser user, SysRole role) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleCode(role == null ? "" : role.getRoleCode());
        vo.setRoleName(role == null ? "" : role.getRoleName());
        vo.setStudentNo(user.getStudentNo());
        vo.setTeacherNo(user.getTeacherNo());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        return vo;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

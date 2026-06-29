package com.reportcheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reportcheck.dto.ForgotPasswordRequest;
import com.reportcheck.dto.LoginRequest;
import com.reportcheck.dto.RegisterRequest;
import com.reportcheck.entity.SysRole;
import com.reportcheck.entity.SysUser;
import com.reportcheck.mapper.SysRoleMapper;
import com.reportcheck.mapper.SysUserMapper;
import com.reportcheck.util.JwtUtil;
import com.reportcheck.vo.LoginResponse;
import com.reportcheck.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("用户已被禁用");
        }

        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "" : role.getRoleCode();
        String roleName = role == null ? "" : role.getRoleName();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roleCode,
                roleName
        );
    }

    public UserVO register(RegisterRequest request) {
        String roleCode = normalize(request.getRoleCode());
        if (!"TEACHER".equals(roleCode) && !"STUDENT".equals(roleCode)) {
            throw new IllegalArgumentException("只允许注册教师或学生账号");
        }

        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            throw new IllegalArgumentException("注册角色不存在，请先初始化系统角色数据");
        }

        ensureUsernameUnique(request.getUsername(), null);
        ensureEmailUnique(request.getEmail(), null);

        String studentNo = normalize(request.getStudentNo());
        String teacherNo = normalize(request.getTeacherNo());
        if ("STUDENT".equals(roleCode)) {
            if (studentNo == null) {
                throw new IllegalArgumentException("学生注册必须填写学号");
            }
            ensureStudentNoUnique(studentNo, null);
            teacherNo = null;
        } else {
            if (teacherNo == null) {
                throw new IllegalArgumentException("教师注册必须填写工号");
            }
            ensureTeacherNoUnique(teacherNo, null);
            studentNo = null;
        }

        SysUser user = new SysUser();
        user.setUsername(normalize(request.getUsername()));
        user.setPassword(request.getPassword());
        user.setRealName(normalize(request.getRealName()));
        user.setRoleId(role.getId());
        user.setStudentNo(studentNo);
        user.setTeacherNo(teacherNo);
        user.setPhone(normalize(request.getPhone()));
        user.setEmail(normalize(request.getEmail()));
        user.setStatus(1);
        userMapper.insert(user);

        return toUserVO(userMapper.selectById(user.getId()), role);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getEmail, request.getEmail()));
        if (user == null) {
            throw new IllegalArgumentException("用户名和绑定邮箱不匹配");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("用户已被禁用，无法在线找回密码");
        }
        user.setPassword(request.getPassword());
        userMapper.updateById(user);
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
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStudentNo, studentNo));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("学号已被注册");
        }
    }

    private void ensureTeacherNoUnique(String teacherNo, Long excludeId) {
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTeacherNo, teacherNo));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("工号已被注册");
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

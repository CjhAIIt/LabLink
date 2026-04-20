package com.lab.recruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.recruitment.dto.PasswordResetDTO;
import com.lab.recruitment.dto.PasswordResetSendCodeDTO;
import com.lab.recruitment.dto.RegisterEmailCodeDTO;
import com.lab.recruitment.dto.UserRegisterDTO;
import com.lab.recruitment.entity.Delivery;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.mapper.DeliveryMapper;
import com.lab.recruitment.mapper.UserMapper;
import com.lab.recruitment.service.EmailAuthCodeService;
import com.lab.recruitment.service.PlatformCacheService;
import com.lab.recruitment.service.TeacherRegisterApplyService;
import com.lab.recruitment.service.UserAccessService;
import com.lab.recruitment.service.UserService;
import com.lab.recruitment.support.UserAccessProfile;
import com.lab.recruitment.utils.JwtUtils;
import com.lab.recruitment.vo.LoginVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String STUDENT_ROLE = "student";
    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("\\d{3,20}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9][0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern CHINESE_REAL_NAME_PATTERN = Pattern.compile("^[\\p{IsHan}·]{2,50}$");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private EmailAuthCodeService emailAuthCodeService;

    @Autowired
    private UserAccessService userAccessService;

    @Autowired
    private PlatformCacheService platformCacheService;

    @Autowired
    @Lazy
    private TeacherRegisterApplyService teacherRegisterApplyService;

    @Override
    public void sendRegisterEmailCode(RegisterEmailCodeDTO registerEmailCodeDTO) {
        String studentId = trimToNull(registerEmailCodeDTO.getStudentId());
        String email = normalizeEmail(registerEmailCodeDTO.getEmail());

        validateStudentId(studentId);
        validateEmail(email);
        if (baseMapper.countByUsernameIncludeDeleted(studentId, NOT_DELETED) > 0
                || baseMapper.countByStudentIdIncludeDeleted(studentId, NOT_DELETED) > 0) {
            throw new RuntimeException("该学号已注册");
        }
        if (baseMapper.countByEmailIncludeDeleted(email, NOT_DELETED) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        emailAuthCodeService.sendRegisterCode(studentId, email);
    }

    @Override
    @Transactional
    public boolean register(UserRegisterDTO registerDTO) {
        prepareRegisterDTO(registerDTO);
        validateStudentId(registerDTO.getStudentId());
        validateRegisterInput(registerDTO);
        ensureActiveRegistrationIsAllowed(registerDTO);

        User deletedUser = findDeletedStudent(registerDTO.getStudentId());
        if (deletedUser != null) {
            return restoreDeletedStudent(deletedUser, registerDTO);
        }

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setUsername(registerDTO.getStudentId());
        user.setStudentId(registerDTO.getStudentId());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(STUDENT_ROLE);
        user.setStatus(1);
        return this.save(user);
    }

    @Override
    public boolean save(User user) {
        normalizeUserFields(user);
        validateUserForCreate(user);
        ensureUserUniqueBeforeCreate(user);
        boolean success = super.save(user);
        if (success && user != null && user.getId() != null) {
            platformCacheService.evictUserAuthCache(user.getId());
        }
        return success;
    }

    @Override
    public boolean updateById(User user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException("用户不存在");
        }

        User existingUser = this.getById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        normalizeUserFields(user);
        validateUserForUpdate(user, existingUser);
        ensureUserUniqueBeforeUpdate(user, existingUser);
        boolean success = super.updateById(user);
        if (success) {
            platformCacheService.evictUserAuthCache(user.getId());
        }
        return success;
    }

    @Override
    public LoginVO login(String username, String password) {
        // 万能测试账号：账号密码均为 1234567，角色 teacher，绑定实验室 id=1
        if ("1234567".equals(username) && "1234567".equals(password)) {
            User mockUser = new User();
            mockUser.setId(-1L);
            mockUser.setUsername("1234567");
            mockUser.setRealName("测试教师");
            mockUser.setRole("teacher");
            mockUser.setLabId(1L);
            mockUser.setStatus(1);
            String token = jwtUtils.generateToken(username, "teacher");
            LoginVO loginVO = new LoginVO();
            loginVO.setId(-1L);
            loginVO.setToken(token);
            loginVO.setUsername("1234567");
            loginVO.setRealName("测试教师");
            loginVO.setRole("teacher");
            loginVO.setLabId(1L);
            return loginVO;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("deleted", 0);
        User user = this.getOne(queryWrapper);

        if (user == null) {
            String teacherRegisterMessage = teacherRegisterApplyService.resolveLoginBlockMessage(username);
            if (StringUtils.hasText(teacherRegisterMessage)) {
                throw new RuntimeException(teacherRegisterMessage);
            }
            throw new RuntimeException("User not found or disabled");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        UserAccessProfile profile = userAccessService.buildProfile(user);
        String token = jwtUtils.generateToken(username, profile.getDisplayRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setToken(token);
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setRole(profile.getDisplayRole());
        Long resolvedLabId = userAccessService.resolveManagedLabId(user);
        loginVO.setLabId(resolvedLabId != null ? resolvedLabId : user.getLabId());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setPrimaryIdentity(profile.getPrimaryIdentity());
        loginVO.setLabMemberRole(profile.getLabMemberRole());
        loginVO.setManagedCollegeId(profile.getManagedCollegeId());
        loginVO.setSchoolDirector(profile.isSchoolDirector());
        loginVO.setCollegeManager(profile.isCollegeManager());
        loginVO.setLabManager(profile.isLabManager());
        loginVO.setPlatformPostCodes(profile.getPlatformPostCodes());

        return loginVO;
    }

    @Override
    public void sendPasswordResetCode(PasswordResetSendCodeDTO passwordResetSendCodeDTO) {
        String username = trimToNull(passwordResetSendCodeDTO.getUsername());
        String email = normalizeEmail(passwordResetSendCodeDTO.getEmail());

        if (!StringUtils.hasText(username)) {
            throw new RuntimeException("账号不能为空");
        }
        validateEmail(email);

        User user = findActiveUserByAccount(username);
        if (user == null) {
            throw new RuntimeException("账号不存在或已被禁用");
        }
        if (!email.equals(normalizeEmail(user.getEmail()))) {
            throw new RuntimeException("账号与邮箱不匹配");
        }

        emailAuthCodeService.sendPasswordResetCode(user.getUsername(), email);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDTO passwordResetDTO) {
        String username = trimToNull(passwordResetDTO.getUsername());
        String email = normalizeEmail(passwordResetDTO.getEmail());
        String code = trimToNull(passwordResetDTO.getCode());
        String newPassword = trimToNull(passwordResetDTO.getNewPassword());

        if (!StringUtils.hasText(username)) {
            throw new RuntimeException("账号不能为空");
        }
        validateEmail(email);
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度需在 6 到 20 位之间");
        }

        User user = findActiveUserByAccount(username);
        if (user == null) {
            throw new RuntimeException("账号不存在或已被禁用");
        }
        if (!email.equals(normalizeEmail(user.getEmail()))) {
            throw new RuntimeException("账号与邮箱不匹配");
        }

        emailAuthCodeService.verifyPasswordResetCode(user.getUsername(), email, code);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("deleted", NOT_DELETED);
        return this.getOne(queryWrapper);
    }

    @Override
    public List<User> getUsersByRole(String role) {
        return baseMapper.selectUsersByRole(role);
    }

    @Override
    public Page<User> getUserPage(Integer pageNum, Integer pageSize, String realName, String studentId, String major, String role) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(realName)) {
            queryWrapper.like("real_name", realName);
        }

        if (StringUtils.hasText(studentId)) {
            queryWrapper.like("student_id", studentId);
        }

        if (StringUtils.hasText(major)) {
            queryWrapper.like("major", major);
        }

        if (StringUtils.hasText(role)) {
            if ("ADMIN".equals(role)) {
                queryWrapper.in("role", "admin", "super_admin");
            } else if ("student".equalsIgnoreCase(role)) {
                queryWrapper.eq("role", STUDENT_ROLE);
            } else {
                queryWrapper.eq("role", role.toLowerCase());
            }
        }

        queryWrapper.orderByDesc("create_time");
        return this.page(page, queryWrapper);
    }

    @Override
    public Page<User> getStudentPageForAdmin(Integer pageNum, Integer pageSize, String keyword, String realName,
                                             String studentId, String major, Long collegeId, Long labId) {
        long current = Math.max(pageNum, 1);
        long size = Math.max(pageSize, 1);
        long offset = (current - 1) * size;

        Page<User> page = new Page<>(current, size);
        page.setRecords(baseMapper.selectStudentPageForAdmin(offset, (int) size, keyword, realName, studentId, major, collegeId, labId));
        page.setTotal(baseMapper.countStudentPageForAdmin(keyword, realName, studentId, major, collegeId, labId));
        return page;
    }

    @Override
    public List<User> selectAllAdmins() {
        return baseMapper.selectAllAdmins();
    }

    @Override
    public User selectAdminById(Long id) {
        User admin = this.getById(id);
        if (admin != null && !"admin".equals(admin.getRole())) {
            throw new RuntimeException("Target user is not an admin");
        }
        return admin;
    }

    @Override
    public boolean addAdmin(User user) {
        user.setUsername(trimToNull(user.getUsername()));
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (this.count(queryWrapper) > 0) {
            throw new RuntimeException("Username already exists");
        }

        user.setRole("admin");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCanEdit(1);
        return this.save(user);
    }

    @Override
    public boolean updateAdmin(User user) {
        User existingAdmin = this.getById(user.getId());
        if (existingAdmin == null) {
            throw new RuntimeException("Admin not found");
        }

        if (!"admin".equals(existingAdmin.getRole())) {
            throw new RuntimeException("Target user is not an admin");
        }

        if (StringUtils.hasText(user.getPassword())) {
            existingAdmin.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRealName() != null) {
            existingAdmin.setRealName(user.getRealName());
        }
        if (user.getEmail() != null) {
            existingAdmin.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existingAdmin.setPhone(user.getPhone());
        }
        if (user.getLabId() != null) {
            existingAdmin.setLabId(user.getLabId());
        }

        return this.updateById(existingAdmin);
    }

    @Override
    @Transactional
    public boolean deleteAdmin(Long id) {
        User admin = this.getById(id);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        if (!"admin".equals(admin.getRole())) {
            throw new RuntimeException("Only admin accounts can be deleted");
        }

        if (admin.getLabId() != null) {
            QueryWrapper<Delivery> deliveryQuery = new QueryWrapper<>();
            deliveryQuery.eq("lab_id", admin.getLabId());
            deliveryQuery.eq("deleted", 0);
            Long deliveryCount = deliveryMapper.selectCount(deliveryQuery);

            if (deliveryCount > 0) {
                throw new RuntimeException("This admin still has delivery records in the lab");
            }
        }

        return this.removeById(id);
    }

    @Override
    public boolean resetAdminPassword(Long adminId, String newPassword) {
        User admin = this.getById(adminId);
        if (admin == null) {
            throw new RuntimeException("Admin not found");
        }

        if (!"admin".equals(admin.getRole())) {
            throw new RuntimeException("Only admin accounts can reset password");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(admin);
    }

    @Override
    public boolean existsByUsernameAndDeleted(String username, boolean deleted) {
        return baseMapper.countByUsernameIncludeDeleted(username, deleted ? DELETED : NOT_DELETED) > 0;
    }

    @Override
    public boolean existsByEmailAndDeleted(String email, boolean deleted) {
        return baseMapper.countByEmailIncludeDeleted(email, deleted ? DELETED : NOT_DELETED) > 0;
    }

    @Override
    public boolean existsByPhoneAndDeleted(String phone, boolean deleted) {
        return baseMapper.countByPhoneIncludeDeleted(phone, deleted ? DELETED : NOT_DELETED) > 0;
    }

    @Override
    @Transactional
    public boolean registerWithDeletedCheck(UserRegisterDTO registerDTO) {
        return register(registerDTO);
    }

    private void prepareRegisterDTO(UserRegisterDTO registerDTO) {
        registerDTO.setStudentId(trimToNull(registerDTO.getStudentId()));
        registerDTO.setUsername(registerDTO.getStudentId());
        registerDTO.setRealName(trimToNull(registerDTO.getRealName()));
        registerDTO.setCollege(trimToNull(registerDTO.getCollege()));
        registerDTO.setMajor(trimToNull(registerDTO.getMajor()));
        registerDTO.setGrade(trimToNull(registerDTO.getGrade()));
        registerDTO.setPhone(trimToNull(registerDTO.getPhone()));
        registerDTO.setEmail(normalizeEmail(registerDTO.getEmail()));
        registerDTO.setEmailCode(trimToNull(registerDTO.getEmailCode()));
        registerDTO.setRole(STUDENT_ROLE);
    }

    private void validateRegisterInput(UserRegisterDTO registerDTO) {
        validateRealName(registerDTO.getRealName());
        validateEmail(registerDTO.getEmail());
        validatePhone(registerDTO.getPhone());
    }

    private void normalizeUserFields(User user) {
        if (user == null) {
            return;
        }
        user.setUsername(trimToNull(user.getUsername()));
        user.setRealName(trimToNull(user.getRealName()));
        user.setStudentId(trimToNull(user.getStudentId()));
        user.setCollege(trimToNull(user.getCollege()));
        user.setMajor(trimToNull(user.getMajor()));
        user.setGrade(trimToNull(user.getGrade()));
        user.setPhone(trimToNull(user.getPhone()));
        user.setEmail(normalizeEmail(user.getEmail()));
    }

    private void validateUserForCreate(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        validateRealName(user.getRealName());
        if (StringUtils.hasText(user.getStudentId())) {
            validateStudentId(user.getStudentId());
        }
        validateEmail(user.getEmail());
        validatePhone(user.getPhone());
    }

    private void validateUserForUpdate(User user, User existingUser) {
        if (hasDifferentValue(user.getUsername(), existingUser.getUsername()) && !StringUtils.hasText(user.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (hasDifferentValue(user.getRealName(), existingUser.getRealName())) {
            validateRealName(user.getRealName());
        }
        if (hasDifferentValue(user.getStudentId(), existingUser.getStudentId())) {
            validateStudentId(user.getStudentId());
        }
        if (hasDifferentValue(user.getEmail(), existingUser.getEmail())) {
            validateEmail(user.getEmail());
        }
        if (hasDifferentValue(user.getPhone(), existingUser.getPhone())) {
            validatePhone(user.getPhone());
        }
    }

    private void validateStudentId(String studentId) {
        if (!StringUtils.hasText(studentId) || !STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            throw new RuntimeException("学号必须为 3 到 20 位纯数字");
        }
    }

    private void validateRealName(String realName) {
        if (!StringUtils.hasText(realName)) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (!CHINESE_REAL_NAME_PATTERN.matcher(realName).matches()) {
            throw new RuntimeException("真实姓名必须为中文");
        }
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("邮箱格式不正确");
        }
    }

    private void validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new RuntimeException("手机号格式不正确");
        }
    }

    private void ensureActiveRegistrationIsAllowed(UserRegisterDTO registerDTO) {
        if (baseMapper.countByUsernameIncludeDeleted(registerDTO.getStudentId(), NOT_DELETED) > 0
                || baseMapper.countByStudentIdIncludeDeleted(registerDTO.getStudentId(), NOT_DELETED) > 0) {
            throw new RuntimeException("该学号已注册");
        }

        if (baseMapper.countByEmailIncludeDeleted(registerDTO.getEmail(), NOT_DELETED) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        if (StringUtils.hasText(registerDTO.getPhone())
                && baseMapper.countByPhoneIncludeDeleted(registerDTO.getPhone(), NOT_DELETED) > 0) {
            throw new RuntimeException("该手机号已被注册");
        }
    }

    private void ensureUserUniqueBeforeCreate(User user) {
        if (StringUtils.hasText(user.getUsername()) && hasActiveUserWithField("username", user.getUsername(), null)) {
            throw new RuntimeException("用户名已存在");
        }
        if (StringUtils.hasText(user.getStudentId()) && hasActiveUserWithField("student_id", user.getStudentId(), null)) {
            throw new RuntimeException("该学号已注册");
        }
        if (StringUtils.hasText(user.getEmail()) && hasActiveUserWithField("email", user.getEmail(), null)) {
            throw new RuntimeException("该邮箱已被注册");
        }
    }

    private void ensureUserUniqueBeforeUpdate(User user, User existingUser) {
        if (hasDifferentValue(user.getUsername(), existingUser.getUsername())
                && hasActiveUserWithField("username", user.getUsername(), existingUser.getId())) {
            throw new RuntimeException("用户名已存在");
        }
        if (hasDifferentValue(user.getStudentId(), existingUser.getStudentId())
                && hasActiveUserWithField("student_id", user.getStudentId(), existingUser.getId())) {
            throw new RuntimeException("该学号已注册");
        }
        if (hasDifferentValue(user.getEmail(), existingUser.getEmail())
                && hasActiveUserWithField("email", user.getEmail(), existingUser.getId())) {
            throw new RuntimeException("该邮箱已被注册");
        }
    }

    private boolean hasActiveUserWithField(String column, String value, Long excludeId) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(column, value);
        queryWrapper.eq("deleted", NOT_DELETED);
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    private boolean hasDifferentValue(String currentValue, String existingValue) {
        if (currentValue == null) {
            return existingValue != null;
        }
        return !currentValue.equals(existingValue);
    }

    private User findDeletedStudent(String studentId) {
        return baseMapper.findByUsernameAndDeleted(studentId, DELETED);
    }

    private User findActiveUserByAccount(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.eq("deleted", NOT_DELETED);
        queryWrapper.and(wrapper -> wrapper.eq("username", account).or().eq("student_id", account));
        queryWrapper.last("LIMIT 1");
        return this.getOne(queryWrapper, false);
    }

    private boolean restoreDeletedStudent(User deletedUser, UserRegisterDTO registerDTO) {
        if (deletedUser == null) {
            return false;
        }

        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
        int result = baseMapper.restoreDeletedUser(
                deletedUser.getId(),
                registerDTO.getStudentId(),
                encodedPassword,
                registerDTO.getRealName(),
                STUDENT_ROLE,
                registerDTO.getStudentId(),
                registerDTO.getCollege(),
                registerDTO.getMajor(),
                registerDTO.getGrade(),
                registerDTO.getPhone(),
                registerDTO.getEmail(),
                1
        );
        return result > 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }
}

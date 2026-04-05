package com.lab.recruitment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.recruitment.dto.PasswordResetDTO;
import com.lab.recruitment.dto.PasswordResetSendCodeDTO;
import com.lab.recruitment.dto.RegisterEmailCodeDTO;
import com.lab.recruitment.dto.UserRegisterDTO;
import com.lab.recruitment.entity.User;
import com.lab.recruitment.vo.LoginVO;

import java.util.List;

public interface UserService extends IService<User> {

    void sendRegisterEmailCode(RegisterEmailCodeDTO registerEmailCodeDTO);
    
    boolean register(UserRegisterDTO registerDTO);
    
    LoginVO login(String username, String password);

    void sendPasswordResetCode(PasswordResetSendCodeDTO passwordResetSendCodeDTO);

    void resetPassword(PasswordResetDTO passwordResetDTO);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 根据角色查询用户列表
     */
    List<User> getUsersByRole(String role);
    
    /**
     * 分页查询用户
     */
    Page<User> getUserPage(Integer pageNum, Integer pageSize, String realName, String studentId, String major, String role);

    Page<User> getStudentPageForAdmin(Integer pageNum, Integer pageSize, String keyword, String realName, String studentId, String major, Long labId);
    
    /**
     * 查询所有管理员（不包括总负责人）
     */
    List<User> selectAllAdmins();
    
    /**
     * 根据ID查询管理员
     */
    User selectAdminById(Long id);
    
    /**
     * 添加管理员（仅总负责人可用）
     */
    boolean addAdmin(User user);
    
    /**
     * 更新管理员信息（仅总负责人可用）
     */
    boolean updateAdmin(User user);
    
    /**
     * 删除管理员（仅总负责人可用）
     */
    boolean deleteAdmin(Long id);
    
    /**
     * 重置管理员密码（仅总负责人可用）
     */
    boolean resetAdminPassword(Long adminId, String newPassword);
    
    /**
     * 检查用户名是否存在（考虑删除状态）
     */
    boolean existsByUsernameAndDeleted(String username, boolean deleted);
    
    /**
     * 检查邮箱是否存在（考虑删除状态）
     */
    boolean existsByEmailAndDeleted(String email, boolean deleted);
    
    /**
     * 检查手机号是否存在（考虑删除状态）
     */
    boolean existsByPhoneAndDeleted(String phone, boolean deleted);
    
    /**
     * 注册用户（处理已删除记录的情况）
     */
    boolean registerWithDeletedCheck(UserRegisterDTO registerDTO);
}

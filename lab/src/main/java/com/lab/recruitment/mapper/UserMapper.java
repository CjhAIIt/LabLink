package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.recruitment.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> selectUsersByRole(@Param("role") String role);

    List<User> selectAllAdmins();

    int countByUsernameIncludeDeleted(@Param("username") String username, @Param("deleted") Integer deleted);

    int countByStudentIdIncludeDeleted(@Param("studentId") String studentId, @Param("deleted") Integer deleted);

    int countByEmailIncludeDeleted(@Param("email") String email, @Param("deleted") Integer deleted);

    int countByPhoneIncludeDeleted(@Param("phone") String phone, @Param("deleted") Integer deleted);

    List<User> selectStudentPageForAdmin(@Param("offset") Long offset,
                                         @Param("pageSize") Integer pageSize,
                                         @Param("keyword") String keyword,
                                         @Param("realName") String realName,
                                         @Param("studentId") String studentId,
                                         @Param("major") String major,
                                         @Param("labId") Long labId);

    Long countStudentPageForAdmin(@Param("keyword") String keyword,
                                  @Param("realName") String realName,
                                  @Param("studentId") String studentId,
                                  @Param("major") String major,
                                  @Param("labId") Long labId);

    User findByUsernameAndDeleted(@Param("username") String username, @Param("deleted") Integer deleted);

    int restoreDeletedUser(@Param("id") Long id,
                           @Param("username") String username,
                           @Param("password") String password,
                           @Param("realName") String realName,
                           @Param("role") String role,
                           @Param("studentId") String studentId,
                           @Param("college") String college,
                           @Param("major") String major,
                           @Param("grade") String grade,
                           @Param("phone") String phone,
                           @Param("email") String email,
                           @Param("status") Integer status);
}

package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Delivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeliveryMapper extends BaseMapper<Delivery> {

    Page<Map<String, Object>> selectDeliveryPage(Page<Map<String, Object>> page, @Param("labId") Long labId,
                                                 @Param("realName") String realName, @Param("studentId") String studentId,
                                                 @Param("auditStatus") Integer auditStatus);

    Page<Map<String, Object>> selectMyDeliveryPage(Page<Map<String, Object>> page, @Param("username") String username,
                                                   @Param("labName") String labName, @Param("auditStatus") Integer auditStatus);

    List<Map<String, Object>> selectPendingOfferList(@Param("username") String username);

    Integer countByLabIdAndAuditStatus(@Param("labId") Long labId, @Param("auditStatus") Integer auditStatus);

    Integer countAdmittedByLabId(@Param("labId") Long labId);
}

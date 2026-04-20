package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.recruitment.entity.AiInterviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiInterviewRecordMapper extends BaseMapper<AiInterviewRecord> {

    @Select("SELECT COUNT(*) FROM t_ai_interview_record WHERE student_id = #{studentId} AND status IN ('进行中','已完成') AND deleted = 0")
    int countUsedChances(@Param("studentId") Long studentId);
}

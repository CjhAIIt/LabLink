package com.lab.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.recruitment.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    @Select({
            "<script>",
            "SELECT",
            "  n.id, n.title, n.content, n.publish_scope AS publishScope, n.college_id AS collegeId,",
            "  n.lab_id AS labId, n.publisher_id AS publisherId, n.status, n.publish_time AS publishTime,",
            "  n.create_time AS createTime, c.college_name AS collegeName, l.lab_name AS labName,",
            "  u.real_name AS publisherName",
            "FROM t_notice n",
            "LEFT JOIN t_college c ON c.id = n.college_id AND c.deleted = 0",
            "LEFT JOIN t_lab l ON l.id = n.lab_id AND l.deleted = 0",
            "LEFT JOIN t_user u ON u.id = n.publisher_id AND u.deleted = 0",
            "WHERE n.deleted = 0",
            "<if test='publishScope != null and publishScope != \"\"'>",
            "  AND n.publish_scope = #{publishScope}",
            "</if>",
            "<if test='collegeId != null'>",
            "  AND n.college_id = #{collegeId}",
            "</if>",
            "<if test='labId != null'>",
            "  AND n.lab_id = #{labId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (MATCH(n.title, n.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR c.college_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY",
            "<if test='keyword != null and keyword != \"\"'>",
            "  MATCH(n.title, n.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC,",
            "</if>",
            "  n.publish_time DESC, n.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectNoticePageByFullText(Page<Map<String, Object>> page,
                                                         @Param("publishScope") String publishScope,
                                                         @Param("collegeId") Long collegeId,
                                                         @Param("labId") Long labId,
                                                         @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  n.id, n.title, n.content, n.publish_scope AS publishScope, n.college_id AS collegeId,",
            "  n.lab_id AS labId, n.publisher_id AS publisherId, n.status, n.publish_time AS publishTime,",
            "  n.create_time AS createTime, c.college_name AS collegeName, l.lab_name AS labName,",
            "  u.real_name AS publisherName",
            "FROM t_notice n",
            "LEFT JOIN t_college c ON c.id = n.college_id AND c.deleted = 0",
            "LEFT JOIN t_lab l ON l.id = n.lab_id AND l.deleted = 0",
            "LEFT JOIN t_user u ON u.id = n.publisher_id AND u.deleted = 0",
            "WHERE n.deleted = 0",
            "<if test='publishScope != null and publishScope != \"\"'>",
            "  AND n.publish_scope = #{publishScope}",
            "</if>",
            "<if test='collegeId != null'>",
            "  AND n.college_id = #{collegeId}",
            "</if>",
            "<if test='labId != null'>",
            "  AND n.lab_id = #{labId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (n.title LIKE CONCAT('%', #{keyword}, '%')",
            "       OR l.lab_name LIKE CONCAT('%', #{keyword}, '%')",
            "       OR c.college_name LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY n.publish_time DESC, n.id DESC",
            "</script>"
    })
    Page<Map<String, Object>> selectNoticePageByLike(Page<Map<String, Object>> page,
                                                     @Param("publishScope") String publishScope,
                                                     @Param("collegeId") Long collegeId,
                                                     @Param("labId") Long labId,
                                                     @Param("keyword") String keyword);

    @Select({
            "<script>",
            "SELECT",
            "  n.id, n.title, n.content, n.publish_scope AS publishScope, n.college_id AS collegeId,",
            "  n.lab_id AS labId, n.publisher_id AS publisherId, n.status, n.publish_time AS publishTime,",
            "  c.college_name AS collegeName, l.lab_name AS labName, u.real_name AS publisherName",
            "FROM t_notice n",
            "LEFT JOIN t_college c ON c.id = n.college_id AND c.deleted = 0",
            "LEFT JOIN t_lab l ON l.id = n.lab_id AND l.deleted = 0",
            "LEFT JOIN t_user u ON u.id = n.publisher_id AND u.deleted = 0",
            "WHERE n.deleted = 0 AND n.status = 1",
            "AND (n.publish_scope = 'school'",
            "<if test='collegeId != null'>",
            "  OR (n.publish_scope = 'college' AND n.college_id = #{collegeId})",
            "</if>",
            "<if test='labId != null'>",
            "  OR (n.publish_scope = 'lab' AND n.lab_id = #{labId})",
            "</if>",
            ")",
            "ORDER BY n.publish_time DESC, n.id DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> selectLatestNotices(@Param("limit") int limit,
                                                  @Param("collegeId") Long collegeId,
                                                  @Param("labId") Long labId);
}

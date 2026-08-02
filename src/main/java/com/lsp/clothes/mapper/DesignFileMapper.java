package com.lsp.clothes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsp.clothes.model.entity.DesignFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DesignFileMapper extends BaseMapper<DesignFile> {
    @Select("<script>SELECT COALESCE(MAX(version_no), 0) FROM design_file "
            + "WHERE project_id = #{projectId} "
            + "<choose><when test='taskId != null'>AND task_id = #{taskId}</when>"
            + "<otherwise>AND task_id IS NULL</otherwise></choose></script>")
    Integer selectMaxVersion(@Param("projectId") Long projectId,
                             @Param("taskId") Long taskId);
}

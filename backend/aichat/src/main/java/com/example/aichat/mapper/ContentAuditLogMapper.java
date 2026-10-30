package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.ContentAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容审核日志 Mapper。分页/条件查询通过 BaseMapper + QueryWrapper 完成。
 */
@Mapper
public interface ContentAuditLogMapper extends BaseMapper<ContentAuditLog> {
}

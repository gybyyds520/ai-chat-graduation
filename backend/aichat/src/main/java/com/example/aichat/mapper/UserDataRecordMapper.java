package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.UserDataRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserDataRecordMapper extends BaseMapper<UserDataRecord> {

    @Select("SELECT * FROM user_data_record WHERE user_id = #{userId} ORDER BY capture_time DESC")
    List<UserDataRecord> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_data_record WHERE data_type = #{dataType} ORDER BY capture_time DESC")
    List<UserDataRecord> selectByDataType(@Param("dataType") String dataType);

    @Select("SELECT * FROM user_data_record WHERE user_id = #{userId} AND data_type = #{dataType} ORDER BY capture_time DESC")
    List<UserDataRecord> selectByUserIdAndType(@Param("userId") Long userId, @Param("dataType") String dataType);

    @Select("SELECT data_type, COUNT(*) as count FROM user_data_record GROUP BY data_type")
    List<Map<String, Object>> selectStatsByType();

    @Select("SELECT user_id, COUNT(*) as count FROM user_data_record GROUP BY user_id ORDER BY count DESC")
    List<Map<String, Object>> selectStatsByUser();

    @Select("SELECT DISTINCT data_type FROM user_data_record")
    List<String> selectAllDataTypes();

    @Select("SELECT * FROM user_data_record WHERE group_id = #{groupId}")
    List<UserDataRecord> selectByGroupId(@Param("groupId") String groupId);

    @Select("SELECT * FROM user_data_record ORDER BY capture_time DESC")
    IPage<UserDataRecord> selectPageWithSort(Page<UserDataRecord> page);

    @Select("SELECT * FROM user_data_record WHERE user_id = #{userId} ORDER BY capture_time DESC")
    IPage<UserDataRecord> selectPageByUserId(Page<UserDataRecord> page, @Param("userId") Long userId);

    @Select("SELECT * FROM user_data_record WHERE data_type = #{dataType} ORDER BY capture_time DESC")
    IPage<UserDataRecord> selectPageByDataType(Page<UserDataRecord> page, @Param("dataType") String dataType);

    @Select("SELECT * FROM user_data_record WHERE user_id = #{userId} AND data_type = #{dataType} ORDER BY capture_time DESC")
    IPage<UserDataRecord> selectPageByUserIdAndType(Page<UserDataRecord> page, @Param("userId") Long userId, @Param("dataType") String dataType);

    @Select("SELECT COUNT(*) FROM user_data_record")
    Integer selectTotalCount();

    @Select("SELECT COUNT(*) FROM user_data_record WHERE backup_status = 'BACKED_UP'")
    Integer selectBackedUpCount();

    @Select("SELECT COUNT(*) FROM user_data_record WHERE status = 'VALIDATED'")
    Integer selectValidatedCount();

    @Select("SELECT user_id, data_type, content, COUNT(*) as count " +
            "FROM user_data_record " +
            "GROUP BY user_id, data_type, content " +
            "HAVING COUNT(*) > 1")
    List<Map<String, Object>> selectDuplicateRecords();

    @Select("SELECT id FROM user_data_record WHERE user_id = #{userId} AND data_type = #{dataType} AND content = #{content} ORDER BY id ASC")
    List<Long> selectDuplicateIds(@Param("userId") Long userId, @Param("dataType") String dataType, @Param("content") String content);

    @Select("SELECT id FROM user_data_record WHERE user_id = #{userId} AND data_type = #{dataType} AND content = #{content} ORDER BY id ASC LIMIT 1, 1000")
    List<Long> selectDuplicateIdsToDelete(@Param("userId") Long userId, @Param("dataType") String dataType, @Param("content") String content);

    @Select("SELECT * FROM user_data_record WHERE user_id = #{userId} AND data_type = #{dataType} AND content = #{content}")
    UserDataRecord selectByUniqueKey(@Param("userId") Long userId, @Param("dataType") String dataType, @Param("content") String content);

    Integer deleteDuplicateRecords(List<Long> ids);
}
package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface UserQuotaMapper extends BaseMapper<UserQuota> {

    /**
     * P1-3 修复：原子条件扣减配额。
     * 仅当当日该功能已用次数严格小于 ceiling 时才 +1，由数据库行锁保证并发安全，
     * 消除原 check-then-act（先 select 读 used、再 update used+1）在并发下超额的竞态。
     *
     * @return 受影响行数：1=扣减成功；0=当日已达上限（或无当日记录），调用方应抛 QUOTA_EXCEEDED
     */
    @Update("UPDATE user_quota SET used_count = used_count + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND feature_type = #{featureType} AND reset_date = #{resetDate} " +
            "AND used_count < #{ceiling}")
    int tryConsumeQuota(@Param("userId") Long userId,
                        @Param("featureType") String featureType,
                        @Param("resetDate") LocalDate resetDate,
                        @Param("ceiling") int ceiling);
}

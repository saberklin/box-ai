package com.boxai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 实体基类
 * 为所有实体提供通用的时间戳字段（创建时间、更新时间）
 */
@Data
public abstract class BaseEntity {
    /**
     * 创建时间
     * 由MyBatis-Plus自动填充（插入时）
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     * 由MyBatis-Plus自动填充（插入和更新时）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}



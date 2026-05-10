package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统提示词配置
 */
@Getter
@Setter
@Entity
@Table(name = "sys_prompt")
public class Prompt extends BaseEntity {

    /** 提示词类型：INTENT_RECOGNITION / REPLY_ATTRACTION / REPLY_FOOD / REPLY_HOTEL / REPLY_PRODUCT / REPLY_TICKET / REPLY_CUSTOMER_SERVICE */
    @Column(name = "type", length = 50, nullable = false, unique = true)
    private String type;

    /** 提示词名称 */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 提示词内容 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 是否启用 */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /** 备注说明 */
    @Column(name = "remark", length = 500)
    private String remark;

    /** 版本号（用于乐观锁或缓存版本控制） */
    @Column(name = "version", nullable = false)
    private Integer version = 1;
}

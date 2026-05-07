package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_ticket")
public class Ticket extends BaseEntity {
    /** 门票图片 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 门票名称 */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 价格 */
    @Column(name = "price", length = 100)
    private String price;

    /** 所属地点 */
    @Column(name = "location", length = 200)
    private String location;

    /** 门票描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 关键词标签 */
    @Column(name = "tags", length = 500)
    private String tags;
}

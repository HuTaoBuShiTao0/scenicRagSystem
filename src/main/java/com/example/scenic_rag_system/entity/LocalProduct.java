package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_local_product")
public class LocalProduct extends BaseEntity {
    /** 产品图片 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 产品名称 */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 产品类别 */
    @Column(name = "category", length = 100)
    private String category;

    /** 价格 */
    @Column(name = "price", length = 100)
    private String price;

    /** 产品描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 产地 */
    @Column(name = "origin", length = 200)
    private String origin;
}

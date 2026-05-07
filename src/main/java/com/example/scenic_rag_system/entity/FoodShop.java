package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_food_shop")
public class FoodShop extends BaseEntity {
    /** 店铺主图 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 店铺名称 (RAG可检索) */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 店铺类型 (固定为:美食) (RAG可检索) */
    @Column(name = "type", length = 100)
    private String type = "美食";

    /** 归属景区 (RAG可检索) */
    @Column(name = "related_scenic", length = 200)
    private String relatedScenic;

    /** 经度 */
    @Column(name = "longitude", length = 50)
    private String longitude;

    /** 纬度 */
    @Column(name = "latitude", length = 50)
    private String latitude;

    /** 详细地址 */
    @Column(name = "address", length = 500)
    private String address;

    /** 店铺标签 (RAG可检索) */
    @Column(name = "tags", length = 500)
    private String tags;
}

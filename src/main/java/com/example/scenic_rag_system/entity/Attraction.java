package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_attraction")
public class Attraction extends BaseEntity {
    /** 景点图片 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 景点名称 (RAG可检索) */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 特色标签 (RAG可检索) */
    @Column(name = "tags", length = 500)
    private String tags;

    /** 关联景区 (RAG可检索) */
    @Column(name = "related_scenic", length = 200)
    private String relatedScenic;

    /** 景点一句话介绍 (RAG可检索) */
    @Column(name = "brief_intro", length = 500)
    private String briefIntro;

    /** 详细地址 */
    @Column(name = "address", length = 500)
    private String address;

    /** 经度 */
    @Column(name = "longitude", length = 50)
    private String longitude;

    /** 纬度 */
    @Column(name = "latitude", length = 50)
    private String latitude;

    /** 景点介绍 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

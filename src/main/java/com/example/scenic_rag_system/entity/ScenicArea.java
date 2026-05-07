package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_scenic_area")
public class ScenicArea extends BaseEntity {
    /** 景区主图 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 景区名称 */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 景区类型 */
    @Column(name = "type", length = 100)
    private String type;

    /** 景区级别 (5A/4A/3A/2A/1A) */
    @Column(name = "level", length = 20)
    private String level;

    /** 经度 */
    @Column(name = "longitude", length = 50)
    private String longitude;

    /** 纬度 */
    @Column(name = "latitude", length = 50)
    private String latitude;

    /** 景区地址 */
    @Column(name = "address", length = 500)
    private String address;

    /** 景区介绍 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

package com.example.scenic_rag_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kb_hotel")
public class Hotel extends BaseEntity {
    /** 酒店主图 */
    @Column(name = "image", columnDefinition = "MEDIUMTEXT")
    private String image;

    /** 酒店名称 */
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    /** 酒店等级 */
    @Column(name = "level", length = 50)
    private String level;

    /** 价格区间 */
    @Column(name = "price", length = 100)
    private String price;

    /** 详细地址 */
    @Column(name = "address", length = 500)
    private String address;

    /** 联系电话 */
    @Column(name = "phone", length = 50)
    private String phone;

    /** 设施服务 */
    @Column(name = "facilities", columnDefinition = "TEXT")
    private String facilities;
}

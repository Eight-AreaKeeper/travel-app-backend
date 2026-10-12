package com.travelapp.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Corresponds to Database Design Document pois table -- standard library of attractions/dining/lodging,
 * used to reference real locations during itinerary generation.
 */
@Getter
@Setter
@TableName("pois")
public class Poi {

    /** type codes: 1=attraction/2=dining/3=lodging */
    public static final int TYPE_SCENIC = 1;
    public static final int TYPE_FOOD = 2;
    public static final int TYPE_HOTEL = 3;

    /** source codes: 1=official entry/2=third-party API (Amap)/3=crawled */
    public static final int SOURCE_OFFICIAL = 1;
    public static final int SOURCE_THIRD_PARTY_API = 2;
    public static final int SOURCE_CRAWLED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 1=attraction/2=dining/3=lodging */
    private Integer type;
    /** Only set when matched against the local destinations table; not required -- lookups rely on destinationName */
    private Long destinationId;
    /** Destination name (free text); the custom itinerary module links POIs by this field, not by destinationId */
    private String destinationName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal avgCost;
    private BigDecimal rating;
    @TableField("images")
    private String imagesJson;
    /** 1=official entry/2=third-party API/3=crawled */
    private Integer source;
    private LocalDateTime createdAt;
}

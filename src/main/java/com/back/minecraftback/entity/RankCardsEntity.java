package com.back.minecraftback.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rank_cards", schema = "mc_backend")
@Getter
@Setter
public class RankCardsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String imageUrl;

    @Column(name = "price_month")
    private Integer priceMonth;

    @Column(name = "price_three_months")
    private Integer priceThreeMonths;

    @Column(name = "price_year")
    private Integer priceYear;

    @Column(name = "allow_forever")
    private Boolean allowForever = false;

    @Column(name = "price_forever")
    private Integer priceForever;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] description;

    /** Подробное описание (длинный текст). */
    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    private Boolean active = true;
}

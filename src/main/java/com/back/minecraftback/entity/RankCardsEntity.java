package com.back.minecraftback.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    private Integer price;

    private String description;

    private Boolean active = true;
}
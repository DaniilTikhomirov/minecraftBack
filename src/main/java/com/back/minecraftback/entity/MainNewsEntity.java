package com.back.minecraftback.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(schema = "mc_backend", name = "main_news")
@Getter
@Setter
public class MainNewsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String description;

    private String date;

    private String imageUrl;

    private Boolean active = true;
}

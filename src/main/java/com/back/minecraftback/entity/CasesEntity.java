package com.back.minecraftback.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(schema = "mc_backend", name = "cases")
public class CasesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String subtitle;

    private String description;

    private String imageUrl;

    private Integer price;

    private Boolean active = true;
}

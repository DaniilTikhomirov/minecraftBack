package com.back.minecraftback.dto;

public record GetCasesDto(
    Long id,
    String title,
    String subtitle,
    String description,
    String imageUrl,
    Integer price
) {
}

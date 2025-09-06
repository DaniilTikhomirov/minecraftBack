package com.back.minecraftback.dto;

public record GetRankDto(
    Long id,
    String title,
    String imageUrl,
    Integer price,
    String description
) {
}

package com.back.minecraftback.dto;

public record GetNewsDto(
        Long id,
        String title,
        String description,
        String date,
        String imageUrl
) {
}

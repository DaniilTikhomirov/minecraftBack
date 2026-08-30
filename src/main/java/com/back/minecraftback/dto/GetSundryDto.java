package com.back.minecraftback.dto;

public record GetSundryDto(
        Long id,
        String title,
        String subtitle,
        String description,
        String detailedDescription,
        String imageUrl,
        Integer price,
        Boolean active
) {
}

package com.back.minecraftback.dto;

public record NewsDTO(
       Long id,
       String title,
       String description,
       String date,
       String imageBase64,
       String imageUrl
) {
}

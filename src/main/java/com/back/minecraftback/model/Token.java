package com.back.minecraftback.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Token {

    JWT_TOKEN("JWT_TOKEN"),
    REFRESH_TOKEN("REFRESH_TOKEN");

    private final String token;
}

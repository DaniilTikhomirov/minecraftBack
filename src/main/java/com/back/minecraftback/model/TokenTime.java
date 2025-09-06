package com.back.minecraftback.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TokenTime {

    JWT_TOKEN_TIME_IN_SECONDS(60 * 15),
    JWT_TOKEN_TIME_IN_MILISECONDS(1000 * 60 * 15),
    REFRESH_TOKEN_TIME_IN_SECONDS(60 * 60 * 24 * 7),
    REFRESH_TOKEN_TIME_IN_MILISECONDS(1000 * 60 * 60 * 24 * 7);


    private final long time;

    public int getTime(){
        return (int) time;
    }
}

package com.back.minecraftback.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AuthDTO(
        @JsonAlias({"login", "user", "email"}) String username,
        @JsonAlias({"rawPassword", "pass", "pwd"}) String password
) {
}

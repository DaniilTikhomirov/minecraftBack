package com.back.minecraftback.dto;

import com.back.minecraftback.model.Role;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateAdminDTO(
        @JsonProperty("username")
        @JsonAlias({"login", "user", "email"}) String username,
        @JsonProperty("password")
        @JsonAlias({"rawPassword", "pass", "pwd"}) String password,
        @JsonProperty("role") Role role
) {
}

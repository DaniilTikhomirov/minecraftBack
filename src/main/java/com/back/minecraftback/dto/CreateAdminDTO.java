package com.back.minecraftback.dto;

import com.back.minecraftback.model.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateAdminDTO(
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("role") Role role
) {
}

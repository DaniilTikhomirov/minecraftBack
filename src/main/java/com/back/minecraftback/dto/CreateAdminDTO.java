package com.back.minecraftback.dto;

import com.back.minecraftback.model.Role;

public record CreateAdminDTO(
        String username,
        String password,
        Role role
) {
}

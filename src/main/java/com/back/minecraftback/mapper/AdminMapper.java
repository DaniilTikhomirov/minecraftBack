package com.back.minecraftback.mapper;


import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.entity.AdminUsersEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mappings({
            @Mapping(target = "password", expression = "java(passwordEncoder.encode(adminDto.password()))"),
            @Mapping(target = "username", source = "username"),
            @Mapping(target = "enabled", expression = "java(true)")
    })
    AdminUsersEntity toEntity(CreateAdminDTO adminDto, @Context PasswordEncoder passwordEncoder);
}

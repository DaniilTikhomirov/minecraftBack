package com.back.minecraftback.mapper;


import com.back.minecraftback.dto.CreateAdminDTO;
import com.back.minecraftback.entity.AdminUsersEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "password", ignore = true),
            @Mapping(target = "username", source = "username"),
            @Mapping(target = "role", source = "role"),
            @Mapping(target = "enabled", expression = "java(true)")
    })
    AdminUsersEntity toEntity(CreateAdminDTO adminDto);
}

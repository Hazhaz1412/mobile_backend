package com.react.mobile.Mapper;

import com.react.mobile.DTO.request.RegisterRequest;
import com.react.mobile.DTO.response.UserResponse;
import com.react.mobile.Entity.AuthUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") 
public interface UserMapper {
     
    AuthUser toEntity(RegisterRequest request); 

    @Mapping(target = "isSuperuser", source = "isSuperuser")
    @Mapping(target = "isStaff", source = "isStaff")
    UserResponse toResponse(AuthUser user);
}

package com.react.mobile.Mapper;

import com.react.mobile.DTO.response.SocialAccountResponse;
import com.react.mobile.Entity.SocialAuthUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SocialUserMapper {
     
    SocialAccountResponse toResponse(SocialAuthUser entity);
     
}
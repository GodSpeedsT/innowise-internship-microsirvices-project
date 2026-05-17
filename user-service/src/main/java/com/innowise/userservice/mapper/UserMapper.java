package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

  User toEntity(UserRequestDto request);

  UserResponseDto toResponseDto(User user);

  void updateUserFromDto(UserRequestDto dto, @MappingTarget User user);
}

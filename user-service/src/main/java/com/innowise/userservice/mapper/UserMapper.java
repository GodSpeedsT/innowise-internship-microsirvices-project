package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "cards", ignore = true)
  User toEntity(UserRequestDto request);

  @Mapping(source = "id", target = "uuid")
  @Mapping(source = "birthDate", target = "birthday")
  @Mapping(target = "cards", ignore = true)
  UserResponseDto toResponseDto(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "cards", ignore = true)
  void updateUserFromDto(UserRequestDto dto, @MappingTarget User user);

  default UserResponseDto toResponseDtoWithCards(User user, List<PaymentCardResponseDto> cards) {
    UserResponseDto responseDto = toResponseDto(user);
    responseDto.setCards(cards);
    return responseDto;
  }
}

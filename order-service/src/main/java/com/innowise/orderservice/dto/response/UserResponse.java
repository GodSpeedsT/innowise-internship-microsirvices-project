package com.innowise.orderservice.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

  private String name;
  private String surname;
  private String email;
  private LocalDate birthday;

}

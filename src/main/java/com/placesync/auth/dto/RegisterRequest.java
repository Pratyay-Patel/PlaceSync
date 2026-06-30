package com.placesync.auth.dto;

import com.placesync.common.validation.ValidPassword;
import com.placesync.user.entity.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @ValidPassword
    private String password;

    @NotNull
    private UserRole role;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    // Required only for ROLE_STUDENT
    @Size(max = 255)
    private String institution;

    @Size(max = 255)
    private String department;

    @Min(2000) @Max(2100)
    private Short graduationYear;
}

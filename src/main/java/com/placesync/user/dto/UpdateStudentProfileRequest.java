package com.placesync.user.dto;

import com.placesync.common.validation.ValidCgpa;
import com.placesync.user.entity.GenderType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateStudentProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(min = 7, max = 20)
    private String phone;

    private LocalDate dateOfBirth;

    private GenderType gender;

    @NotBlank
    @Size(max = 255)
    private String institution;

    @NotBlank
    @Size(max = 255)
    private String department;

    @NotNull
    @Min(2000) @Max(2100)
    private Short graduationYear;

    @ValidCgpa
    private BigDecimal cgpa;

    @Size(max = 2000)
    private String bio;

    private Boolean isProfilePublic;
}

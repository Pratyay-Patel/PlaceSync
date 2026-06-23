package com.placesync.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StudentEducationRequest {

    @NotBlank
    @Size(max = 255)
    private String degree;

    @NotBlank
    @Size(max = 255)
    private String institution;

    @Size(max = 255)
    private String fieldOfStudy;

    @NotNull
    @Min(1980) @Max(2100)
    private Short startYear;

    @Min(1980) @Max(2100)
    private Short endYear;

    @DecimalMin("0.00")
    private BigDecimal percentageOrCgpa;
}

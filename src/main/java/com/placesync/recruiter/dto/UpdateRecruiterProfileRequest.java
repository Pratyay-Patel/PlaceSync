package com.placesync.recruiter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateRecruiterProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 255)
    private String jobTitle;

    @Email
    @Size(max = 255)
    private String contactEmail;

    @Size(min = 7, max = 20)
    private String phone;

    private UUID companyId;
}

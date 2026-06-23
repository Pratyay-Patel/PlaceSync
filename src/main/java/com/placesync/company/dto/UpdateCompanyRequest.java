package com.placesync.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 255)
    private String industry;

    @Size(max = 255)
    private String headquarters;
}

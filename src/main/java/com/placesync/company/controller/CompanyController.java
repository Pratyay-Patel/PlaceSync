package com.placesync.company.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.common.util.PagedResponse;
import com.placesync.company.dto.CompanyResponse;
import com.placesync.company.dto.CompanyVerificationRequest;
import com.placesync.company.dto.CreateCompanyRequest;
import com.placesync.company.dto.UpdateCompanyRequest;
import com.placesync.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Company", description = "Company profile and verification management")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/companies/{companyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a company by ID")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID companyId) {
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }

    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all verified companies (paginated)")
    public ResponseEntity<PagedResponse<CompanyResponse>> getVerifiedCompanies(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(companyService.getVerifiedCompanies(pageable));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Create a new company (recruiter only)")
    public ResponseEntity<CompanyResponse> createCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCompanyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createCompany(principal.getId(), req));
    }

    @PutMapping("/companies/{companyId}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Update a company (creator only)")
    public ResponseEntity<CompanyResponse> updateCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest req) {
        return ResponseEntity.ok(companyService.updateCompany(principal.getId(), companyId, req));
    }

    @DeleteMapping("/companies/{companyId}")
    @PreAuthorize("hasAuthority('ROLE_RECRUITER')")
    @Operation(summary = "Deactivate a company (creator only)")
    public ResponseEntity<Void> deleteCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companyId) {
        companyService.softDeleteCompany(principal.getId(), companyId);
        return ResponseEntity.noContent().build();
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/admin/companies/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List companies pending approval (admin only)")
    public ResponseEntity<PagedResponse<CompanyResponse>> getPendingCompanies(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(companyService.getPendingCompanies(pageable));
    }

    @PatchMapping("/admin/companies/{companyId}/verify")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Approve or reject a company (admin only)")
    public ResponseEntity<CompanyResponse> processVerification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyVerificationRequest req) {
        return ResponseEntity.ok(companyService.processVerification(principal.getId(), companyId, req));
    }
}

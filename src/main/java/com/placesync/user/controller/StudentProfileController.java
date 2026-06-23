package com.placesync.user.controller;

import com.placesync.common.security.UserPrincipal;
import com.placesync.user.dto.*;
import com.placesync.user.entity.StudentEducation;
import com.placesync.user.entity.StudentExperience;
import com.placesync.user.entity.StudentSkill;
import com.placesync.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_STUDENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Student Profile", description = "Student profile and portfolio management")
public class StudentProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get own student profile")
    public ResponseEntity<StudentProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getMyProfile(principal.getId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update student profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateStudentProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), req));
    }

    // ── Skills ───────────────────────────────────────────────────────────────

    @GetMapping("/profile/skills")
    @Operation(summary = "List all skills on the student profile")
    public ResponseEntity<List<StudentSkill>> getSkills(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getSkills(principal.getId()));
    }

    @PostMapping("/profile/skills")
    @Operation(summary = "Add a skill to the student profile")
    public ResponseEntity<Map<String, String>> addSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StudentSkillRequest req) {
        userService.addSkill(principal.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Skill added"));
    }

    @DeleteMapping("/profile/skills/{skillId}")
    @Operation(summary = "Remove a skill from the student profile")
    public ResponseEntity<Void> removeSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID skillId) {
        userService.removeSkill(principal.getId(), skillId);
        return ResponseEntity.noContent().build();
    }

    // ── Education ────────────────────────────────────────────────────────────

    @GetMapping("/profile/education")
    @Operation(summary = "List all education records")
    public ResponseEntity<List<StudentEducation>> getEducation(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getEducation(principal.getId()));
    }

    @PostMapping("/profile/education")
    @Operation(summary = "Add an education record")
    public ResponseEntity<StudentEducation> addEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StudentEducationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addEducation(principal.getId(), req));
    }

    @PutMapping("/profile/education/{educationId}")
    @Operation(summary = "Update an education record")
    public ResponseEntity<StudentEducation> updateEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId,
            @Valid @RequestBody StudentEducationRequest req) {
        return ResponseEntity.ok(userService.updateEducation(principal.getId(), educationId, req));
    }

    @DeleteMapping("/profile/education/{educationId}")
    @Operation(summary = "Delete an education record")
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID educationId) {
        userService.deleteEducation(principal.getId(), educationId);
        return ResponseEntity.noContent().build();
    }

    // ── Experience ───────────────────────────────────────────────────────────

    @GetMapping("/profile/experience")
    @Operation(summary = "List all experience records")
    public ResponseEntity<List<StudentExperience>> getExperience(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getExperience(principal.getId()));
    }

    @PostMapping("/profile/experience")
    @Operation(summary = "Add an experience record")
    public ResponseEntity<StudentExperience> addExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StudentExperienceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addExperience(principal.getId(), req));
    }

    @PutMapping("/profile/experience/{experienceId}")
    @Operation(summary = "Update an experience record")
    public ResponseEntity<StudentExperience> updateExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId,
            @Valid @RequestBody StudentExperienceRequest req) {
        return ResponseEntity.ok(userService.updateExperience(principal.getId(), experienceId, req));
    }

    @DeleteMapping("/profile/experience/{experienceId}")
    @Operation(summary = "Delete an experience record")
    public ResponseEntity<Void> deleteExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId) {
        userService.deleteExperience(principal.getId(), experienceId);
        return ResponseEntity.noContent().build();
    }
}

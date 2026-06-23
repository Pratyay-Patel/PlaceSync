package com.placesync.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_student_educations_student"))
    private StudentProfile student;

    @Column(name = "degree", nullable = false, length = 255)
    private String degree;

    @Column(name = "institution", nullable = false, length = 255)
    private String institution;

    @Column(name = "field_of_study", length = 255)
    private String fieldOfStudy;

    @Column(name = "start_year", nullable = false)
    private Short startYear;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "percentage_or_cgpa", precision = 5, scale = 2)
    private BigDecimal percentageOrCgpa;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}

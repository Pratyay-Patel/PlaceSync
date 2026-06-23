package com.placesync.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "job_eligible_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEligibleDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_eligible_departments_job"))
    private Job job;

    @Column(name = "department_name", nullable = false, length = 255)
    private String departmentName;
}

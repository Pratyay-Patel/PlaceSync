package com.placesync.company.repository;

import com.placesync.company.entity.Company;
import com.placesync.company.entity.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndDeletedAtIsNull(UUID id);

    Page<Company> findByStatusAndDeletedAtIsNull(CompanyStatus status, Pageable pageable);

    boolean existsByNameAndDeletedAtIsNull(String name);

    long countByDeletedAtIsNull();
}

package com.placesync.user.repository;

import com.placesync.user.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentSkillRepository extends JpaRepository<StudentSkill, UUID> {

    List<StudentSkill> findByStudentId(UUID studentId);

    void deleteByStudentIdAndSkillName(UUID studentId, String skillName);

    boolean existsByStudentIdAndSkillName(UUID studentId, String skillName);
}

package com.atlas.modules.milestones.repository;

import com.atlas.modules.milestones.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

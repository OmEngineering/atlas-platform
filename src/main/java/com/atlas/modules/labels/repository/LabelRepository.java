package com.atlas.modules.labels.repository;

import com.atlas.modules.labels.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectIdOrderByNameAsc(UUID projectId);

    boolean existsByOrganizationIdAndProjectIdAndNameIgnoreCase(UUID organizationId, UUID projectId, String name);

    Optional<Label> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

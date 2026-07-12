package com.atlas.modules.organizations.repository;

import com.atlas.modules.organizations.entity.Organization;
import com.atlas.modules.organizations.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);
}

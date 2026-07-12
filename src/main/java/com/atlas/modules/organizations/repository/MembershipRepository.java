package com.atlas.modules.organizations.repository;

import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByOrganizationIdAndUserIdAndStatus(
            UUID organizationId, UUID userId, MembershipStatus status);

    Page<Membership> findByOrganizationIdAndStatus(UUID organizationId, MembershipStatus status, Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, MembershipStatus status);
}

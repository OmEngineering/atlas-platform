package com.atlas.modules.invitations.repository;

import com.atlas.modules.invitations.entity.Invitation;
import com.atlas.modules.invitations.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, InvitationStatus status);

    Optional<Invitation> findByOrganizationIdAndEmailAndStatus(
            UUID organizationId, String email, InvitationStatus status);

    Optional<Invitation> findByIdAndStatus(UUID id, InvitationStatus status);
}

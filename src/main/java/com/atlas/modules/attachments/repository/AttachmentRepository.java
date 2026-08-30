package com.atlas.modules.attachments.repository;

import com.atlas.modules.attachments.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    long countByTaskId(UUID taskId);
}

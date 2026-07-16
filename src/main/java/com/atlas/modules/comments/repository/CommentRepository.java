package com.atlas.modules.comments.repository;

import com.atlas.modules.comments.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    long countByTaskIdAndDeletedAtIsNull(UUID taskId);

    List<Comment> findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID taskId);

    Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);
}

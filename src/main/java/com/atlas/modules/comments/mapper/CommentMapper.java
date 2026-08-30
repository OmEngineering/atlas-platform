package com.atlas.modules.comments.mapper;

import com.atlas.modules.comments.dto.CommentResponse;
import com.atlas.modules.comments.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTaskId(),
                comment.getAuthorId(),
                comment.getBody(),
                comment.getEditedAt() != null,
                comment.getEditedAt(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}

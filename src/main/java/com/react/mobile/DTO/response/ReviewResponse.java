package com.react.mobile.DTO.response;

import com.react.mobile.Entity.Enums.ReviewModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private String targetType;
    private String targetId;
    private String targetName;
    private Double rating;
    private String comment;
    private String photoUrl;
    private String authorUsername;
    private Long authorId;
    private Long helpfulCount;
    private Boolean helpfulByCurrentUser;
    private Long flagCount;
    private ReviewModerationStatus moderationStatus;
    private String ownerReply;
    private String ownerReplyAuthor;
    private Long ownerReplyAuthorId;
    private String ownerReplyAt;
    private Boolean canReply;
    private Boolean canModerate;
    private List<String> reportReasons;
    private String createdAt;
    private String updatedAt;
}

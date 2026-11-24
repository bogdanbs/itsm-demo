package de.bintintan.itsm.ticket.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tickets", schema = "itsm")
public class Ticket {

    @Id
    @Column(name = "id")
    private UUID id;
    @NotBlank
    @Column(name = "title", nullable = false)
    @Size(max = 100)
    private String title;
    @Column(name = "description")
    private String description;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TicketPriority priority;
    @Column(name = "assignee_id")
    private UUID assigneeId;
    @Column(name = "pending_user_since")
    private Instant pendingUserSince;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @NotNull
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Version
    private int version;

    protected Ticket() {
    }

    public Ticket(String title, String description, TicketPriority priority, Instant dueAt) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueAt = dueAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void startProgress(Instant now) {
        if (this.status != TicketStatus.OPEN) {
            throw new TicketProcessingException("Ticket is not currently OPEN.");
        }
        validateTransition(TicketStatus.IN_PROGRESS);
        transitionTo(TicketStatus.IN_PROGRESS, now);
    }

    public void assignTo(UUID assigneeId) {
        if (this.status == TicketStatus.RESOLVED || this.status == TicketStatus.CLOSED) {
            throw new TicketProcessingException("Ticket can no longer be assigned to. Status " + this.status);
        }
        this.assigneeId = Objects.requireNonNull(assigneeId, "assigneeId must not be null");
    }

    public void requestUserInput(Instant now) {
        validateTransition(TicketStatus.PENDING_USER);
        transitionTo(TicketStatus.PENDING_USER, now);
        this.pendingUserSince = now;
    }

    public void onUserReply(Instant now) {
        if (this.status == TicketStatus.PENDING_USER) {
            TicketStatus next = (this.assigneeId != null) ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN;
            if (this.status.canTransitionTo(next)) {
                transitionTo(next, now);
            } else {
                throw new InvalidStatusTransitionException(this.status, next);
            }
        }
    }

    public void putOnHold(Instant now) {
        validateTransition(TicketStatus.ON_HOLD);
        transitionTo(TicketStatus.ON_HOLD, now);
    }

    public void resumeFromHold(Instant now) {
        if (this.status != TicketStatus.ON_HOLD) {
            throw new TicketProcessingException("Ticket is not currently ON_HOLD.");
        }
        TicketStatus nextStatus = TicketStatus.IN_PROGRESS;
        if (this.assigneeId == null) {
            nextStatus = TicketStatus.OPEN;
        }
        validateTransition(nextStatus);
        transitionTo(nextStatus, now);
    }

    public void resolve(Instant now) {
        validateTransition(TicketStatus.RESOLVED);
        transitionTo(TicketStatus.RESOLVED, now);
        this.resolvedAt = now;
    }

    public void close(Instant now) {
        validateTransition(TicketStatus.CLOSED);
        transitionTo(TicketStatus.CLOSED, now);
        this.closedAt = now;
    }

    public void reopen(Instant now) {
        switch (this.status) {
            case TicketStatus.RESOLVED -> {
                validateTransition(TicketStatus.IN_PROGRESS);
                transitionTo(TicketStatus.IN_PROGRESS, now);
            }
            case TicketStatus.CLOSED -> {
                validateTransition(TicketStatus.OPEN);
                transitionTo(TicketStatus.OPEN, now);
            }
            default -> throw new TicketProcessingException(
                    "Only RESOLVED or CLOSED tickets can be reopened. Status " + this.status);
        }
    }

    private void validateTransition(TicketStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(this.status, target);
        }
    }

    private void transitionTo(TicketStatus target, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (target == TicketStatus.IN_PROGRESS && this.assigneeId == null) {
            throw new TicketProcessingException("Ticket must be assigned before work can start.");
        }

        this.status = target;

        if (target != TicketStatus.PENDING_USER) {
            this.pendingUserSince = null;
        }
        if (target != TicketStatus.RESOLVED && target != TicketStatus.CLOSED) {
            this.resolvedAt = null;
        }
        if (target != TicketStatus.CLOSED) {
            this.closedAt = null;
        }
    }

    public void verifyVersion(int expectedVersion) throws TicketVersionConflictException {
        if (version != expectedVersion) {
            throw new TicketVersionConflictException(id, expectedVersion, version);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public Instant getPendingUserSince() {
        return pendingUserSince;
    }

    public int getVersion() {
        return version;
    }

    public void updateData(@Nullable String title,
                           @Nullable String description,
                           @Nullable TicketPriority priority,
                           @Nullable Instant dueAt) {
        if (this.status == TicketStatus.CLOSED) {
            throw new TicketProcessingException("Ticket is closed and can no longer be updated.");
        }
        if (!StringUtils.isBlank(title)) {
            this.title = title;
        }
        this.description = description;
        if (priority != null) {
            this.priority = priority;
        }
        if (dueAt != null) {
            this.dueAt = dueAt;
        }
    }
}

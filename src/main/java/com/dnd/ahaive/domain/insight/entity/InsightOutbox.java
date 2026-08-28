package com.dnd.ahaive.domain.insight.entity;

import com.dnd.ahaive.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightOutbox extends BaseEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Insight insight;

    @Enumerated(value = EnumType.STRING)
    private OutboxStatus status;

    public static InsightOutbox pendingFrom(Insight insight) {
        return new InsightOutbox(insight, OutboxStatus.PENDING);
    }

    private InsightOutbox(Insight insight, OutboxStatus status) {
        this.insight = insight;
        this.status = status;
    }

    public void markCompleted() {
        this.status = OutboxStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILURE;
    }
}

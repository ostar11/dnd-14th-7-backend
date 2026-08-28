package com.dnd.ahaive.domain.insight.entity;

public enum OutboxStatus {

    PENDING("대기중"),
    COMPLETED("처리 완료"),
    FAILURE("처리 실패");

    private final String description;

    OutboxStatus(String description) {
        this.description = description;
    }
}

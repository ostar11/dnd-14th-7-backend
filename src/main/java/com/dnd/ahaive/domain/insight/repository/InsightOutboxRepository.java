package com.dnd.ahaive.domain.insight.repository;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.entity.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightOutboxRepository extends JpaRepository<InsightOutbox, Long> {

    boolean existsByInsightIdAndStatus(Long insightId, OutboxStatus status);

    Optional<InsightOutbox> findByInsight(Insight insight);


    List<InsightOutbox> findByStatusAndCreatedAtBefore(OutboxStatus status,
                                                       LocalDateTime createAt,
                                                       PageRequest pageRequest);
}

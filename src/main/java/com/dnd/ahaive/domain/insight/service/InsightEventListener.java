package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.service.dto.InsightDocumentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InsightEventListener {

    private final InsightDocumentSyncService insightDocumentSyncService;

    @Async("esSyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void saveInsightDocument(InsightDocumentRequest request) {
        insightDocumentSyncService.syncDocument(request);
    }
}

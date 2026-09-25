package com.dnd.ahaive.domain.insight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.entity.OutboxStatus;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.question.repository.QuestionRepository;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import com.dnd.ahaive.domain.tag.repository.InsightTagRepository;
import com.dnd.ahaive.domain.tag.repository.TagEntityRepository;
import com.dnd.ahaive.domain.user.entity.Provider;
import com.dnd.ahaive.domain.user.entity.User;
import com.dnd.ahaive.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * outbox 패턴(OutboxEventListener → InsightEventListener → InsightDocumentSyncService →
 * InsightOutboxRelayScheduler)이 PG-ES 동기화 중 장애 상황에서도 최종적으로 데이터 일관성을
 * 지키는지 검증한다. ES 쓰기는 Testcontainers로 띄운 실제 Elasticsearch에 반영되며,
 * 특정 시점의 장애만 {@link ElasticsearchInsightService}를 스파이로 감싸 결정적으로 주입한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InsightOutboxConsistencyTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.2.3"))
            .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    @Autowired
    UserRepository userRepository;
    @Autowired
    InsightRepository insightRepository;
    @Autowired
    InsightPieceRepository insightPieceRepository;
    @Autowired
    InsightOutboxRepository insightOutboxRepository;
    @Autowired
    TagEntityRepository tagEntityRepository;
    @Autowired
    InsightTagRepository insightTagRepository;
    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    InsightCreationService insightCreationService;
    @Autowired
    InsightOutboxRelayScheduler insightOutboxRelayScheduler;
    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @MockitoSpyBean
    ElasticsearchInsightService elasticsearchInsightService;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    User user;

    @BeforeEach
    void setup() {
        reset(elasticsearchInsightService);
        user = User.createMember("nickname", 0, "email", Provider.GOOGLE, "provided");
        userRepository.save(user);
    }

    @AfterEach
    void teardown() {
        elasticsearchOperations.indexOps(InsightDocument.class).delete();
        insightOutboxRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        insightTagRepository.deleteAllInBatch();
        tagEntityRepository.deleteAllInBatch();
        insightPieceRepository.deleteAllInBatch();
        insightRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private AiInsightResponse buildAiInsightResponse() {
        AiTagResponse aiTagResponse = new AiTagResponse();
        aiTagResponse.setTags(List.of());

        AiQuestionResponse aiQuestionResponse = new AiQuestionResponse();
        aiQuestionResponse.setQuestions(List.of());

        return new AiInsightResponse("title", "insight piece content", aiTagResponse, aiQuestionResponse);
    }

    private Insight saveInsightWithInitPiece(String initThought, String title, String pieceContent) {
        Insight insight = Insight.from(initThought, title, user);
        insightRepository.save(insight);
        insightPieceRepository.save(InsightPiece.of(insight, pieceContent, InsightGenerationType.INIT));
        return insight;
    }

    /** 스케줄러의 10초 유예 기간을 넘겨야 하는 테스트를 위해 outbox의 createdAt을 과거로 이동시킨다. */
    private void backdateOutboxCreatedAt(Long insightId, long secondsAgo) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                entityManager.createQuery("update InsightOutbox o set o.createdAt = :createdAt where o.id = :id")
                        .setParameter("createdAt", LocalDateTime.now().minusSeconds(secondsAgo))
                        .setParameter("id", insightId)
                        .executeUpdate());
        entityManager.clear();
    }

    private OutboxStatus fetchOutboxStatus(Long insightId) {
        return insightOutboxRepository.findById(insightId).orElseThrow().getStatus();
    }

    private void assertEsDocumentMatches(Long insightId, Insight insight, String expectedFirstPiece) {
        InsightDocument document = elasticsearchOperations.get(String.valueOf(insightId), InsightDocument.class);
        assertThat(document).isNotNull();
        assertThat(document.getTitle()).isEqualTo(insight.getTitle());
        assertThat(document.getInitThought()).isEqualTo(insight.getInitThought());
        assertThat(document.getUserUuid()).isEqualTo(user.getUserUuid());
        assertThat(document.isTrash()).isEqualTo(insight.isTrash());
        assertThat(document.getFirstInsightPiece()).isEqualTo(expectedFirstPiece);
    }

    @Test
    @DisplayName("장애 없이 저장하면 PG와 ES의 InsightDocument가 최종적으로 완전히 일치한다")
    void asyncSyncEventuallyPersistsMatchingDocumentToElasticsearch() {
        // when
        Long insightId = insightCreationService.save("initThought", user, buildAiInsightResponse());

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.COMPLETED));

        Insight insight = insightRepository.findById(insightId).orElseThrow();
        assertEsDocumentMatches(insightId, insight, "insight piece content");
    }

    @Test
    @DisplayName("1차 비동기 ES 동기화가 실패하면 outbox는 PENDING을 유지하고 ES에는 문서가 저장되지 않는다")
    void asyncSyncFailureLeavesOutboxPendingWithoutEsDocument() {
        // given: ES 쓰기의 유일한 통로인 saveDocument()만 실패하도록 스파이에 주입한다.
        // InsightEventListener -> InsightDocumentSyncService.syncDocument()가 이 예외를 그대로
        // 전파하면 @Async 메서드이므로 예외는 Spring 기본 핸들러에 조용히 삼켜지고,
        // markOutboxComplete()는 호출되지 않는다 -> outbox가 섣불리 COMPLETED로 전이되면 안 된다.
        doThrow(new RuntimeException("es down")).when(elasticsearchInsightService).saveDocument(any());

        // when
        Long insightId = insightCreationService.save("initThought", user, buildAiInsightResponse());

        // then: 비동기 처리가 실행되고 실패할 시간을 넉넉히 준 뒤에도 outbox는 PENDING이어야 한다
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.PENDING));

        assertThat(elasticsearchOperations.exists(String.valueOf(insightId), InsightDocument.class)).isFalse();
    }

    @Test
    @DisplayName("1차 동기화 실패 후에도 폴백 스케줄러가 재시도해 outbox가 COMPLETED로 수렴하고 ES 데이터가 PG와 일치한다")
    void fallbackSchedulerRecoversAfterAsyncSyncFailure() {
        // given: 앞 테스트와 동일하게 1차 비동기 동기화를 실패시켜 outbox를 PENDING에 묶어둔다.
        doThrow(new RuntimeException("es down")).when(elasticsearchInsightService).saveDocument(any());

        Long insightId = insightCreationService.save("initThought", user, buildAiInsightResponse());

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.PENDING));

        // 스케줄러가 집어갈 수 있도록 outbox의 생성 시각을 10초 유예 기간 밖으로 되돌린다.
        backdateOutboxCreatedAt(insightId, 15);

        // when: InsightOutboxRelayScheduler는 saveDocument가 아니라 saveAll을 호출하는 별도 경로라서
        // 위에서 걸어둔 saveDocument 스텁의 영향을 받지 않고 실제 ES에 정상적으로 저장된다.
        // (프로덕션 코드에서 비동기 경로와 폴백 경로가 서로 다른 메서드를 쓴다는 사실 자체를
        //  이 테스트가 그대로 이용해 "재시도가 실제로 다른 경로로 성공하는지"를 검증한다.)
        insightOutboxRelayScheduler.relay();

        // then
        assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.COMPLETED);
        Insight insight = insightRepository.findById(insightId).orElseThrow();
        assertEsDocumentMatches(insightId, insight, "insight piece content");
    }

    @Test
    @DisplayName("INIT InsightPiece가 없는 상태로 스케줄러가 처리하면 outbox가 FAILURE로 전이되고 ES에는 아무 것도 저장되지 않는다")
    void schedulerMarksOutboxAsFailureWhenInitPieceMissing() {
        // given: InsightCreationService.save() 안에서는 Insight/INIT InsightPiece/Outbox가
        // 항상 같은 트랜잭션으로 함께 커밋되므로 이 상태는 생성 과정만으로는 재현되지 않는다.
        // 다만 InsightService.deleteInsightPiece()가 INIT 조각 삭제를 막기 전에는
        // "생성 직후, ES 동기화 전에 INIT 조각을 삭제"하는 경로로 실제로 발생할 수 있었다.
        // 지금은 그 경로가 막혀 있어 재현 불가능하지만, 스케줄러의 방어 코드 자체가
        // 앞으로도 안전하게 동작하는지 보장하기 위한 회귀 테스트로 repository를 직접 조작해 재현한다.
        Insight insight = Insight.from("initThought", "title", user);
        insightRepository.save(insight);
        insightOutboxRepository.save(InsightOutbox.pendingFrom(insight));
        backdateOutboxCreatedAt(insight.getId(), 15);

        // when
        insightOutboxRelayScheduler.relay();

        // then: 현재 구현상 FAILURE는 재처리 로직이 없는 종료 상태다 - 이후 스케줄러 tick에서도 다시 시도되지 않는다.
        assertThat(fetchOutboxStatus(insight.getId())).isEqualTo(OutboxStatus.FAILURE);
        assertThat(elasticsearchOperations.exists(String.valueOf(insight.getId()), InsightDocument.class)).isFalse();
    }

    @Test
    @DisplayName("배치 처리 중 ES 저장이 실패하면 해당 배치의 모든 outbox 상태 변경이 롤백되어 PENDING으로 남는다")
    void batchEsFailureRollsBackAllStatusChangesInThatRelay() {
        // given: 같은 relay() 배치에 "정상 처리 대상" 1건과 "INIT piece가 없어 FAILURE 처리될 대상" 1건을
        // 함께 넣는다. relay()는 @Transactional 하나로 묶여 있어 markFailed() 호출과 saveAll() 호출이
        // 같은 트랜잭션 안에 있다 - saveAll()이 예외를 던지면 이미 메모리상 markFailed() 처리된
        // brokenInsight의 outbox까지 통째로 롤백되는지가 이 테스트의 핵심이다.
        // (brokenInsight 상태는 위 테스트와 마찬가지로 deleteInsightPiece의 INIT 삭제 방지 로직
        //  적용 전에는 실제로 발생 가능했던 상태이며, 지금은 스케줄러 방어 코드의 회귀 테스트다.)
        Insight normalInsight = saveInsightWithInitPiece("initThought1", "title1", "piece1");
        insightOutboxRepository.save(InsightOutbox.pendingFrom(normalInsight));
        backdateOutboxCreatedAt(normalInsight.getId(), 15);

        Insight brokenInsight = Insight.from("initThought2", "title2", user);
        insightRepository.save(brokenInsight);
        insightOutboxRepository.save(InsightOutbox.pendingFrom(brokenInsight));
        backdateOutboxCreatedAt(brokenInsight.getId(), 15);

        doThrow(new RuntimeException("es down")).when(elasticsearchInsightService).saveAll(anyList());

        // when & then
        assertThatThrownBy(() -> insightOutboxRelayScheduler.relay())
                .isInstanceOf(RuntimeException.class);

        // 알려진 리스크: brokenInsight는 원래대로라면 FAILURE로 남아야 하지만, saveAll() 예외로
        // relay() 트랜잭션 전체가 롤백되면서 markFailed() 호출분까지 함께 되돌아가 PENDING으로 남는다.
        // 즉 다음 tick에서 두 건 모두 다시 시도된다 - 의도된 동작인지 확인할 목적의 테스트다.
        assertThat(fetchOutboxStatus(normalInsight.getId())).isEqualTo(OutboxStatus.PENDING);
        assertThat(fetchOutboxStatus(brokenInsight.getId())).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("비동기 리스너와 스케줄러가 동시에 처리해도 최종적으로 하나의 일관된 상태로 수렴한다")
    void concurrentAsyncListenerAndSchedulerConvergeToConsistentState() {
        // given: ES 스텁 없이(둘 다 실 ES로 통과) save()를 호출하면 비동기 리스너가 즉시
        // 백그라운드 스레드에서 syncDocument()를 시도한다.
        Long insightId = insightCreationService.save("initThought", user, buildAiInsightResponse());
        backdateOutboxCreatedAt(insightId, 15);

        // when: 비동기 리스너가 아직 처리 중일 수 있는 타이밍에 메인 스레드에서 스케줄러를 바로 실행해
        // 두 경로가 같은 insightId를 동시에 처리하도록 경쟁시킨다. ES 문서 id가 insightId로 고정돼
        // 있어 "중복 저장"은 구조적으로 불가능하므로(upsert), 실제로 검증하려는 위험은
        // "한쪽이 처리 중인 걸 다른 쪽이 건드려서 예외를 던지거나 상태를 깨뜨리는지"다.
        assertThatCode(() -> insightOutboxRelayScheduler.relay()).doesNotThrowAnyException();

        // then: 어느 경로가 먼저 끝나든 최종 상태는 COMPLETED 하나로 수렴해야 한다
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.COMPLETED));

        Insight insight = insightRepository.findById(insightId).orElseThrow();
        assertEsDocumentMatches(insightId, insight, "insight piece content");
    }

    @Test
    @DisplayName("생성된 지 10초 미만인 PENDING outbox는 스케줄러가 즉시 가져가지 않는다")
    void schedulerDoesNotPickUpOutboxYoungerThanGracePeriod() {
        // given: 비동기 경로가 먼저 COMPLETED로 전이시켜 버리면 "스케줄러가 안 가져갔다"는 것과
        // "비동기 경로가 이미 끝냈다"는 것을 구분할 수 없으므로, saveDocument를 실패시켜
        // 비동기 경로를 무력화한 채 스케줄러의 10초 유예 조건만 순수하게 검증한다.
        doThrow(new RuntimeException("es down")).when(elasticsearchInsightService).saveDocument(any());

        Long insightId = insightCreationService.save("initThought", user, buildAiInsightResponse());

        // when: createdAt을 되돌리지 않은 채(방금 생성돼 10초 미만) 바로 스케줄러를 실행한다
        insightOutboxRelayScheduler.relay();

        // then: findByStatusAndCreatedAtBefore(now - 10s) 조건에 걸리지 않아 이번 tick에서는 스킵된다
        assertThat(fetchOutboxStatus(insightId)).isEqualTo(OutboxStatus.PENDING);
        assertThat(elasticsearchOperations.exists(String.valueOf(insightId), InsightDocument.class)).isFalse();
    }
}

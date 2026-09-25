package com.dnd.ahaive.domain.insight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.question.entity.Question;
import com.dnd.ahaive.domain.question.entity.QuestionStatus;
import com.dnd.ahaive.domain.question.repository.QuestionRepository;
import com.dnd.ahaive.domain.question.service.QuestionService;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import com.dnd.ahaive.domain.tag.entity.InsightTag;
import com.dnd.ahaive.domain.tag.entity.TagEntity;
import com.dnd.ahaive.domain.tag.repository.InsightTagRepository;
import com.dnd.ahaive.domain.tag.repository.TagEntityRepository;
import com.dnd.ahaive.domain.user.entity.Provider;
import com.dnd.ahaive.domain.user.entity.User;
import com.dnd.ahaive.domain.user.repository.UserRepository;
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
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * InsightCreationService.save()의 저장 정확성 + 트랜잭션 원자성 검증.
 * 장애 주입 없이 "정상 입력이 의도한 대로 저장되는지"만 다룬다.
 * outbox-ES 파이프라인의 장애 상황 검증은 {@link InsightOutboxConsistencyTest}에서 다룬다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InsightCreationServiceTest {

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
    ElasticsearchOperations elasticsearchOperations;

    @MockitoSpyBean
    QuestionService questionService;

    User user;

    @BeforeEach
    void setup() {
        reset(questionService);
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

    private AiInsightResponse buildAiInsightResponse(List<String> tags, List<String> questions) {
        AiTagResponse aiTagResponse = new AiTagResponse();
        aiTagResponse.setTags(tags);

        AiQuestionResponse aiQuestionResponse = new AiQuestionResponse();
        aiQuestionResponse.setQuestions(questions);

        return new AiInsightResponse("title", "insight piece content", aiTagResponse, aiQuestionResponse);
    }

    @Test
    @DisplayName("save() 호출 시 Insight/InsightPiece/InsightTag/Question이 모두 정상 저장된다")
    void saveInsightAndAllRelatedEntities() {
        // given
        AiInsightResponse aiInsightResponse = buildAiInsightResponse(List.of("태그A", "태그B"), List.of("질문1", "질문2"));

        // when
        Long insightId = insightCreationService.save("initThought", user, aiInsightResponse);

        // then
        Insight insight = insightRepository.findById(insightId).orElseThrow();
        assertThat(insight.getTitle()).isEqualTo("title");
        assertThat(insight.getInitThought()).isEqualTo("initThought");

        List<InsightPiece> pieces = insightPieceRepository.findAllByInsightIdOrderByCreatedAtAsc(insightId);
        assertThat(pieces).hasSize(1);
        assertThat(pieces.get(0).getCreatedType()).isEqualTo(InsightGenerationType.INIT);
        assertThat(pieces.get(0).getContent()).isEqualTo("insight piece content");

        List<InsightTag> insightTags = insightTagRepository.findAllByInsightId(insightId);
        assertThat(insightTags).hasSize(2);

        List<Question> questions =
                questionRepository.findAllByInsightIdAndStatusOrderByCreatedAtDesc(insightId, QuestionStatus.WAITING);
        assertThat(questions).hasSize(2);
        assertThat(questions).extracting(Question::getContent).containsExactlyInAnyOrder("질문1", "질문2");
    }

    @Test
    @DisplayName("이미 존재하는 유저 태그는 재사용하고, 새로운 태그만 TagEntity로 추가 생성된다")
    void reuseExistingTagEntityAndCreateOnlyNewInsightTag() {
        // given
        TagEntity existingTag = TagEntity.of(user, "태그A");
        tagEntityRepository.save(existingTag);

        AiInsightResponse aiInsightResponse = buildAiInsightResponse(List.of("태그A", "태그B"), List.of());

        // when
        Long insightId = insightCreationService.save("initThought", user, aiInsightResponse);

        // then
        List<TagEntity> userTags = tagEntityRepository.findAllByUserId(user.getId());
        assertThat(userTags).hasSize(2);

        List<InsightTag> insightTags = insightTagRepository.findAllByInsightId(insightId);
        assertThat(insightTags).hasSize(2);
        assertThat(insightTags)
                .extracting(insightTag -> insightTag.getTagEntity().getId())
                .contains(existingTag.getId());
    }

    @Test
    @DisplayName("save() 트랜잭션 중간에 예외가 발생하면 Insight와 Outbox 모두 저장되지 않는다")
    void rollbackInsightAndOutboxWhenSaveFailsMidTransaction() {
        // given
        // questionService.saveQuestions(...)는 save() 안에서 Insight/InsightPiece/InsightTag를
        // 모두 저장한 "이후"에 호출되는 마지막 저장 단계다. 여기서 예외를 던지게 하면
        // - 앞서 저장된 Insight/InsightPiece/InsightTag까지 롤백되는지
        // - InsightDocumentRequest 이벤트 발행(그리고 그 결과인 Outbox 생성)까지 도달하지 못하고
        //   트랜잭션 전체가 롤백되는지
        // 를 한 번에 검증할 수 있다.
        AiInsightResponse aiInsightResponse = buildAiInsightResponse(List.of(), List.of("질문1"));
        doThrow(new RuntimeException("question save failed"))
                .when(questionService).saveQuestions(any(), any());

        // when & then
        assertThatThrownBy(() -> insightCreationService.save("initThought", user, aiInsightResponse))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("question save failed");

        assertThat(insightRepository.count()).isZero();
        assertThat(insightOutboxRepository.count()).isZero();
    }
}

package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.history.entity.AnswerInsightPromotion;
import com.dnd.ahaive.domain.history.exception.AlreadyConvertedAnswerException;
import com.dnd.ahaive.domain.history.repository.AnswerInsightPromotionRepository;
import com.dnd.ahaive.domain.insight.dto.request.AnswerToInsightRequest;
import com.dnd.ahaive.domain.insight.dto.request.InsightCreateRequest;
import com.dnd.ahaive.domain.insight.dto.request.PieceCreateRequest;
import com.dnd.ahaive.domain.insight.dto.request.PieceUpdateRequest;
import com.dnd.ahaive.domain.insight.dto.request.TitleUpdateRequest;
import com.dnd.ahaive.domain.insight.dto.response.AiInsightCandidateResponse;
import com.dnd.ahaive.domain.insight.dto.response.InsightCandidateReGenResponse;
import com.dnd.ahaive.domain.insight.dto.response.InsightCreateResponse;
import com.dnd.ahaive.domain.insight.dto.response.InsightDetailResponse;
import com.dnd.ahaive.domain.insight.dto.response.InsightListResponse;
import com.dnd.ahaive.domain.insight.dto.response.InsightPieceResponse;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightCandidate;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.entity.InsightSortType;
import com.dnd.ahaive.domain.insight.exception.InsightNotFoundException;
import com.dnd.ahaive.domain.insight.repository.InsightCandidateRepository;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.question.entity.Answer;
import com.dnd.ahaive.domain.question.exception.AnswerNotFoundException;
import com.dnd.ahaive.domain.question.repository.AnswerRepository;
import com.dnd.ahaive.domain.tag.entity.InsightTag;
import com.dnd.ahaive.domain.tag.entity.TagEntity;
import com.dnd.ahaive.domain.tag.exception.TagNotFoundException;
import com.dnd.ahaive.domain.tag.repository.InsightTagRepository;
import com.dnd.ahaive.domain.tag.repository.TagEntityRepository;
import com.dnd.ahaive.domain.user.entity.User;
import com.dnd.ahaive.domain.user.repository.UserRepository;
import com.dnd.ahaive.global.exception.ErrorCode;
import com.dnd.ahaive.global.exception.InvalidInputValueException;
import com.dnd.ahaive.global.security.exception.UserNotFoundException;
import com.dnd.ahaive.infra.claude.ClaudeAiClient;
import com.dnd.ahaive.infra.claude.prompt.ClaudeAiPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

  private final UserRepository userRepository;
  private final InsightRepository insightRepository;
  private final InsightPieceRepository insightPieceRepository;
  private final TagEntityRepository tagEntityRepository;
  private final InsightTagRepository insightTagRepository;
  private final AnswerRepository answerRepository;
  private final AnswerInsightPromotionRepository answerInsightPromotionRepository;

  private final ClaudeAiClient claudeAiClient;
  private final ObjectMapper objectMapper;
  private final InsightCandidateRepository insightCandidateRepository;

  private final InsightAiService insightAiService;
  private final InsightCreationService insightCreationService;
  private final InsightValidator insightValidator;

  @Transactional
  public InsightCreateResponse createInsight(InsightCreateRequest insightCreateRequest, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    String initThought = insightCreateRequest.getMemo();

    // AI 호출 병렬 처리
    AiInsightResponse aiInsightResponse = insightAiService.generateInsightData(initThought);

    // 객체 저장
    Long insightId = insightCreationService.save(initThought, user, aiInsightResponse);

    return InsightCreateResponse.from(insightId);
  }

  /**
   * 사용자가 작성한 메모를(첫 생각) 기반으로 AI가 생성한 질문들을 반환합니다.
   * 질문 재생성 API 에서 사용.
   * @param initThought 첫 생각
   * @return AI가 생성한 질문들을 담은 AiQuestionResponse 객체
   */
  public AiQuestionResponse generateQuestions(String initThought) throws JsonProcessingException {
    String questionResponse = claudeAiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_QUESTION_PROMPT(initThought));
    return objectMapper.readValue(questionResponse, AiQuestionResponse.class);
  }

  @Transactional
  public InsightDetailResponse getInsightDetail(Long id, String uuid) {

    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(id, uuid);

    // 인사이트 조회수 증가
    insight.increaseView();

    // 태그 조회
    List<TagEntity> tags = insightTagRepository.findAllByInsightId(insight.getId()).stream()
        .map(InsightTag::getTagEntity)
        .toList();

    return InsightDetailResponse.of(insight, tags);
  }

  @Transactional
  public void createInsightFromAnswer(AnswerToInsightRequest answerToInsightRequest, Long insightId, String uuid) {

    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    // 답변이 존재하는지 확인
    Answer answer = answerRepository.findById(answerToInsightRequest.getAnswerId())
        .orElseThrow(() -> new AnswerNotFoundException(ErrorCode.ANSWER_NOT_FOUND));

    // 이미 인사이트로 변환된 이력이 있는지 확인
    if(answerInsightPromotionRepository.findByAnswerId(answerToInsightRequest.getAnswerId()).isPresent()) {
      throw new AlreadyConvertedAnswerException(ErrorCode.ALREADY_CONVERTED_ANSWER);
    }

    // 답변-인사이트 변환 및 인사이트를 저장
    String insightContent = claudeAiClient.sendMessage(ClaudeAiPrompt.ANSWER_TO_INSIGHT_PROMPT(answer.getContent()));
    InsightPiece insightPiece = InsightPiece.of(insight, insightContent,InsightGenerationType.ANSWER);

    insightPieceRepository.save(insightPiece);

    // 답변-인사이트 변환 이력 저장
    AnswerInsightPromotion answerInsightPromotion = AnswerInsightPromotion.of(insightPiece, answer);
    answerInsightPromotionRepository.save(answerInsightPromotion);

    // 답변 인사이트로 변환됨
    answer.convert();
  }

  @Transactional(readOnly = true)
  public InsightPieceResponse getInsightPieces(Long insightId, String uuid) {

    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    List<InsightPiece> insightPieces = insightPieceRepository.findAllByInsightIdOrderByCreatedAtAsc(insight.getId());

    return InsightPieceResponse.from(insightPieces);
  }

  @Transactional
  public void createInsightPiece(@Valid PieceCreateRequest pieceCreateRequest,
      Long insightId, String uuid) {

    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    InsightPiece insightPiece = InsightPiece.of(insight, pieceCreateRequest.getContent(), InsightGenerationType.SELF);

    insightPieceRepository.save(insightPiece);
  }

  @Transactional
  public void updateInsightPiece(String pieceId, PieceUpdateRequest pieceUpdateRequest, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    InsightPiece insightPiece = insightPieceRepository.findById(Long.parseLong(pieceId))
        .orElseThrow(() -> new InsightNotFoundException(ErrorCode.INSIGHT_NOT_FOUND));

    insightPiece.updateContent(pieceUpdateRequest.getContent());
  }

  @Transactional
  public void deleteInsightPiece(String pieceId, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    InsightPiece insightPiece = insightPieceRepository.findById(Long.parseLong(pieceId))
        .orElseThrow(() -> new InsightNotFoundException(ErrorCode.INSIGHT_NOT_FOUND));

    insightPieceRepository.delete(insightPiece);
  }

  @Transactional
  public InsightListResponse getInsights(int page, int size, InsightSortType sort, Long tag, String uuid) {

    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );


    List<Insight> insights = new ArrayList<>();
    int totalPages;
    int totalElements;

    Pageable pageable = PageRequest.of(page - 1, size,
        sort == InsightSortType.LATEST
    ? Sort.by(Sort.Direction.DESC, "createdAt")
        : Sort.by(Sort.Direction.DESC, "view"));

    if(tag == null) {

      insights = insightRepository.findAllByUserIdWithPiecesAndTags(user.getId(), pageable);
      totalElements = insightRepository.countByUserId(user.getId());
      totalPages = (int) Math.ceil((double) totalElements / size);

    } else {

      // 해당 태그가 존재하는지 확인
      tagEntityRepository.findById(tag).orElseThrow(
          () -> new TagNotFoundException(ErrorCode.TAG_NOT_FOUND)
      );

      insights = insightRepository.findAllByUserIdAndTagIdWithPiecesAndTags(user.getId(), tag, pageable);
      totalElements = insightRepository.countByUserIdAndTagId(user.getId(), tag);
      totalPages = (int) Math.ceil((double) totalElements / size);
    }


    // 조회하려는 페이지 번호가 총 페이지 수보다 큰 경우 예외 처리
    if(totalPages < page) {
      throw new InvalidInputValueException(ErrorCode.INVALID_INPUT_VALUE);
    }

    return InsightListResponse.of(insights, page, size, totalElements, totalPages);
  }

  @Transactional
  public InsightCandidateReGenResponse reGenerateInsightCandidates(Long insightId, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    try {
      // 첫 생각에 해당하는 인사이트 조각 조회
      InsightPiece insightPiece = insightPieceRepository.findByInsightIdAndType(insightId, InsightGenerationType.INIT)
          .orElseThrow(() -> new InsightNotFoundException(ErrorCode.INSIGHT_NOT_FOUND));

      // 해당 인사이트 조각에 대한 최신 인사이트 후보 3개 조회 (이전 버전과 다르게 생성하기 위함)
      List<InsightCandidate> latestCandidates = insightCandidateRepository.findTop3ByInsightPieceIdOrderByCreatedAtDesc(insightPiece.getId());

      // 첫 생각을 기반으로 AI가 새로운 인사이트 후보 3개 생성
      String aiResponse = claudeAiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_INSIGHT_CANDIDATE_PROMPT(insight.getInitThought(), latestCandidates));
      AiInsightCandidateResponse aiCandidateResponse = objectMapper.readValue(aiResponse, AiInsightCandidateResponse.class);

      // 해당 인사이트 조각에 대한 기존 후보들의 버전 중 최댓값 조회
      Long maxVersion = insightCandidateRepository.findMaxVersionByInsightPieceId(insightPiece.getId());

      if(maxVersion == null) {
        maxVersion = 0L;
      }

      List<InsightCandidate> insightCandidates = InsightCandidate.from(aiCandidateResponse, insightPiece, maxVersion + 1);

      insightCandidateRepository.saveAll(insightCandidates);

      return InsightCandidateReGenResponse.of(insight, insightCandidates);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Transactional
  public void moveInsightToTrash(Long insightId, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    insight.moveToTrash();
  }

  @Transactional
  public void restoreInsightFromTrash(Long insightId, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    insight.restoreFromTrash();
  }

  @Transactional
  public void updateInsightTitle(Long insightId, TitleUpdateRequest titleUpdateRequest, String uuid) {
    User user = userRepository.findByUserUuid(uuid).orElseThrow(
        () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
    );

    // 인사이트 존재 여부 및 조회 권한 검증
    Insight insight = insightValidator.findInsightAndValidate(insightId, uuid);

    insight.changeTitle(titleUpdateRequest.getTitle());
  }
}

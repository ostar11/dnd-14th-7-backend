package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.question.service.QuestionService;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import com.dnd.ahaive.domain.tag.entity.InsightTag;
import com.dnd.ahaive.domain.tag.entity.TagEntity;
import com.dnd.ahaive.domain.tag.repository.InsightTagRepository;
import com.dnd.ahaive.domain.tag.repository.TagEntityRepository;
import com.dnd.ahaive.domain.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightCreationService {

    private final InsightPieceService insightPieceService;
    private final QuestionService questionService;
    private final InsightRepository insightRepository;
    private final InsightTagRepository insightTagRepository;
    private final TagEntityRepository tagEntityRepository;

    @Transactional
    public Long save(String initThought, User user, AiInsightResponse aiInsightResponse) {
        // 인사이트 저장
        String title = aiInsightResponse.title();
        Insight insight = Insight.from(initThought, title, user);
        insightRepository.save(insight);

        // 인사이트 조각 저장
        String insightPieceContent = aiInsightResponse.insightPieceContent();
        insightPieceService.saveInsightPieces(insight, insightPieceContent);

        // 태그 저장 및 인사이트-태그 연결
        AiTagResponse aiTagResponse = aiInsightResponse.aiTagResponse();
        saveInsightTags(insight, aiTagResponse.getTags(), user);

        // 질문 저장
        AiQuestionResponse aiQuestionResponse = aiInsightResponse.aiQuestionResponse();
        questionService.saveQuestions(aiQuestionResponse, insight);

        return insight.getId();
    }

    /**
     * 태그 이름들을 받아 저장하고 인사이트와 연결합니다.
     * 같은 이름의 태그가 이미 있다면 재사용하고, 새로운 태그만 저장합니다.
     * @param insight 연결할 인사이트 객체
     * @param tagNames 태그 이름 리스트
     * @param user 태그를 저장할 사용자 객체
     */
    private void saveInsightTags(Insight insight, List<String> tagNames, User user) {

        // 기존 유저 태그 조회
        Map<String, TagEntity> existingTagMap = tagEntityRepository.findAllByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(TagEntity::getTagName, tag -> tag));

        List<String> newTagNames = tagNames.stream()
                .filter(tagName -> !existingTagMap.containsKey(tagName))
                .toList();

        List<String> duplicatedTagNames = tagNames.stream()
                .filter(existingTagMap::containsKey)
                .toList();

        // 새로운 태그 저장
        List<TagEntity> newTagEntities = newTagNames.stream()
                .map(tagName -> TagEntity.of(user, tagName))
                .toList();
        tagEntityRepository.saveAll(newTagEntities);

        // 인사이트-태그 생성 (새로운 태그 + 기존 중복 태그)
        List<InsightTag> insightTags = new ArrayList<>();

        newTagEntities.stream()
                .map(tagEntity -> InsightTag.of(tagEntity, insight))
                .forEach(insightTags::add);

        duplicatedTagNames.stream()
                .map(tagName -> InsightTag.of(existingTagMap.get(tagName), insight))
                .forEach(insightTags::add);

        insightTagRepository.saveAll(insightTags);
    }

}

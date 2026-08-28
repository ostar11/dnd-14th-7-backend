package com.dnd.ahaive.domain.insight.document;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Document(indexName = "insights", createIndex = true)
@Setting(settingPath = "elasticsearch/insight-setting.json")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Keyword)
    private String userUuid;

    @Field(type = FieldType.Boolean)
    private boolean trash;

    @Field(type = FieldType.Text, analyzer = "custom-analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "custom-analyzer")
    private String initThought;

    @Field(type = FieldType.Text, analyzer = "custom-analyzer")
    private String firstInsightPiece;

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis})
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis})
    private LocalDateTime updatedAt;

    public static InsightDocument from(Insight insight, InsightPiece insightPiece) {
        return InsightDocument.builder()
                .id(insight.getId())
                .userUuid(insight.getUser().getUserUuid())
                .trash(insight.isTrash())
                .title(insight.getTitle())
                .initThought(insight.getInitThought())
                .firstInsightPiece(insightPiece.getContent())
                .createdAt(insight.getCreatedAt())
                .updatedAt(insight.getUpdatedAt())
                .build();
    }

    @Builder
    protected InsightDocument(Long id, String userUuid, boolean trash, String title, String initThought,
                           String firstInsightPiece, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userUuid = userUuid;
        this.trash = trash;
        this.title = title;
        this.initThought = initThought;
        this.firstInsightPiece = firstInsightPiece;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

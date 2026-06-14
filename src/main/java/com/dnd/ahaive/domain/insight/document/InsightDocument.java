package com.dnd.ahaive.domain.insight.document;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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


}

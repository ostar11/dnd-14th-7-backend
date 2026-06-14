package com.dnd.ahaive.domain.tag.document;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Document(indexName = "tags", createIndex = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Keyword)
    private String userUuid;

    @Field(type = FieldType.Text, analyzer = "custom-analyzer")
    private String tagName;

    @Field(type = FieldType.Long)
    private Long insightCount;


}

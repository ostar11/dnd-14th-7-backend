package com.dnd.ahaive.domain.search.controller;

import com.dnd.ahaive.domain.search.controller.dto.SearchResultDto;
import com.dnd.ahaive.domain.search.service.JpaSearchService;
import com.dnd.ahaive.domain.search.service.PgSearchService;
import com.dnd.ahaive.domain.search.service.SearchService;
import com.dnd.ahaive.global.common.response.ResponseDTO;
import com.dnd.ahaive.global.security.core.CustomUserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService elasticSearchService;
    private final JpaSearchService jpaSearchService;
    private final PgSearchService pgSearchService;

    public SearchController(@Qualifier("elasticSearchService") SearchService elasticSearchService,
                            JpaSearchService jpaSearchService,
                            PgSearchService pgSearchService) {
        this.elasticSearchService = elasticSearchService;
        this.jpaSearchService = jpaSearchService;
        this.pgSearchService = pgSearchService;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/v1/search")
    public ResponseDTO<?> search(@RequestParam(name = "searchTerm") String searchTerm,
                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        SearchResultDto searchResult = elasticSearchService.search(customUserDetails.getUuid(), searchTerm);
        return ResponseDTO.of(searchResult, "success");
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/v1/search/jpa")
    public ResponseDTO<?> searchJpa(@RequestParam(name = "searchTerm") String searchTerm,
                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        SearchResultDto searchResult = jpaSearchService.search(customUserDetails.getUuid(), searchTerm);
        return ResponseDTO.of(searchResult, "success");
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/v1/search/pg")
    public ResponseDTO<?> searchPg(@RequestParam(name = "searchTerm") String searchTerm,
                                   @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        SearchResultDto searchResult = pgSearchService.search(customUserDetails.getUuid(), searchTerm);
        return ResponseDTO.of(searchResult, "success");
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/v1/search/pg-bigm")
    public ResponseDTO<?> searchPgBigm(@RequestParam(name = "searchTerm") String searchTerm,
                                       @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        SearchResultDto searchResult = pgSearchService.searchBigm(customUserDetails.getUuid(), searchTerm);
        return ResponseDTO.of(searchResult, "success");
    }

}

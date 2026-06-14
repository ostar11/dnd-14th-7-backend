package com.dnd.ahaive.domain.search.service;

import com.dnd.ahaive.domain.search.controller.dto.SearchResultDto;

public interface SearchService {
    SearchResultDto search(String uuid, String searchTerm);
}

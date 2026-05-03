package com.bytedance.contract.dto;

import java.util.List;

public record InputSuggestions(
        List<String> signatoryCompanies,
        List<String> counterpartyCompanies,
        List<String> projectNames,
        List<String> signedAreas
) {
}

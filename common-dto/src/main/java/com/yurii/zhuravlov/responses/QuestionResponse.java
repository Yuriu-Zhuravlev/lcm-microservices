package com.yurii.zhuravlov.responses;

import lombok.Builder;

import java.util.Map;

@Builder
public record QuestionResponse(Long id, String text, Map<Character, OptionResponse> options) {
}

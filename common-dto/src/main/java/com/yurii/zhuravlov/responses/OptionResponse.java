package com.yurii.zhuravlov.responses;

import lombok.Builder;

@Builder
public record OptionResponse(String text, boolean isCorrect) {
}

package com.yurii.zhuravlov.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

public record OptionResponse(
        String text,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean isCorrect
) {}

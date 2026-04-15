package com.yurii.zhuravlov.responses;

import lombok.Builder;

@Builder
public record LessonResponseShort(Long id, String title, int orderIndex) {
}

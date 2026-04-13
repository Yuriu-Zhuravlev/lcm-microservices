package com.yurii.zhuravlov.courseservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses", schema = "courses_schema")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long authorId;
}
package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.exceptions.AlreadyEnrolledException;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.EnrollmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrolmentRepository enrolmentRepository;
    private final CourseServiceClient courseServiceClient;

    @Transactional
    public EnrollmentResponse enrollUser(Long userId, Long courseId){
        if (enrolmentRepository.existsByUserIdAndCourseId(userId, courseId)){
            throw new AlreadyEnrolledException();
        }
        CourseResponseShort courseResponseShort = courseServiceClient.getCourseShortById(courseId);
        Enrolment enrolment = Enrolment.builder()
                .userId(userId)
                .courseId(courseId)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build();

        enrolment = enrolmentRepository.save(enrolment);

        return EnrollmentResponse.builder()
                .id(enrolment.getId())
                .course(courseResponseShort)
                .enrollmentStatus(enrolment.getStatus().toString())
                .enrolledAt(enrolment.getEnrolledAt())
                .build();
    }
}

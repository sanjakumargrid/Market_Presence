package com.griddynamics.forge.market_presence_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "application_question_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private CandidateApplication application;

    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;
}

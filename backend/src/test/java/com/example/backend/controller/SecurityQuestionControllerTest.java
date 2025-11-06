package com.example.backend.controller;

import com.example.backend.model.SecurityQuestion;
import com.example.backend.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityQuestionControllerTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private SecurityQuestionController controller;

    @BeforeEach
    void setUp() {
        controller = new SecurityQuestionController(questionRepository);
    }

    @Test
    void getAllSecurityQuestionsReturnsMappedDtos() {
        SecurityQuestion q = new SecurityQuestion();
        q.setId(4);
        q.setQuestionText("Favourite color?");

        when(questionRepository.findAll()).thenReturn(List.of(q));

        var result = controller.getAllSecurityQuestions();

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(4);
                    assertThat(dto.getQuestionText()).isEqualTo("Favourite color?");
                });
    }
}

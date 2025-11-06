package com.example.backend.config;

import com.example.backend.model.SecurityQuestion;
import com.example.backend.repository.QuestionRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SecurityQuestionSeederTest {

    @Test
    void seeds_whenEmpty() throws Exception {
        QuestionRepository repo = mock(QuestionRepository.class);
        when(repo.count()).thenReturn(0L);

        SecurityQuestionSeeder seeder = new SecurityQuestionSeeder();
        seeder.initSecurityQuestions(repo).run();

        verify(repo, atLeast(5)).save(any(SecurityQuestion.class));
    }

    @Test
    void skip_whenNotEmpty() throws Exception {
        QuestionRepository repo = mock(QuestionRepository.class);
        when(repo.count()).thenReturn(3L);

        SecurityQuestionSeeder seeder = new SecurityQuestionSeeder();
        seeder.initSecurityQuestions(repo).run();

        verify(repo, never()).save(any(SecurityQuestion.class));
    }
}

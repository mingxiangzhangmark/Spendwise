package com.example.backend.config;

import com.example.backend.model.SecurityQuestion;
import com.example.backend.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SecurityQuestionSeeder {

    @Bean
    CommandLineRunner initSecurityQuestions(QuestionRepository questionRepository) {
        return args -> {
            if (questionRepository.count() == 0) {
                SecurityQuestion question1 = new SecurityQuestion();
                question1.setQuestionText("What was the name of your first pet?");
                questionRepository.save(question1);

                SecurityQuestion question2 = new SecurityQuestion();
                question2.setQuestionText("What is your mother's maiden name?");
                questionRepository.save(question2);
                SecurityQuestion question3 = new SecurityQuestion();
                question3.setQuestionText("What was the name of your elementary school?"); 
                questionRepository.save(question3);
                SecurityQuestion question4 = new SecurityQuestion();
                question4.setQuestionText("What city were you born in?");
                questionRepository.save(question4);
                SecurityQuestion question5 = new SecurityQuestion();
                question5.setQuestionText("What was your childhood nickname?");
                questionRepository.save(question5);

                System.out.println("Security questions seeded successfully.");
            }
        };
    }
}

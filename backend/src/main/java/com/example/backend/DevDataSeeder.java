package com.example.backend;

import com.example.backend.model.Category;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DevDataSeeder {

    @Bean
    CommandLineRunner initData(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ExpenseRecordRepository expenseRecordRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // --- Initialization Category ---
            if (categoryRepository.count() == 0) {
                Category food = new Category(null, "Food", "All food and dining expenses", null, null);
                Category transport = new Category(null, "Transport", "Bus, train, taxi etc.", null, null);
                Category entertainment = new Category(null, "Entertainment", "Movies, games, events", null, null);
                Category shopping = new Category(null, "Shopping", "Clothes, electronics, goods", null, null);
                Category utilities = new Category(null, "Utilities", "Electricity, water, internet", null, null);

                categoryRepository.save(food);
                categoryRepository.save(transport);
                categoryRepository.save(entertainment);
                categoryRepository.save(shopping);
                categoryRepository.save(utilities);

                System.out.println("Categories initialized");
            }

            // --- Initialize bob User ---
            User bob = userRepository.findByUsernameOrEmail("bob", "bob@example.com");
            if (bob == null) {
                bob = new User();
                bob.setUsername("bob");
                bob.setEmail("bob@example.com");
                bob.setPassword_hash(passwordEncoder.encode("abc12345"));
                userRepository.save(bob);
                System.out.println("User bob initialized");
            }

            // --- Initialize ExpenseRecord for bob ---
            if (expenseRecordRepository.count() == 0) {
                Category food = categoryRepository.findBycategoryName("Food");
                Category transport = categoryRepository.findBycategoryName("Transport");

                ExpenseRecord record1 = new ExpenseRecord();
                record1.setUser(bob);
                record1.setCategory(food);
                record1.setAmount(new BigDecimal("12.50"));
                record1.setCurrency("USD");
                record1.setExpenseDate(LocalDate.now());
                record1.setDescription("Lunch at cafe");
                record1.setIsRecurring(false);
                expenseRecordRepository.save(record1);

                ExpenseRecord record2 = new ExpenseRecord();
                record2.setUser(bob);
                record2.setCategory(transport);
                record2.setAmount(new BigDecimal("3.20"));
                record2.setCurrency("USD");
                record2.setExpenseDate(LocalDate.now());
                record2.setDescription("Bus ticket");
                record2.setIsRecurring(false);
                expenseRecordRepository.save(record2);

                System.out.println("Expense records for bob initialized");
            }
        };
    }
}

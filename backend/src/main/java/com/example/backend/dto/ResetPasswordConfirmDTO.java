package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordConfirmDTO {
    private String identifier;   // username or email
    private Integer questionId;  // security question ID
    private String answer;       // user's answer
    private String newPassword;  // new password
}

package com.example.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyUserDTO {
    @Getter @Setter
    private Integer id;
    @Getter @Setter
    private String username;
    @Getter @Setter
    private String email;
    @Getter @Setter
    private String phoneNumber;
    @Getter @Setter
    private String profilePictureUrl;
    @Getter @Setter
    private LocalDateTime updatedAt;
}
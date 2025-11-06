package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;
    private String title;
    private String description;
    private String icon;

    public Achievement(String code, String title, String description, String icon) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.icon = icon;
    }
}
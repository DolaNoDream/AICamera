package com.example.aicamerabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 姿势推荐DTO
/*
{
  "id": "p01",
  "name": "自然站姿",
  "priority": 1,
  "tips": ["双脚与肩同宽", "身体微前倾"]
}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoseSuggestion {
    private String id;
    private String name;
    private int priority;      // 1 最推荐
    private String[] tips;     // 简短提示
}

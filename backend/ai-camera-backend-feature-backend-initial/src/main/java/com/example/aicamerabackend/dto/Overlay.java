package com.example.aicamerabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 叠加信息DTO（视觉提示）
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Overlay {
    private String textHint;      // 叠加文字提示
    private String hintImageUrl;  // 示意图链接（先 mock）
}

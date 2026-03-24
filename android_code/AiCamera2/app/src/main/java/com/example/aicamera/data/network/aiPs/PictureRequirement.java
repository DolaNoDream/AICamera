package com.example.aicamera.data.network.aiPs;

public class PictureRequirement {
    private String filter;    // 滤镜（如：复古）
    private String portrait;  // 人像（如：瘦脸）
    private String background;// 背景（如：去人群）
    private String special;   // 特殊要求（如：衣服穿上西装）

    // 空构造器（Gson解析必需）
    public PictureRequirement() {}

    // 全参构造器（可选）
    public PictureRequirement(String filter, String portrait, String background, String special) {
        this.filter = filter;
        this.portrait = portrait;
        this.background = background;
        this.special = special;
    }

    // Getter & Setter
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public String getPortrait() { return portrait; }
    public void setPortrait(String portrait) { this.portrait = portrait; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
    public String getSpecial() { return special; }
    public void setSpecial(String special) { this.special = special; }
}

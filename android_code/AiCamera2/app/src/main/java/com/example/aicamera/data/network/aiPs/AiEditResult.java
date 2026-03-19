package com.example.aicamera.data.network.aiPs;

/**
 * AI修图最终结果封装类
 * 主程序仅需关注此类的字段
 */
public class AiEditResult {
    private boolean success;       // 是否成功
    private String imageSavePath;  // 图片保存的本地绝对路径（成功时非空）
    private String errorMsg;       // 错误信息（失败时非空）

    // 空构造
    public AiEditResult() {}

    // 成功构造器
    public static AiEditResult success(String imageSavePath) {
        AiEditResult result = new AiEditResult();
        result.success = true;
        result.imageSavePath = imageSavePath;
        return result;
    }

    // 失败构造器
    public static AiEditResult failure(String errorMsg) {
        AiEditResult result = new AiEditResult();
        result.success = false;
        result.errorMsg = errorMsg;
        return result;
    }

    // Getter & Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getImageSavePath() { return imageSavePath; }
    public void setImageSavePath(String imageSavePath) { this.imageSavePath = imageSavePath; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}

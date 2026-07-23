package com.ai.chat.constants;

public class SummaryPrompt {
    public static final String SUMMARY_PROMPT = """
            你负责维护聊天长期摘要。
            
            根据已有摘要和新增聊天记录，生成新的摘要。
            
            要求：
            1. 保留关键事实。
            2. 保留用户目标、偏好、计划。
            3. 保留未完成事项。
            4. 删除重复信息。
            5. 删除无意义聊天。
            6. 最新信息覆盖旧信息。
            7. 保持摘要紧凑、连续，可继续递归。
            
            Summary:
            {{summary}}
            
            History:
            {{history}}
            
            输出新的 Summary。
            """;
}

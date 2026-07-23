package com.ai.chat.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class ToolRegistry {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Function<JsonNode,String>> executors = new ConcurrentHashMap<>();
    @Getter
    private final List<ChatCompletionTool> tools = new ArrayList<>();

    public ToolRegistry() {
        registerGetWeather(); // 注册工具
        // ...
    }

    // 具体执行
    public String execute(String name,String argumentsJson) {
        Function<JsonNode,String> executor = executors.get(name);
        if (executor == null) return "位置工具: "+name;

        try {
            JsonNode  args = (argumentsJson == null || argumentsJson.isEmpty()) ?
                    objectMapper.createObjectNode() :
                    objectMapper.readTree(argumentsJson);
            return executor.apply(args);
        } catch (Exception e) {
            log.error("解析工具参数失败: {}", argumentsJson, e);
            return "参数解析失败: " + e.getMessage();
        }
    }

    // 工具定义
    private void registerGetWeather() {
        String name = "getWeather";

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type",JsonValue.from("object"))
                .putAdditionalProperty("properties",JsonValue.from(Map.of(
                        "location",Map.of(
                                "type","string",
                                "description","城市名称，例如：北京"
                        )
                )))
                .putAdditionalProperty("required",JsonValue.from(List.of("location")))
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("查询指定城市天气")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> {
            String location = args.has("location") ? args.get("location").asText() : "未知";
            // ...
            return String.format("{\"location\":\"%s\",\"weather\":\"晴\",\"temperature\":\"26℃\"}", location);
        });
    }
}

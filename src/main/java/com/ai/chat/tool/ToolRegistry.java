package com.ai.chat.tool;

import com.ai.chat.service.PhoneService;
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

    private final PhoneService phoneService;

    public ToolRegistry(PhoneService phoneService) {
        this.phoneService = phoneService;
        registerGetWeather(); // 注册天气工具
        registerPhoneSkip(); // 手机滑动工具
        registerPhonePress(); // 点击工具
        registerPhoneScreenCut(); // 截屏(未拉取)
        registerPhonePower(); // 开关
        registerPhoneHome(); // 回到主页
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

    // 滑动工具定义
    private void registerPhoneSkip() {
        String name = "phoneSkip";

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type",JsonValue.from("object"))
                .putAdditionalProperty("properties",JsonValue.from(Map.of(
                        "x1",Map.of(
                                "type","string",
                                "description","初始x坐标"
                        ),
                        "y1",Map.of(
                                "type","string",
                                "description","初始y坐标"
                        ),
                        "x2",Map.of(
                                "type","string",
                                "description","目标x坐标"
                        ),
                        "y2",Map.of(
                                "type","string",
                                "description","目标y坐标"
                        )
                )))
                .putAdditionalProperty("required",JsonValue.from(List.of("x1","y1","x2","y2")))
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("滑动手机屏幕")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> {
            String x1 = args.has("x1") ? args.get("x1").asText() : "未知";
            String y1 = args.has("y1") ? args.get("y1").asText() : "未知";
            String x2 = args.has("x2") ? args.get("x2").asText() : "未知";
            String y2 = args.has("y2") ? args.get("y2").asText() : "未知";

            return phoneService.skip(x1,y1,x2,y2);
        });
    }

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

    private void registerPhonePress() {
        String name = "phonePress";

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type",JsonValue.from("object"))
                .putAdditionalProperty("properties",JsonValue.from(Map.of(
                        "x",Map.of(
                                "type","string",
                                "description","要点击的x坐标"
                        ),
                        "y",Map.of(
                                "type","string",
                                "description","要点击的y坐标"
                        )
                )))
                .putAdditionalProperty("required",JsonValue.from(List.of("x","y")))
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("点击手机屏幕")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> {
            String x = args.has("x") ? args.get("x").asText() : "未知";
            String y = args.has("y") ? args.get("y").asText() : "未知";
            // ...
            return phoneService.press(x,y);
        });
    }

    private void registerPhoneScreenCut() {
        String name = "phonePress";

        FunctionParameters parameters = FunctionParameters.builder()
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("手机截屏")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> phoneService.screenCut());
    }

    // 模拟电源键
    private void registerPhonePower() {
        String name = "phonePower";

        FunctionParameters parameters = FunctionParameters.builder()
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("手机息屏，或手机亮屏")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> phoneService.power());
    }

    // 模拟回到主页
    private void registerPhoneHome() {
        String name = "phoneHome";

        FunctionParameters parameters = FunctionParameters.builder()
                .build();

        tools.add(ChatCompletionTool.
                ofFunction(ChatCompletionFunctionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(name)
                                .description("手机回到主页")
                                .parameters(parameters)
                                .build())
                        .build()
                )
        );

        executors.put(name,args -> phoneService.home());
    }
}

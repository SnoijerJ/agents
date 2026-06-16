package com.example.tools;

import com.openai.core.JsonValue;
import com.openai.models.responses.FunctionTool;

import java.util.*;

public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Tool get(String name) {
        Tool tool = tools.get(name);

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Unknown tool: " + name
            );
        }

        return tool;
    }

    public Collection<Tool> all() {
        return tools.values();
    }

    public com.openai.models.responses.Tool getToolDefinition(String name) {
        if (!tools.containsKey(name)) {
            throw new RuntimeException("Tool does not exist: " + name);
        }
        return getToolDefinition(tools.get(name));
    }

    public static com.openai.models.responses.Tool getToolDefinition(Tool tool) {

        List<String> required = new ArrayList<>();
        Map<String, Object> properties = new HashMap<>();

        for (Parameter param : tool.getParameters()) {

            properties.put(param.name(), Map.of(
                    "type", param.type(),
                    "description", param.description()
            ));

            required.add(param.name());
        }

        com.openai.models.responses.FunctionTool.Parameters.Builder paramBuilder = com.openai.models.responses.FunctionTool.Parameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties));

        if (!required.isEmpty()) {
            paramBuilder.putAdditionalProperty("required", JsonValue.from(required));
        }

        paramBuilder.putAdditionalProperty("additionalProperties", JsonValue.from(false));

        return com.openai.models.responses.Tool.ofFunction(
                FunctionTool.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .parameters(JsonValue.from(paramBuilder.build()))
                        .strict(true)
                        .build()
        );
    }
}

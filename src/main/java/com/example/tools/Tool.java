package com.example.tools;

import com.example.memory.Logger;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;

import java.util.List;
import java.util.Map;

public interface Tool {
    String getName();
    String getDescription();
    List<Parameter> getParameters();

    ResponseInputItem.FunctionCallOutput execute(ResponseFunctionToolCall toolCall, Logger logger);
}

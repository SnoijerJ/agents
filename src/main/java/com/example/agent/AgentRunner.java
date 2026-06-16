package com.example.agent;

import com.example.memory.Logger;
import com.example.tools.ToolRegistry;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AgentRunner {

    private final OpenAIClient client;
    private final ToolRegistry toolRegistry;
    private final Logger logger;

    public AgentRunner(OpenAIClient openAIClient, ToolRegistry toolRegistry, Logger logger) {
        this.client = openAIClient;
        this.toolRegistry = toolRegistry;
        this.logger = logger;
    }

    public void run(Agent agent, String userInput) throws IOException {

        List<Tool> agentTools = agent.tools().stream().map(t -> toolRegistry.getToolDefinition(t)).toList();

        ResponseCreateParams.Input input = ResponseCreateParams.Input.ofText(userInput);
        String previousResponseId = null;

        while (true) {

            // Ask OpenAI
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .input(input)
                    .instructions(agent.SystemPrompt() + "\n" + getContext())
                    .model(agent.model())
                    .previousResponseId(previousResponseId)
                    .tools(agentTools)
                    .build();

            logger.append(params.toString());

            Response response = client.responses().create(params);

            logger.append(response.toString());

            List<ResponseInputItem> toolResults = new ArrayList<>();

            for (ResponseOutputItem item : response.output()) {

                if (item.isFunctionCall()) {
                    ResponseFunctionToolCall toolCall = item.asFunctionCall();

                    ResponseInputItem.FunctionCallOutput result = toolRegistry.get(toolCall.name()).execute(toolCall, logger);

                    toolResults.add(ResponseInputItem.ofFunctionCallOutput(result));
                } else if (item.isMessage()) {
                    ResponseOutputMessage message = item.asMessage();
                    for (ResponseOutputMessage.Content content : message.content()) {
                        if (content.isOutputText()) {
                            System.out.println(content.asOutputText().text());

                            if (response.output().size() == 1) {
                                Scanner scanner = new Scanner(System.in);
                                toolResults.add(ResponseInputItem.ofMessage(ResponseInputItem.Message.builder()
                                        .content(List.of(ResponseInputContent.ofInputText(ResponseInputText.builder()
                                                .text(scanner.nextLine())
                                                .build())))
                                        .role(ResponseInputItem.Message.Role.USER)
                                        .build()));
                            }
                        }
                    }
                }
            }

            input = ResponseCreateParams.Input.ofResponse(toolResults);
            previousResponseId = response.id();
        }
    }

    private String getContext() {
        Path pwd = Path.of("./").toAbsolutePath();

        return String.format("<context>\nPWD: %s\n</context>", pwd);
    }
}

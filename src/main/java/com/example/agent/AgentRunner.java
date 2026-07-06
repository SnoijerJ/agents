package com.example.agent;

import com.example.memory.Logger;
import com.example.skills.Skill;
import com.example.tools.ToolRegistry;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        List<Tool> agentTools = agent.tools().stream().map(toolRegistry::getToolDefinition).toList();

        ResponseCreateParams.Input input = ResponseCreateParams.Input.ofText(parseUserCommand(userInput));
        String previousResponseId = null;

        while (true) {

            // Ask OpenAI
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .input(input)
                    .instructions(agent.SystemPrompt().strip() + "\n"
                            + getContext().strip())
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
                                                .text(parseUserCommand(scanner.nextLine()))
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

    private String getContext() throws IOException {
        Path pwd = Path.of("./").toAbsolutePath();
        List<Skill> skills = Skill.getSkills(Path.of(".ai/skills"));

        return "<context>\n" +
                String.format("<pwd>%s</pwd>\n", pwd) +
                Skill.getSkillsOverview(skills) +
                "</context>";
    }

    private String parseUserCommand(String input) {
        String userInput = input;
        while (true) {
            if (!userInput.strip().startsWith("/")) {
                return userInput;
            }
            try {
                String promptFileName = input.strip().split(" ")[0].replace("/", "");
                List<Path> promptFiles = new ArrayList<>(getPrompts(Path.of("./.ai/prompts")));
                promptFiles.addAll(getPrompts(Path.of("src/main/resources/prompts")));

                Optional<Path> prompt = promptFiles.stream()
                        .filter(f -> f.getFileName().toString().replace(".md", "").equals(promptFileName))
                        .findFirst();

                if (prompt.isPresent()) {
                    return Files.readString(prompt.get()) + "\n" + input.replace(promptFileName, "");
                } else {
                    System.out.println("[SYSTEM] Could not find the promptfile specified in `.ai/prompts`, provide new input");
                }
            } catch (IOException e) {
                System.out.println("[SYSTEM] Could not read prompts in `.ai/prompts`, provide new input");
            }
            Scanner scanner = new Scanner(System.in);
            userInput = scanner.nextLine();
        }
    }

    private static List<Path> getPrompts(Path promptFolder) throws IOException {
        if (!Files.exists(promptFolder)) {
            return new ArrayList<>();
        }

        return Files.list(promptFolder)
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".md"))
                .toList();
    }
}

package com.example;

import com.example.agent.Agent;
import com.example.agent.AgentRunner;
import com.example.memory.Logger;
import com.example.tools.*;
import com.example.tools.fileOperations.*;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static String getKey() throws IOException {
        Path keyFile = Path.of("src/main/resources/secrets/openai_api_key");
        if (Files.exists(keyFile)) {
            return Files.readString(keyFile);
        } else {
            String key = System.getenv().get("OPENAI_API_KEY");
            if (key == null) {
                throw new RuntimeException("No env var OPENAI_API_KEY set");
            }
            return key;
        }

    }

    public static void main(String[] args) throws Exception {

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(getKey())
                .build();

        Logger logger = new Logger();
        logger.createLogFile();

        ToolRegistry toolRegistry = new ToolRegistry();

        toolRegistry.register(new ReadFileTool());
        toolRegistry.register(new EditFileTool());
        toolRegistry.register(new FileStructureTool());
        toolRegistry.register(new CreateFileTool());
        toolRegistry.register(new DeleteFileTool());
        toolRegistry.register(new MoveFileTool());
        toolRegistry.register(new ShellTool());
        toolRegistry.register(new ExecuteTestsTool());

        AgentRunner runner = new AgentRunner(client, toolRegistry, logger);

        run(runner);
    }

    private static void run(AgentRunner runner) throws IOException, Agent.AgentParserException {
        List<Agent> agents = getAgents(Path.of("src/main/resources/agents"));
        agents.addAll(getAgents(Path.of(".ai/agents")));
        agents.add(new Agent("chat_agent", "gpt-5.4-mini", "", List.of()));
        StringBuilder question = new StringBuilder("Select agent:");
        for (int i = 0; i < agents.size(); i++) {
            question.append("\n").append(i).append(" ").append(agents.get(i).name());
        }
        Agent agent;
        while (true) {
            String response = getUserInput(question.toString());
            try {
                agent = agents.get(Integer.parseInt(response));
                break;
            } catch (Exception e) {
                System.out.println("Not a valid response, try again");
            }
        }
        String input = getUserInput("Provide input:");
        runner.run(agent, input);
    }

    private static String getUserInput(String question) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(question);
        return scanner.nextLine();
    }

    private static List<Agent> getAgents(Path agentFolder) throws IOException, Agent.AgentParserException {
        if (!Files.exists(agentFolder)) {
            return new ArrayList<>();
        }

        List<Path> files = Files.list(agentFolder)
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith("Agent.md"))
                .toList();

        List<Agent> agents = new ArrayList<>();
        for (Path file : files) {
            agents.add(Agent.fromFile(file));
        }
        return agents;
    }
}

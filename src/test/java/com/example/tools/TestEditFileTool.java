package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.models.responses.ResponseFunctionToolCall;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEditFileTool {
    Path TEST_DIR = Path.of("target", "test", "testEditFileTool");

    @Test
    public void testFileEdit() throws IOException {
        // Prepare
        Files.createDirectories(TEST_DIR);
        Path testFile = Path.of(TEST_DIR.toString(), "test1.txt");

        String content = """
                Hello sheeple
                This is very nice
                Ow an ugly line
                """;

        if(!Files.exists(testFile)) {
            Files.writeString(testFile, content, StandardOpenOption.CREATE);
        } else {
            Files.writeString(testFile, content, StandardOpenOption.TRUNCATE_EXISTING);
        }

        // Execute
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("diff",
                String.format("""
                --- a/%s
                +++ b/%s
                @@ -1,3 +1,3 @@
                 Hello sheeple
                 This is very nice
                -Ow an ugly line
                +Ah, a nice ending
                """, testFile, testFile)
        );

        String json = mapper.writeValueAsString(node);
        ResponseFunctionToolCall toolCall = Mockito.mock(ResponseFunctionToolCall.class);
        Mockito.when(toolCall.arguments()).thenReturn(json);
        Mockito.when(toolCall.callId()).thenReturn("Foo");
        Logger logger = Mockito.mock(Logger.class);
        new EditFileTool().execute(toolCall, logger);

        // Verify
        String result = Files.readString(testFile);
        assertEquals(content.replace("Ow an ugly line", "Ah, a nice ending"), result);
    }
}

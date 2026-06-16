package com.example.tools;

import com.example.memory.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFileStructureTool {
    Path TEST_DIR = Path.of("target", "test", "testFileStructureTool");

    @Test
    public void testExecute() throws IOException {
        // Prepare
        Path root = Path.of(TEST_DIR.toString(), "exampleRoot");
        Path rootFile = Path.of(root.toString(), "rootFile");
        Path rootDir = Path.of(root.toString(), "rootDir");
        Path rootDirDir = Path.of(rootDir.toString(), "rootDirDir");
        Path rootDirDirDir = Path.of(rootDirDir.toString(), "rootDirDirDir");
        Path rootDirDirDirFile = Path.of(rootDirDirDir.toString(), "rootDirDirDirFile");
        Path rootDir2 = Path.of(root.toString(), "rootDir2");
        Path rootDirDirFile = Path.of(rootDirDir.toString(), "rootDirDirFile");

        if (Files.exists(root)) {
            Files.walk(root)
                    .sorted(Comparator.reverseOrder()) // children before parent
                    .forEach(it -> {
                        try {
                            Files.deleteIfExists(it);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        Files.createDirectories(root);
        Files.createFile(rootFile);
        Files.createDirectory(rootDir);
        Files.createDirectory(rootDirDir);
        Files.createDirectory(rootDirDirDir);
        Files.createFile(rootDirDirDirFile);
        Files.createDirectory(rootDir2);
        Files.createFile(rootDirDirFile);

        // Execute
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("path", root.toString());
        String json = mapper.writeValueAsString(node);
        ResponseFunctionToolCall toolCall = Mockito.mock(ResponseFunctionToolCall.class);
        Mockito.when(toolCall.arguments()).thenReturn(json);
        Mockito.when(toolCall.callId()).thenReturn("Foo");
        Logger logger = Mockito.mock(Logger.class);
        ResponseInputItem.FunctionCallOutput output = new FileStructureTool().execute(toolCall, logger);

        // Verify
        String actual = output.output().asString();
        String expected = "{exampleRoot=[{rootDir2=[]}, {rootDir=[{rootDirDir=[{rootDirDirDir=[rootDirDirDirFile]}, rootDirDirFile]}]}, rootFile]}";
        assertEquals(expected, actual);
    }
}

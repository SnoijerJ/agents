package com.example.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.example.Utils.parseFile;

public record Skill (
    String name,
    String description,
    String skill,
    Path folder
    ) {

    public static String getSkillsOverview(List<Skill> skills) {
        StringBuilder result = new StringBuilder();
        result.append("<skills>\n");
        for (Skill skill : skills) {
            result.append("<skill>\n")
                    .append(String.format("<name>%s</name>\n", skill.name))
                    .append(String.format("<description>%s</description>\n", skill.description))
                    .append(String.format("<path>%s</path>\n", skill.folder))
                    .append("</skill>");
        }
        result.append("</skills>\n");
        return result.toString();
    }

    public static List<Skill> getSkills(Path folder) throws IOException {

        return Files.list(folder)
                .filter(Files::isDirectory)
                .filter(p -> Files.exists(Path.of(p.toString(), "SKILL.md")))
                .map(p -> {
                    try {
                        return parseSkill(Path.of(p.toString(), "SKILL.md"));
                    } catch (SkillException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    public static Skill parseSkill(Path skill) throws SkillException {
        try {
            Map<String, String> skillMap = parseFile(skill);
            if (!skillMap.containsKey("name")) {
                throw new SkillException("Skill must have a name: " + skill);
            }
            if (!skillMap.containsKey("description")) {
                throw new SkillException("Skill must have a description: " + skill);
            }
            if (!skillMap.containsKey("content") || skillMap.get("content").isBlank()) {
                throw new SkillException("Skill must contain content: " + skill);
            }
            return new Skill(skillMap.get("name"), skillMap.get("description"), skillMap.get("content"), skill.getParent());
        } catch (IOException e) {
            throw new SkillException(e.getMessage(), e);
        }
    }
}


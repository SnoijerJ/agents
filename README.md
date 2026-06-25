# Agents

## Defining an agent
Create a <name>Agent.md file in .ai/agents with the following content:
```markdown
---
name: <name>
model: <gpt_model>
tools: []
---
<agent prompt>
```

## Available tools
All tools are restricted to work only within the pwd.

| Name            | Purpose                                                                                   |
|-----------------|-------------------------------------------------------------------------------------------|
| edit_file       | For editing files                                                                         |
| create_file     | For creating files or folders                                                             |
| delete_file     | For deleting files or folders                                                             |
| file_structure  | For searching files or folders                                                            |
| read_file       | For reading files                                                                         |
| execute_shell   | Lets the AI execute commands (not restricted, but human validation before each execution) |
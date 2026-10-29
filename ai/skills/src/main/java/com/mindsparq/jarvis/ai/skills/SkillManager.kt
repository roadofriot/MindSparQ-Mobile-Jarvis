package com.mindsparq.jarvis.ai.skills

import java.io.File

data class SkillDefinition(
    val name: String,
    val description: String,
    val allowedTools: List<String>,
    val instructionBody: String,
    val directoryPath: String,
    val hasScripts: Boolean = false
)

object SkillManager {

    fun parseSkillFile(skillMdFile: File): SkillDefinition? {
        if (!skillMdFile.exists() || !skillMdFile.isFile) return null

        val content = skillMdFile.readText()
        if (!content.startsWith("---")) {
            return SkillDefinition(
                name = skillMdFile.parentFile.name,
                description = "Skill without frontmatter",
                allowedTools = emptyList(),
                instructionBody = content,
                directoryPath = skillMdFile.parentFile.absolutePath
            )
        }

        val parts = content.split("---", limit = 3)
        if (parts.size < 3) return null

        val frontmatter = parts[1]
        val body = parts[2].trim()

        var name = skillMdFile.parentFile.name
        var description = ""
        val tools = mutableListOf<String>()

        var inTools = false
        for (line in frontmatter.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("name:")) {
                name = trimmed.removePrefix("name:").trim()
                inTools = false
            } else if (trimmed.startsWith("description:")) {
                description = trimmed.removePrefix("description:").trim()
                inTools = false
            } else if (trimmed.startsWith("tools:")) {
                inTools = true
            } else if (inTools && trimmed.startsWith("- ")) {
                tools.add(trimmed.removePrefix("- ").trim())
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                inTools = false
            }
        }

        val scriptsDir = File(skillMdFile.parentFile, "scripts")
        val hasScripts = scriptsDir.exists() && scriptsDir.isDirectory && (scriptsDir.listFiles()?.isNotEmpty() == true)

        return SkillDefinition(
            name = name,
            description = description,
            allowedTools = tools,
            instructionBody = body,
            directoryPath = skillMdFile.parentFile.absolutePath,
            hasScripts = hasScripts
        )
    }

    fun loadAllSkills(baseSkillsDir: File): List<SkillDefinition> {
        if (!baseSkillsDir.exists() || !baseSkillsDir.isDirectory) return emptyList()

        val result = mutableListOf<SkillDefinition>()
        baseSkillsDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val skillMd = File(dir, "SKILL.md")
                if (skillMd.exists()) {
                    parseSkillFile(skillMd)?.let { result.add(it) }
                }
            }
        }
        return result
    }
}


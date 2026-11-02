package com.mindsparq.jarvis.ai.skills

import java.io.File

data class SkillDefinition(
    val name: String,
    val description: String,
    val allowedTools: List<String>,
    val instructionBody: String,
    val directoryPath: String,
    val hasScripts: Boolean = false,
    val version: String = "1.0.0",
    val author: String = "MindSparQ",
    val category: String = "engineering"
)

object SkillManager {

    fun parseSkillFile(skillMdFile: File): SkillDefinition? {
        if (!skillMdFile.exists() || !skillMdFile.isFile) return null

        val parent = skillMdFile.parentFile ?: File(".")
        val parentName = parent.name.ifBlank { "unknown" }
        val parentPath = parent.absolutePath

        val content = skillMdFile.readText()
        if (!content.startsWith("---")) {
            return SkillDefinition(
                name = parentName,
                description = "Skill without frontmatter",
                allowedTools = emptyList(),
                instructionBody = content,
                directoryPath = parentPath
            )
        }

        val parts = content.split("---", limit = 3)
        if (parts.size < 3) return null

        val frontmatter = parts[1]
        val body = parts[2].trim()

        var name = parentName
        var description = ""
        var version = "1.0.0"
        var author = "MindSparQ"
        var category = "engineering"
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
            } else if (trimmed.startsWith("version:")) {
                version = trimmed.removePrefix("version:").trim()
                inTools = false
            } else if (trimmed.startsWith("author:")) {
                author = trimmed.removePrefix("author:").trim()
                inTools = false
            } else if (trimmed.startsWith("category:")) {
                category = trimmed.removePrefix("category:").trim()
                inTools = false
            } else if (trimmed.startsWith("tools:") || trimmed.startsWith("allowed-tools:")) {
                inTools = true
            } else if (inTools && trimmed.startsWith("- ")) {
                tools.add(trimmed.removePrefix("- ").trim())
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                inTools = false
            }
        }

        val scriptsDir = File(parent, "scripts")
        val hasScripts = scriptsDir.exists() && scriptsDir.isDirectory && (scriptsDir.listFiles()?.isNotEmpty() == true)

        return SkillDefinition(
            name = name,
            description = description,
            allowedTools = tools,
            instructionBody = body,
            directoryPath = parentPath,
            hasScripts = hasScripts,
            version = version,
            author = author,
            category = category
        )
    }

    /**
     * Multi-directory skill discovery:
     * Discovers skills across workspace, user directory (~/.jarvis/skills), and bundled assets.
     * Recursively traverses subdirectories up to depth 3 with deduplication by skill name.
     */
    fun discoverSkills(vararg roots: File): List<SkillDefinition> {
        val discovered = mutableMapOf<String, SkillDefinition>()
        for (root in roots) {
            if (!root.exists() || !root.isDirectory) continue
            root.walkTopDown().maxDepth(3).forEach { file ->
                if (file.isFile && file.name.equals("SKILL.md", ignoreCase = true)) {
                    parseSkillFile(file)?.let { def ->
                        if (!discovered.containsKey(def.name)) {
                            discovered[def.name] = def
                        }
                    }
                }
            }
        }
        return discovered.values.toList().sortedBy { it.name }
    }

    fun loadAllSkills(baseSkillsDir: File): List<SkillDefinition> =
        discoverSkills(baseSkillsDir)

    fun filterSkillsForTask(skills: List<SkillDefinition>, taskClass: String): List<SkillDefinition> {
        val tc = taskClass.lowercase().trim()
        if (tc.isEmpty() || tc == "all" || tc == "general") return skills
        return skills.filter { s ->
            val name = s.name.lowercase()
            val desc = s.description.lowercase()
            when (tc) {
                "coding", "implementation" -> name.contains("code") || name.contains("implementation") || name.contains("healer") || name.contains("refactor")
                "testing", "qa" -> name.contains("test") || name.contains("debug") || name.contains("verification")
                "architecture" -> name.contains("architect") || name.contains("audit") || name.contains("decision")
                "security" -> name.contains("security") || name.contains("threat") || name.contains("vulnerability")
                "design", "ui" -> name.contains("design") || name.contains("taste") || name.contains("polish")
                "performance" -> name.contains("performance") || name.contains("lighthouse") || name.contains("optimization")
                else -> name.contains(tc) || desc.contains(tc)
            }
        }
    }
}


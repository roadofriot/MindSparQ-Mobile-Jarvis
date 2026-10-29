package com.mindsparq.jarvis.ai.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SkillManagerTest {

    @Test
    fun testParseSkillFile() {
        val skillFile = File("../../skills/project-discovery/SKILL.md")
        val target = if (skillFile.exists()) skillFile else File("skills/project-discovery/SKILL.md")
        
        if (target.exists()) {
            val def = SkillManager.parseSkillFile(target)
            assertNotNull(def)
            assertEquals("project-discovery", def!!.name)
            assertTrue(def.allowedTools.contains("list_dir"))
            assertTrue(def.instructionBody.contains("Project Discovery Skill"))
        } else {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "test_skill_dir").apply { mkdirs() }
            val tempSkill = File(tempDir, "SKILL.md").apply {
                writeText("""
                    ---
                    name: test-skill
                    description: A skill for testing
                    tools:
                      - read_file
                      - write_file
                    ---
                    # Instruction Body
                """.trimIndent())
            }
            val def = SkillManager.parseSkillFile(tempSkill)
            assertNotNull(def)
            assertEquals("test-skill", def!!.name)
            assertEquals(2, def.allowedTools.size)
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDiscoverAll17Skills() {
        val rootSkills = File("../../skills")
        val target = if (rootSkills.exists()) rootSkills else File("skills")

        if (target.exists()) {
            val skills = SkillManager.loadAllSkills(target)
            assertTrue("Expected at least 17 skills, found ${skills.size}", skills.size >= 17)
            val names = skills.map { it.name }.toSet()
            assertTrue(names.contains("taste-design-polish"))
            assertTrue(names.contains("autonomous-code-healer"))
            assertTrue(names.contains("architecture-diagrammer"))
            assertTrue(names.contains("security-vulnerability-scanner"))
            assertTrue(names.contains("performance-lighthouse-audit"))
            assertTrue(names.contains("multi-provider-mcp-hub"))
            assertTrue(names.contains("multi-agent-orchestrator"))
        }
    }
}

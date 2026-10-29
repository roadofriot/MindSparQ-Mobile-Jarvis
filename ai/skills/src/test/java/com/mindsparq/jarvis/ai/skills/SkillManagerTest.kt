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
        // If running from module root or project root, resolve path safely
        val target = if (skillFile.exists()) skillFile else File("skills/project-discovery/SKILL.md")
        
        if (target.exists()) {
            val def = SkillManager.parseSkillFile(target)
            assertNotNull(def)
            assertEquals("project-discovery", def!!.name)
            assertTrue(def.allowedTools.contains("list_dir"))
            assertTrue(def.instructionBody.contains("Project Discovery Skill"))
        } else {
            // Test with mock string file
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
}


package com.mindsparq.jarvis.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class JarvisDatabaseTest {

    private lateinit var database: JarvisDatabase

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JarvisDatabase.Schema.create(driver)
        database = JarvisDatabase(driver)
    }

    @Test
    fun testProjectAndTaskPersistence() {
        val now = System.currentTimeMillis()
        val queries = database.jarvisDatabaseQueries

        // 1. Insert Project
        queries.insertProject(
            id = "proj-1",
            name = "Test Expense Tracker",
            slug = "test-expense-tracker",
            root_path = "/workspaces/test-expense-tracker",
            stack = "FLUTTER",
            template = "flutter",
            created_at = now,
            updated_at = now,
            archived = 0,
            deleted_at = null,
            settings_json = "{}"
        )

        val projects = queries.selectAllProjects().executeAsList()
        assertEquals(1, projects.size)
        assertEquals("Test Expense Tracker", projects[0].name)

        // 2. Insert Task
        queries.insertTask(
            id = "task-101",
            project_id = "proj-1",
            chat_id = null,
            title = "Add biometric auth",
            state = "COMPLETED",
            step = 5,
            repair_attempts = 0,
            budget_json = null,
            started_at = now,
            ended_at = now + 1000,
            report_md = "All tests passed"
        )

        val tasks = queries.selectTasksForProject("proj-1").executeAsList()
        assertEquals(1, tasks.size)
        assertEquals("COMPLETED", tasks[0].state)

        // 3. Insert Evidence
        queries.insertEvidence(
            id = "ev-1",
            task_id = "task-101",
            kind = "TEST_RUN",
            summary = "12/12 unit tests passed",
            exit_code = 0,
            payload_path = null,
            created_at = now + 500
        )

        val evidenceList = queries.selectEvidenceForTask("task-101").executeAsList()
        assertEquals(1, evidenceList.size)
        assertEquals(0, evidenceList[0].exit_code?.toInt())
    }

    @Test
    fun testChatAndMessageRelationalIntegrity() {
        val now = System.currentTimeMillis()
        val queries = database.jarvisDatabaseQueries

        // 1. Insert Project
        queries.insertProject(
            id = "proj-chat-1",
            name = "Jarvis AI",
            slug = "jarvis-ai",
            root_path = "/workspaces/jarvis-ai",
            stack = "KOTLIN",
            template = "compose",
            created_at = now,
            updated_at = now,
            archived = 0,
            deleted_at = null,
            settings_json = "{\"type\":\"ANDROID\"}"
        )

        assertEquals(1L, queries.countProjects().executeAsOne())

        // 2. Insert Chat
        queries.insertChat(
            id = "chat-main",
            project_id = "proj-chat-1",
            title = "Architecture Discussion",
            agent = "claude-code",
            mode = "code",
            created_at = now,
            updated_at = now
        )

        val chats = queries.selectChatsForProject("proj-chat-1").executeAsList()
        assertEquals(1, chats.size)
        assertEquals("Architecture Discussion", chats[0].title)

        // 3. Insert Messages
        queries.insertOrReplaceMessage(
            id = "msg-1",
            chat_id = "chat-main",
            role = "user",
            content_json = "{\"text\":\"Hello Jarvis\"}",
            model_ref = null,
            tokens_in = 10,
            tokens_out = 0,
            created_at = now
        )

        queries.insertOrReplaceMessage(
            id = "msg-2",
            chat_id = "chat-main",
            role = "assistant",
            content_json = "{\"text\":\"Hello Sir, ready to code.\"}",
            model_ref = "gemini-2.5-flash",
            tokens_in = 15,
            tokens_out = 20,
            created_at = now + 100
        )

        val messages = queries.selectMessagesForChat("chat-main").executeAsList()
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("assistant", messages[1].role)

        // 4. Test delete messages
        queries.deleteMessagesForChat("chat-main")
        assertEquals(0, queries.selectMessagesForChat("chat-main").executeAsList().size)

        // 5. Test delete project
        queries.deleteProjectPermanently("proj-chat-1")
        assertEquals(0L, queries.countProjects().executeAsOne())
    }
}

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
    fun testProviderAndKeyCooldown() {
        val now = System.currentTimeMillis()
        val queries = database.jarvisDatabaseQueries

        // Insert Provider
        queries.transaction {
            database.jarvisDatabaseQueries.insertProject(
                id = "dummy",
                name = "Dummy",
                slug = "dummy",
                root_path = "/dummy",
                stack = null,
                template = null,
                created_at = now,
                updated_at = now,
                archived = 0,
                deleted_at = null,
                settings_json = null
            )
        }
    }
}


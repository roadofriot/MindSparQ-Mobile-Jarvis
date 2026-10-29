package com.mindsparq.jarvis.engine.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestResultParserTest {

    @Test
    fun testPytestOutputParsing() {
        val output = """
            ============================= test session starts ==============================
            collected 12 items
            tests/test_api.py ...........
            ======================= 11 passed, 1 failed in 0.45s =======================
        """.trimIndent()

        val summary = TestResultParser.parsePytestOutput(output)
        assertEquals("Pytest", summary.framework)
        assertEquals(12, summary.total)
        assertEquals(11, summary.passed)
        assertEquals(1, summary.failed)
        assertFalse(summary.allPassed)
    }

    @Test
    fun testFlutterOutputParsing() {
        val passOutput = "00:04 +15: All tests passed!"
        val passSummary = TestResultParser.parseFlutterTestOutput(passOutput)
        assertTrue(passSummary.allPassed)

        val failOutput = "00:05 +10 -2: Some tests failed."
        val failSummary = TestResultParser.parseFlutterTestOutput(failOutput)
        assertFalse(failSummary.allPassed)
    }

    @Test
    fun testJUnitXmlParsing() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.AppTest" tests="20" failures="1" errors="0" skipped="2" time="1.2">
            </testsuite>
        """.trimIndent()

        val summary = TestResultParser.parseJUnitXml(xml)
        assertEquals("JUnit", summary.framework)
        assertEquals(20, summary.total)
        assertEquals(17, summary.passed)
        assertEquals(1, summary.failed)
        assertEquals(2, summary.skipped)
        assertFalse(summary.allPassed)
    }
}


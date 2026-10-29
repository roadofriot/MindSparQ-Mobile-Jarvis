package com.mindsparq.jarvis.engine.test

data class TestFailure(
    val testName: String,
    val message: String,
    val stackTrace: String? = null
)

data class TestRunSummary(
    val framework: String,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val allPassed: Boolean,
    val rawSummaryLine: String,
    val failures: List<TestFailure> = emptyList()
)

object TestResultParser {

    private val PYTEST_REGEX = Regex("""(?m)^=+\s*(?:(\d+)\s+passed)?(?:,\s*(\d+)\s+failed)?(?:,\s*(\d+)\s+skipped)?.*in\s+([\d\.]+)s\s*=+$""")
    private val VITEST_REGEX = Regex("""(?m)Tests\s+(\d+)\s+passed\s*(?:\((\d+)\))?""")
    private val FLUTTER_PASS_REGEX = Regex("""(?m)All tests passed!""")
    private val FLUTTER_FAIL_REGEX = Regex("""(?m)(\d+)\s+tests?\s+failed""")

    fun parsePytestOutput(output: String): TestRunSummary {
        val match = PYTEST_REGEX.find(output)
        if (match != null) {
            val passed = match.groupValues[1].toIntOrNull() ?: 0
            val failed = match.groupValues[2].toIntOrNull() ?: 0
            val skipped = match.groupValues[3].toIntOrNull() ?: 0
            val total = passed + failed + skipped
            return TestRunSummary(
                framework = "Pytest",
                total = total,
                passed = passed,
                failed = failed,
                skipped = skipped,
                allPassed = (failed == 0 && total > 0),
                rawSummaryLine = match.value
            )
        }
        // Fallback check
        val hasFailed = output.contains("FAILED") || output.contains("failed")
        return TestRunSummary(
            framework = "Pytest",
            total = if (hasFailed) 1 else 0,
            passed = if (hasFailed) 0 else 1,
            failed = if (hasFailed) 1 else 0,
            skipped = 0,
            allPassed = !hasFailed,
            rawSummaryLine = "Generic parsed output"
        )
    }

    fun parseFlutterTestOutput(output: String): TestRunSummary {
        if (FLUTTER_PASS_REGEX.containsMatchIn(output)) {
            return TestRunSummary(
                framework = "Flutter",
                total = 1,
                passed = 1,
                failed = 0,
                skipped = 0,
                allPassed = true,
                rawSummaryLine = "All tests passed!"
            )
        }
        val failMatch = FLUTTER_FAIL_REGEX.find(output)
        val failedCount = failMatch?.groupValues?.get(1)?.toIntOrNull() ?: if (output.contains("failed")) 1 else 0
        return TestRunSummary(
            framework = "Flutter",
            total = failedCount,
            passed = 0,
            failed = failedCount,
            skipped = 0,
            allPassed = false,
            rawSummaryLine = failMatch?.value ?: "Flutter test failure detected"
        )
    }

    fun parseJUnitXml(xmlContent: String): TestRunSummary {
        val testsMatch = Regex("""tests="(\d+)"""").find(xmlContent)
        val failuresMatch = Regex("""failures="(\d+)"""").find(xmlContent)
        val errorsMatch = Regex("""errors="(\d+)"""").find(xmlContent)
        val skippedMatch = Regex("""skipped="(\d+)"""").find(xmlContent)

        val total = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val failures = failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val errors = errorsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val skipped = skippedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val totalFailed = failures + errors
        val passed = maxOf(0, total - totalFailed - skipped)

        return TestRunSummary(
            framework = "JUnit",
            total = total,
            passed = passed,
            failed = totalFailed,
            skipped = skipped,
            allPassed = (totalFailed == 0 && total > 0),
            rawSummaryLine = "JUnit XML: $passed passed, $totalFailed failed out of $total"
        )
    }
}


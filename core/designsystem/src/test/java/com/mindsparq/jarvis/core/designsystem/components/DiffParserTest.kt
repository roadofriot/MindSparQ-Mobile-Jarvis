package com.mindsparq.jarvis.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DiffParserTest {

    @Test
    fun testParseUnifiedDiff() {
        val diff = """
            @@ -1,3 +1,4 @@
             fun calculate() {
            -    return 0
            +    val factor = 2
            +    return 10 * factor
             }
        """.trimIndent()

        val parsed = DiffParser.parse(diff)
        assertEquals(DiffLineType.HEADER, parsed[0].type)
        assertEquals(DiffLineType.CONTEXT, parsed[1].type)
        assertEquals(DiffLineType.DELETION, parsed[2].type)
        assertEquals(DiffLineType.ADDITION, parsed[3].type)
        assertEquals(DiffLineType.ADDITION, parsed[4].type)
        assertEquals(DiffLineType.CONTEXT, parsed[5].type)
    }
}


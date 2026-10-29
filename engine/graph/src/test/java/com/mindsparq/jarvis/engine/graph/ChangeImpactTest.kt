package com.mindsparq.jarvis.engine.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeImpactTest {

    @Test
    fun testReverseDependencyImpactAnalysis() {
        val analyzer = ChangeImpactAnalyzer()

        val dbNode = SymbolNode("n_db", "Database", SymbolKind.CLASS, "src/data/Database.kt")
        val repoNode = SymbolNode("n_repo", "UserRepository", SymbolKind.CLASS, "src/data/UserRepository.kt")
        val authNode = SymbolNode("n_auth", "AuthService", SymbolKind.CLASS, "src/service/AuthService.kt")
        val testNode = SymbolNode("n_test", "AuthTest", SymbolKind.TEST, "src/test/AuthTest.kt")

        analyzer.addNode(dbNode)
        analyzer.addNode(repoNode)
        analyzer.addNode(authNode)
        analyzer.addNode(testNode)

        // Dependencies:
        // UserRepository imports Database
        // AuthService imports UserRepository
        // AuthTest tests AuthService
        analyzer.addEdge(fromNodeId = "n_repo", toNodeId = "n_db", kind = EdgeKind.IMPORTS)
        analyzer.addEdge(fromNodeId = "n_auth", toNodeId = "n_repo", kind = EdgeKind.IMPORTS)
        analyzer.addEdge(fromNodeId = "n_test", toNodeId = "n_auth", kind = EdgeKind.TESTS)

        // When UserRepository changes -> AuthService and AuthTest must be affected!
        val report = analyzer.analyzeImpact("src/data/UserRepository.kt", maxDepth = 3)

        assertEquals("src/data/UserRepository.kt", report.modifiedTarget)
        assertTrue(report.indirectlyAffectedFiles.contains("src/service/AuthService.kt"))
        assertTrue(report.relatedTestsToRun.contains("src/test/AuthTest.kt"))
    }
}


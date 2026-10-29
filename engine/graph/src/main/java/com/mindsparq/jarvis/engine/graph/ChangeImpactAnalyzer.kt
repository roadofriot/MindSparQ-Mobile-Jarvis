package com.mindsparq.jarvis.engine.graph

enum class SymbolKind {
    FILE,
    CLASS,
    FUNCTION,
    TEST
}

enum class EdgeKind {
    IMPORTS,
    CALLS,
    INHERITS,
    TESTS
}

data class SymbolNode(
    val id: String,
    val name: String,
    val kind: SymbolKind,
    val filePath: String,
    val line: Int = 1
)

data class DependencyEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val kind: EdgeKind
)

data class ImpactReport(
    val modifiedTarget: String,
    val directlyAffectedNodes: List<SymbolNode>,
    val indirectlyAffectedFiles: Set<String>,
    val relatedTestsToRun: Set<String>
)

class ChangeImpactAnalyzer {

    private val nodes = mutableMapOf<String, SymbolNode>()
    private val reverseEdges = mutableMapOf<String, MutableList<DependencyEdge>>() // toNodeId -> list of edges where toNodeId is dependent upon

    fun addNode(node: SymbolNode) {
        nodes[node.id] = node
    }

    fun addEdge(fromNodeId: String, toNodeId: String, kind: EdgeKind) {
        // fromNode depends on toNode (e.g. A imports B -> when B changes, A is impacted)
        reverseEdges.computeIfAbsent(toNodeId) { mutableListOf() }.add(
            DependencyEdge(fromNodeId, toNodeId, kind)
        )
    }

    fun analyzeImpact(modifiedNodeIdOrPath: String, maxDepth: Int = 3): ImpactReport {
        // Find matching nodes
        val initialNodes = nodes.values.filter { it.id == modifiedNodeIdOrPath || it.filePath == modifiedNodeIdOrPath }
        val affectedNodeIds = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>()

        initialNodes.forEach {
            queue.add(it.id to 0)
        }

        while (queue.isNotEmpty()) {
            val (currentId, depth) = queue.removeFirst()
            if (depth >= maxDepth) continue

            val dependents = reverseEdges[currentId] ?: emptyList()
            for (edge in dependents) {
                if (affectedNodeIds.add(edge.fromNodeId)) {
                    queue.add(edge.fromNodeId to depth + 1)
                }
            }
        }

        val affectedNodes = affectedNodeIds.mapNotNull { nodes[it] }
        val affectedFiles = affectedNodes.map { it.filePath }.toSet()
        val relatedTests = affectedNodes
            .filter { it.kind == SymbolKind.TEST || it.filePath.contains("test") }
            .map { it.filePath }
            .toSet()

        return ImpactReport(
            modifiedTarget = modifiedNodeIdOrPath,
            directlyAffectedNodes = affectedNodes,
            indirectlyAffectedFiles = affectedFiles,
            relatedTestsToRun = relatedTests
        )
    }
}


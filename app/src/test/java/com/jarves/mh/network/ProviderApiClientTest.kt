package com.jarves.mh.network

import com.jarves.mh.model.ProviderProtocol
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProviderApiClientTest {
    @Test
    fun openAiResponsesProbeUsesStructuredInputWithoutOutputCap() {
        val body = JSONObject(
            ProviderApiClient().validationBody(
                model = "muse-spark-1.3-contributor-free",
                protocol = ProviderProtocol.OPENAI_RESPONSES,
            ),
        )

        assertEquals("muse-spark-1.3-contributor-free", body.getString("model"))
        val message = body.getJSONArray("input").getJSONObject(0)
        assertEquals("user", message.getString("role"))
        val content = message.getJSONArray("content").getJSONObject(0)
        assertEquals("input_text", content.getString("type"))
        assertEquals("Hello, reply with 1 word.", content.getString("text"))
        assertEquals(false, body.has("max_output_tokens"))
    }

    @Test
    fun openRouterProbeIncludesRequestedProviderOrder() {
        val body = JSONObject(
            ProviderApiClient().validationBody(
                model = "anthropic/claude-sonnet-4.6",
                protocol = ProviderProtocol.OPENROUTER,
                openRouterProviderOrder = " anthropic, amazon-bedrock, anthropic ",
                openRouterAllowFallbacks = false,
            ),
        )

        val routing = body.getJSONObject("provider")
        assertEquals(listOf("anthropic", "amazon-bedrock"), routing.getJSONArray("order").let { array ->
            (0 until array.length()).map(array::getString)
        })
        assertFalse(routing.getBoolean("allow_fallbacks"))
        assertEquals(8, body.getInt("max_tokens"))
    }

    @Test
    fun chatAndAnthropicCompatibleProbesUseProviderSafeTokenLimit() {
        val client = ProviderApiClient()
        val chat = JSONObject(client.validationBody("chat-model", ProviderProtocol.OPENAI_CHAT))
        val anthropic = JSONObject(client.validationBody("claude-model", ProviderProtocol.ANTHROPIC))

        assertEquals(8, chat.getInt("max_tokens"))
        assertEquals(8, anthropic.getInt("max_tokens"))
    }

    @Test
    fun autonomousGenerationBodiesCarryRolePromptsAndBoundedOutput() {
        val client = ProviderApiClient()
        val chat = JSONObject(
            client.generationBody(
                model = "review-model",
                protocol = ProviderProtocol.OPENAI_CHAT,
                systemPrompt = "Act as a reviewer",
                userPrompt = "Review this diff",
                maxTokens = 900,
            ),
        )
        assertEquals(900, chat.getInt("max_tokens"))
        assertEquals("Act as a reviewer", chat.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertEquals("Review this diff", chat.getJSONArray("messages").getJSONObject(1).getString("content"))

        val responses = JSONObject(
            client.generationBody(
                model = "planner-model",
                protocol = ProviderProtocol.OPENAI_RESPONSES,
                systemPrompt = "Act as a planner",
                userPrompt = "Plan this task",
                maxTokens = 700,
            ),
        )
        assertEquals("Act as a planner", responses.getString("instructions"))
        assertEquals("Plan this task", responses.getString("input"))
        assertEquals(700, responses.getInt("max_output_tokens"))
    }

    @Test
    fun generatedTextParserSupportsAnthropicChatAndResponsesFormats() {
        val client = ProviderApiClient()
        assertEquals(
            "review notes",
            client.parseGeneratedText(
                """{"content":[{"type":"text","text":"review notes"}]}""",
                ProviderProtocol.ANTHROPIC,
            ),
        )
        assertEquals(
            "review notes",
            client.parseGeneratedText(
                """{"choices":[{"message":{"content":"review notes"}}]}""",
                ProviderProtocol.OPENAI_CHAT,
            ),
        )
        assertEquals(
            "review notes",
            client.parseGeneratedText(
                """{"output":[{"content":[{"type":"output_text","text":"review notes"}]}]}""",
                ProviderProtocol.OPENAI_RESPONSES,
            ),
        )
    }
}

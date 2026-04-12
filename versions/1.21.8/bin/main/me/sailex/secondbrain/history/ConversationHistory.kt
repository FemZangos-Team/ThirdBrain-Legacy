package me.sailex.secondbrain.history

import com.fasterxml.jackson.databind.ObjectMapper
import me.sailex.secondbrain.constant.Instructions
import me.sailex.secondbrain.llm.LLMClient

class ConversationHistory(
    private val llmClient: LLMClient,
    val latestConversations: MutableList<Message>
) {
    companion object {
        private const val MAX_HISTORY_LENGTH = 30
        private val objectMapper = ObjectMapper()
    }

    @Synchronized
    fun add(message: Message) {
        if (message.role.equals("system", ignoreCase = true)) {
            return
        }
        latestConversations.add(message)

        if (latestConversations.size >= MAX_HISTORY_LENGTH) {
            updateConversations()
        }
    }

    @Synchronized
    fun buildMessagesForApi(systemMessage: String): List<Message> {
        return listOf(Message(systemMessage, "system")) +
            latestConversations.filterNot { it.role.equals("system", ignoreCase = true) }
    }

    private fun updateConversations() {
        val removeCount = MAX_HISTORY_LENGTH / 3
        val toSummarize = latestConversations.subList(0, removeCount).toList()
        val message = summarize(toSummarize)
        latestConversations.removeAll(toSummarize)
        latestConversations.add(0, message)
    }

    private fun summarize(conversations: List<Message>): Message {
        val summarizeMessage = Message(
            Instructions.SUMMARY_PROMPT.format( objectMapper.writeValueAsString(conversations)),
            "user")
        return llmClient.chat(listOf(summarizeMessage))
    }

    fun getLastMessage(): String {
        return latestConversations.last().message
    }
}

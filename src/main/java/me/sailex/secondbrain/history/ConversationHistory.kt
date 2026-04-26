package me.sailex.secondbrain.history

class ConversationHistory(
    val latestConversations: MutableList<Message>
) {
    companion object {
        private const val MAX_HISTORY_LENGTH = 5
    }

    @Synchronized
    fun add(message: Message) {
        if (message.role.equals("system", ignoreCase = true)) {
            return
        }
        latestConversations.add(message)

        while (latestConversations.size > MAX_HISTORY_LENGTH) {
            latestConversations.removeAt(0)
        }
    }

    @Synchronized
    fun buildMessagesForApi(systemMessage: String): List<Message> {
        return listOf(Message(systemMessage, "system")) +
            latestConversations.filterNot { it.role.equals("system", ignoreCase = true) }
    }

    fun getLastMessage(): String {
        return latestConversations.lastOrNull()?.message ?: ""
    }
}

package me.sailex.secondbrain.database.repositories

import me.sailex.secondbrain.database.SqliteClient
import me.sailex.secondbrain.model.database.Conversation
import java.util.UUID

class ConversationRepository(
    val sqliteClient: SqliteClient,
) {
    fun init() {
        createTable()
    }

    fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid CHARACTER(36) NOT NULL,
                role CHARACTER(9) NOT NULL,
                message TEXT NOT NULL
            );
        """
        sqliteClient.update(sql)
    }

    fun insert(conversation: Conversation) {
        val statement = sqliteClient.buildPreparedStatement(
            "INSERT INTO conversations (uuid, role, message) VALUES (?, ?, ?)",
        )
        try {
            statement.setString(1, conversation.uuid.toString())
            statement.setString(2, conversation.role)
            statement.setString(3, conversation.message)
            statement.executeUpdate()
        } finally {
            statement.close()
        }
    }

    /**
     * Selects latest one hundred conversations of an NPC
     */
    fun selectByUuid(uuid: UUID): List<Conversation> {
        val statement = sqliteClient.buildPreparedStatement(
            "SELECT uuid, role, message FROM conversations WHERE uuid = ? ORDER BY id DESC LIMIT 100"
        )
        val latestConversations = arrayListOf<Conversation>()

        try {
            statement.setString(1, uuid.toString())
            val result = statement.executeQuery()
            try {
                while (result.next()) {
                    latestConversations.add(
                        Conversation(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("role"),
                            result.getString("message")
                        )
                    )
                }
            } finally {
                result.close()
            }
        } finally {
            statement.close()
        }

        latestConversations.reverse()
        return latestConversations
    }

    /**
     * Deletes all conversations of the given uuid.
     */
    fun deleteByUuid(uuid: UUID) {
        val statement = sqliteClient.buildPreparedStatement(
            "DELETE FROM conversations WHERE uuid = ?"
        )
        try {
            statement.setString(1, uuid.toString())
            statement.executeUpdate()
        } finally {
            statement.close()
        }
    }
}

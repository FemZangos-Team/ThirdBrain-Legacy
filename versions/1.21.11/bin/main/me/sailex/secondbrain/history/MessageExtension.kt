@file:JvmName("MessageConverter")
package me.sailex.secondbrain.history

import io.github.ollama4j.models.chat.OllamaChatMessage
import io.github.ollama4j.models.chat.OllamaChatMessageRole
import me.sailex.secondbrain.llm.player2.model.Player2ChatMessage
import me.sailex.secondbrain.llm.player2.model.Player2ResponseMessage
import me.sailex.secondbrain.llm.roles.Player2ChatRole

// player2
fun Player2ResponseMessage.toMessage(): Message = Message(
    this.content,
    this.role.toString().lowercase()
)

fun Message.toPlayer2ChatMessage(): Player2ChatMessage = Player2ChatMessage(
    Player2ChatRole.valueOf(this.role.uppercase()),
    this.message
)

// ollama
fun OllamaChatMessage.toMessage(): Message = Message(
    this.content,
    this.role.toString().lowercase()
)

fun Message.toOllamaChatMessage(): OllamaChatMessage = OllamaChatMessage(
    OllamaChatMessageRole.getRole(this.role),
    this.message
)

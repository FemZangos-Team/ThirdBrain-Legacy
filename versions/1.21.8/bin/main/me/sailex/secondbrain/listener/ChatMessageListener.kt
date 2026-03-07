package me.sailex.secondbrain.listener

import me.sailex.secondbrain.model.NPC
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import java.util.UUID

class ChatMessageListener(
    npcs: Map<UUID, NPC>
) : AEventListener(npcs) {
    companion object {
        private const val CHAT_HEARING_RANGE_SQUARED = 16.0 // 4 blocks
    }

    override fun register() {
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            npcs.forEach { npcEntry ->
                if (npcEntry.value.entity.uuid == sender.uuid) {
                    return@forEach
                }
                if (npcEntry.value.entity.squaredDistanceTo(sender) > CHAT_HEARING_RANGE_SQUARED) {
                    return@forEach
                }
                val chatMessage =
                    String.format(
                        "Player '%s' has written the message: %s",
                        sender.name.string ?: "Server Console",
                        message.content.string,
                    )
                npcEntry.value.eventHandler.onEvent(chatMessage)
            }
        }
    }
}

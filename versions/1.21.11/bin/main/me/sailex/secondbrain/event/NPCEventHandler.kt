package me.sailex.secondbrain.event

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import me.sailex.altoclef.AltoClefController
import me.sailex.secondbrain.config.NPCConfig
import me.sailex.secondbrain.constant.Instructions
import me.sailex.secondbrain.context.ContextProvider
import me.sailex.secondbrain.history.ConversationHistory
import me.sailex.secondbrain.history.Message
import me.sailex.secondbrain.llm.LLMClient
import me.sailex.secondbrain.llm.openai.OpenAiClient
import me.sailex.secondbrain.llm.player2.Player2APIClient
import me.sailex.secondbrain.llm.roles.Player2ChatRole
import me.sailex.secondbrain.util.LogUtil
import me.sailex.secondbrain.util.PromptFormatter
import net.minecraft.util.math.BlockPos
import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NPCEventHandler(
    private val llmClient: LLMClient,
    private val history: ConversationHistory,
    private val contextProvider: ContextProvider,
    private val controller: AltoClefController,
    private val config: NPCConfig,
): EventHandler {
    companion object {
        private val gson = GsonBuilder()
            .setLenient()
            .create()
    }

    private val executorService: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(10),
        ThreadPoolExecutor.DiscardPolicy()
    )
    private val commandErrorPromptTimestamps = ArrayDeque<Long>()
    private val diagRateLimitMs = 5000L
    private val commandLoopWindowMs = 10_000L

    /**
     * Processes an event asynchronously by allowing call actions from llm using the specified prompt.
     * Saves the prompt and responses in conversation history.
     *
     * @param prompt prompt of a user or system e.g. chatmessage of a player
     */
    override fun onEvent(prompt: String) {
        val queueDepthBeforeEnqueue = executorService.queue.size
        logQueueDepthDiagnostic(queueDepthBeforeEnqueue)
        trackCommandErrorLoopPressure(prompt)

        CompletableFuture.runAsync({
            val eventStartNs = System.nanoTime()
            LogUtil.info("onEvent: $prompt")

            val worldContext = contextProvider.buildContext()
            val zoneAwarePrompt = applyZoneSpecificBehaviour(prompt, worldContext.state().position())
            val formattedPrompt: String = PromptFormatter.format(zoneAwarePrompt, worldContext)

            history.add(Message(formattedPrompt, Player2ChatRole.USER.toString().lowercase()))
            val systemPrompt = Instructions.getLlmSystemPrompt(
                config.npcName,
                config.getEffectiveLlmCharacter(),
                controller.commandExecutor.allCommands(),
                config.llmType
            )
            val llmStartNs = System.nanoTime()
            val response = llmClient.chat(history.buildMessagesForApi(systemPrompt))
            val llmCallMs = millisSince(llmStartNs)
            logLlmLatencyDiagnostic(llmCallMs)
            history.add(response)

            val parsedMessage = parse(response.message)
            val commandDispatchStartNs = System.nanoTime()
            val succeeded = execute(parsedMessage.command)
            val commandDispatchMs = millisSince(commandDispatchStartNs)
            logCommandDispatchLatencyDiagnostic(commandDispatchMs)
            if (!succeeded) {
                logEventTotalLatencyDiagnostic(millisSince(eventStartNs), queueDepthBeforeEnqueue, llmCallMs, commandDispatchMs)
                return@runAsync
            }

            //prevent printing multiple times the same when llm is running in command syntax errors
            if (parsedMessage.message != history.getLastMessage()) {
                // Always send text chat; TTS is additional output when enabled.
                controller.controllerExtras.chat(parsedMessage.message)
                if (!config.isTTS) return@runAsync

                when (llmClient) {
                    is Player2APIClient -> llmClient.startTextToSpeech(parsedMessage.message)
                    is OpenAiClient -> {
                        try {
                            llmClient.startTextToSpeech(parsedMessage.message)
                        } catch (e: Exception) {
                            LogUtil.error("OpenAI TTS failed", e)
                        }
                    }
                    else -> {}
                }
            }
            logEventTotalLatencyDiagnostic(millisSince(eventStartNs), queueDepthBeforeEnqueue, llmCallMs, commandDispatchMs)
        }, executorService)
            .exceptionally {
                LogUtil.debugInChat("Could not generate a response: " + buildErrorMessage(it))
                LogUtil.error("Error occurred handling event: $prompt", it)
                null
            }
    }

    override fun stopService() {
        executorService.shutdownNow()
    }

    override fun queueIsEmpty(): Boolean {
        return executorService.queue.isEmpty()
    }

    //TODO: refactor this into own class
    private fun parse(content: String): CommandMessage {
        return try {
            parseContent(content)
        } catch (_: JsonParseException) {
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
            try {
                parseContent(cleanedContent)
            } catch (e: JsonParseException) {
                throw CustomEventException("The selected model may be too small to understand the context or to reliably produce valid JSON. " +
                        "Please switch to a larger or more capable LLM model.", e)
            }
        }
    }

    private fun parseContent(content: String): CommandMessage {
        return gson.fromJson(content, CommandMessage::class.java)
    }

    fun execute(command: String): Boolean {
        var successful = true
        val cmdExecutor = controller.commandExecutor
        val normalized = command.lowercase()
        val blocked = listOf("attack", "kill", "break", "place", "mine", "punch", "destroy")
        val safeCommand = if (blocked.any { normalized.contains(it) }) "idle" else command
        val commandWithPrefix = if (cmdExecutor.isClientCommand(safeCommand)) {
            safeCommand
        } else {
            cmdExecutor.commandPrefix + safeCommand
        }
        cmdExecutor.execute(commandWithPrefix, {
//            if (queueIsEmpty()) {
//                //this.onEvent(Instructions.COMMAND_FINISHED_PROMPT.format(commandWithPrefix))
//            }
        }, {
            successful = false
            this.onEvent(Instructions.COMMAND_ERROR_PROMPT.format(commandWithPrefix, it.message))
            LogUtil.error("Error executing command: $commandWithPrefix", it)
        })
        return successful
    }

    data class CommandMessage(
        val command: String,
        val message: String
    )

    private fun buildErrorMessage(exception: Throwable): String {
        val chain = generateSequence(exception) { it.cause }.toList()
        val custom = chain.filterIsInstance<CustomEventException>().firstOrNull()
        if (custom != null) return custom.message ?: "Custom event error"
        val meaningful = chain.firstOrNull { !it.message.isNullOrBlank() && it.message!!.trim() != "ERROR :" }
        if (meaningful != null) return meaningful.message!!
        return "LLM request failed: provider returned a non-2xx response with an empty error body. Check base URL, model, and API key."
    }

    private fun applyZoneSpecificBehaviour(prompt: String, position: BlockPos): String {
        val matchingZone = config.zoneBehaviors
            .filter { it.instructions.isNotBlank() && it.contains(position) }
            .maxByOrNull { it.priority }
            ?: return prompt

        return """
            $prompt

            Additional zone instructions (zone: ${matchingZone.name}, priority: ${matchingZone.priority}):
            ${matchingZone.instructions}
        """.trimIndent()
    }

    private fun trackCommandErrorLoopPressure(prompt: String) {
        if (!LogUtil.isVerboseEnabled()) return
        val trimmedPrompt = prompt.trimStart()
        if (!trimmedPrompt.startsWith("Command ") || !trimmedPrompt.contains(" failed. Error content:")) return

        val now = System.currentTimeMillis()
        val failuresInWindow: Int
        synchronized(commandErrorPromptTimestamps) {
            while (true) {
                val oldest = commandErrorPromptTimestamps.peekFirst() ?: break
                if (now - oldest > commandLoopWindowMs) {
                    commandErrorPromptTimestamps.removeFirst()
                } else {
                    break
                }
            }
            commandErrorPromptTimestamps.addLast(now)
            failuresInWindow = commandErrorPromptTimestamps.size
        }

        if (failuresInWindow > 3) {
            LogUtil.warnRateLimited(
                "event.command_loop_pressure.${config.npcName}",
                "[SB-DIAG] area=command-loop npc=${config.npcName} thread=${Thread.currentThread().name} metric=failed_command_retries_10s value=$failuresInWindow",
                diagRateLimitMs
            )
        }
    }

    private fun logQueueDepthDiagnostic(queueDepthBeforeEnqueue: Int) {
        if (!LogUtil.isVerboseEnabled() || queueDepthBeforeEnqueue < 8) return
        LogUtil.warnRateLimited(
            "event.queue_depth.${config.npcName}",
            "[SB-DIAG] area=event npc=${config.npcName} thread=${Thread.currentThread().name} metric=queue_depth value=$queueDepthBeforeEnqueue",
            diagRateLimitMs
        )
    }

    private fun logLlmLatencyDiagnostic(llmCallMs: Long) {
        if (!LogUtil.isVerboseEnabled() || llmCallMs <= 1200) return
        LogUtil.warnRateLimited(
            "event.llm_latency.${config.npcName}",
            "[SB-DIAG] area=event npc=${config.npcName} thread=${Thread.currentThread().name} metric=llm_ms value=$llmCallMs",
            diagRateLimitMs
        )
    }

    private fun logCommandDispatchLatencyDiagnostic(commandDispatchMs: Long) {
        if (!LogUtil.isVerboseEnabled() || commandDispatchMs <= 500) return
        LogUtil.warnRateLimited(
            "event.command_dispatch.${config.npcName}",
            "[SB-DIAG] area=event npc=${config.npcName} thread=${Thread.currentThread().name} metric=command_dispatch_ms value=$commandDispatchMs",
            diagRateLimitMs
        )
    }

    private fun logEventTotalLatencyDiagnostic(totalMs: Long, queueDepthBeforeEnqueue: Int, llmCallMs: Long, commandDispatchMs: Long) {
        if (!LogUtil.isVerboseEnabled() || totalMs <= 1500) return
        LogUtil.warnRateLimited(
            "event.total_latency.${config.npcName}",
            "[SB-DIAG] area=event npc=${config.npcName} thread=${Thread.currentThread().name} metric=event_total_ms value=$totalMs queue_depth_before=$queueDepthBeforeEnqueue llm_ms=$llmCallMs command_dispatch_ms=$commandDispatchMs",
            diagRateLimitMs
        )
    }

    private fun millisSince(startNs: Long): Long {
        return (System.nanoTime() - startNs) / 1_000_000L
    }

}

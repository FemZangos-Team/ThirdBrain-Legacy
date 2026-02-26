package me.sailex.secondbrain.database.resources

import me.sailex.secondbrain.database.repositories.ConversationRepository
import me.sailex.secondbrain.history.Message
import me.sailex.secondbrain.model.database.Conversation
import me.sailex.secondbrain.util.LogUtil
import java.util.UUID

import java.util.concurrent.CompletableFuture

import java.util.concurrent.ExecutorService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ResourceProvider(
    val conversationRepository: ConversationRepository
) {
    @Volatile
    private var executorService: ExecutorService? = null
    private val loadedConversations = ConcurrentHashMap<UUID, List<Conversation>>()

    /**
     * Loads conversations recipes from db/mc into memory
     */
    fun loadResources(uuids: List<UUID>) {
        val activeExecutor = replaceExecutor()
        runAsync(activeExecutor) {
            LogUtil.info("Loading conversations into memory...")
            uuids.forEach {
                loadedConversations[it] = conversationRepository.selectByUuid(it)
            }
        }.whenComplete { _, _ ->
            activeExecutor.shutdown()
        }
    }

    fun addConversations(uuid: UUID, messages: List<Message>) {
        putLoadedConversation(uuid, messages.map { Conversation(uuid, it.role, it.message) })
    }

    fun getLoadedConversation(uuid: UUID): List<Conversation>? {
        return loadedConversations[uuid]
    }

    fun putLoadedConversation(uuid: UUID, conversations: List<Conversation>) {
        loadedConversations[uuid] = conversations
    }

    fun removeLoadedConversation(uuid: UUID) {
        loadedConversations.remove(uuid)
    }

    /**
     * Saves recipes and conversations to local db. (called on server stop)
     *
     * Stops initial resources indexing if not finished by shutting down executor
     */
    fun saveResources() {
        val activeExecutor = replaceExecutor()
        runAsync(activeExecutor) {
            loadedConversations.forEach { (uuid, conversations) ->
                // Forward-only fix: rewrite each NPC's current conversation snapshot to avoid new duplicates.
                conversationRepository.deleteByUuid(uuid)
                conversations.forEach { conversationRepository.insert(it) }
            }
            LogUtil.info("Saved conversations to db")
        }.get()
        activeExecutor.shutdown()
    }

    @Synchronized
    private fun replaceExecutor(): ExecutorService {
        shutdownServiceNow()
        val activeExecutor = initExecutorPool()
        executorService = activeExecutor
        return activeExecutor
    }

    private fun shutdownServiceNow() {
        val currentExecutor = executorService ?: return
        if (!currentExecutor.isTerminated && !currentExecutor.isShutdown) {
            currentExecutor.shutdownNow()
            LogUtil.info("Interrupted ongoing resource task during executor reset.")
            currentExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        }
    }

    private fun runAsync(executor: ExecutorService, task: () -> Unit): CompletableFuture<Void> {
        return CompletableFuture.runAsync(task, executor).exceptionally {
            LogUtil.error("Error loading/saving resources into memory", it)
            null
        }
    }

    private fun initExecutorPool(): ExecutorService {
        return Executors.newFixedThreadPool(2)
    }
}

package me.sailex.secondbrain.commands;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.AllArgsConstructor;
import me.sailex.secondbrain.common.NPCService;
import me.sailex.secondbrain.config.ConfigProvider;
import me.sailex.secondbrain.config.NPCConfig;
import me.sailex.secondbrain.util.LogUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Optional;

@AllArgsConstructor
public class NPCMemoryCommand {

	private final NPCService npcService;
	private final ConfigProvider configProvider;

	public LiteralArgumentBuilder<ServerCommandSource> getCommand() {
		return literal("memory")
			.requires(/*? >=1.21.11 {*/ source -> net.minecraft.server.command.CommandManager.MODERATORS_CHECK.allows(source.getPermissions()) /*?} else {*/ source -> source.hasPermissionLevel(2) /*?}*/)
			.then(literal("create")
				.then(literal("unlocked")
						.then(argument("npcName", StringArgumentType.string())
								.suggests((context, builder) -> {
									configProvider.getNpcConfigs().stream().map(NPCConfig::getNpcName).forEach(builder::suggest);
									return builder.buildFuture();
								}).then(argument("memoryPrompt", StringArgumentType.greedyString())
										.executes(context -> createMemory(context, true))))
				).then(literal("locked")
						.then(argument("npcName", StringArgumentType.string())
								.suggests((context, builder) -> {
									configProvider.getNpcConfigs().stream().map(NPCConfig::getNpcName).forEach(builder::suggest);
									return builder.buildFuture();
								}).then(argument("memoryPrompt", StringArgumentType.greedyString())
										.executes(context -> createMemory(context, false)))))
			.then(literal("unlock")
				.then(argument("npcName", StringArgumentType.string())
						.suggests((context, builder) -> {
							configProvider.getNpcConfigs().stream().map(NPCConfig::getNpcName).forEach(builder::suggest);
							return builder.buildFuture();
						}).then(argument("memoryId", StringArgumentType.string())
								.suggests((context, builder) -> {
									String npcName = StringArgumentType.getString(context, "npcName");
									Optional<NPCConfig> configOpt = configProvider.getNpcConfigByName(npcName);
									configOpt.ifPresent(config -> config.getMemoryFragments()
											.forEach(fragment -> builder.suggest(fragment.getId())));
									return builder.buildFuture();
								}).executes(this::unlockMemory))))
			.then(literal("lock")
				.then(argument("npcName", StringArgumentType.string())
						.suggests((context, builder) -> {
							configProvider.getNpcConfigs().stream().map(NPCConfig::getNpcName).forEach(builder::suggest);
							return builder.buildFuture();
						}).then(argument("memoryId", StringArgumentType.string())
								.suggests((context, builder) -> {
									String npcName = StringArgumentType.getString(context, "npcName");
									Optional<NPCConfig> configOpt = configProvider.getNpcConfigByName(npcName);
									configOpt.ifPresent(config -> config.getMemoryFragments()
											.forEach(fragment -> builder.suggest(fragment.getId())));
									return builder.buildFuture();
									}).executes(this::lockMemory)))));
	}

	private int createMemory(CommandContext<ServerCommandSource> context, boolean isUnlocked) {
		String npcName = StringArgumentType.getString(context, "npcName");
		String memoryPrompt = StringArgumentType.getString(context, "memoryPrompt");
		NPCService.MemoryCreateResult result = npcService.createMemoryForNpc(npcName, memoryPrompt, isUnlocked);
		NPCService.MemoryCreateStatus status = result.getStatus();
		String memoryId = result.getMemoryId();

		switch (status) {
			case SUCCESS -> {
				String unlockedState = isUnlocked ? "unlocked" : "locked";
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Created memory '" + memoryId + "' as " + unlockedState + " for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
			case NPC_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("NPC '" + npcName + "' not found"),
						false
				);
				return 0;
			}
			case INVALID_INPUT -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("Memory prompt cannot be empty"),
						false
				);
				return 0;
			}
			case MEMORY_ID_ALREADY_EXISTS -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("Memory id '" + memoryId + "' already exists for NPC '" + npcName + "'"),
						false
				);
				return 0;
			}
		}
		return 0;
	}

	private int unlockMemory(CommandContext<ServerCommandSource> context) {
		String npcName = StringArgumentType.getString(context, "npcName");
		String memoryId = StringArgumentType.getString(context, "memoryId");
		NPCService.MemoryUnlockStatus status = npcService.unlockMemoryForNpc(npcName, memoryId);

		switch (status) {
			case SUCCESS -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Unlocked memory '" + memoryId + "' for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
			case NPC_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("NPC '" + npcName + "' not found"),
						false
				);
				return 0;
			}
			case MEMORY_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("Memory fragment '" + memoryId + "' not found for NPC '" + npcName + "'"),
						false
				);
				return 0;
			}
			case NO_CHANGE -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Memory fragment '" + memoryId + "' is already unlocked for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
		}
		return 0;
	}

	private int lockMemory(CommandContext<ServerCommandSource> context) {
		String npcName = StringArgumentType.getString(context, "npcName");
		String memoryId = StringArgumentType.getString(context, "memoryId");
		NPCService.MemoryUnlockStatus status = npcService.lockMemoryForNpc(npcName, memoryId);

		switch (status) {
			case SUCCESS -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Locked memory '" + memoryId + "' for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
			case NPC_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("NPC '" + npcName + "' not found"),
						false
				);
				return 0;
			}
			case MEMORY_NOT_FOUND -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatError("Memory fragment '" + memoryId + "' not found for NPC '" + npcName + "'"),
						false
				);
				return 0;
			}
			case NO_CHANGE -> {
				context.getSource().sendFeedback(
						() -> LogUtil.formatInfo("Memory fragment '" + memoryId + "' is already locked for NPC '" + npcName + "'"),
						false
				);
				return 1;
			}
		}
		return 0;
	}
}

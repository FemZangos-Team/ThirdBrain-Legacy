error id: file://<WORKSPACE>/src/main/java/me/sailex/secondbrain/commands/CommandManager.java:_empty_/NPCCreateCommand#
file://<WORKSPACE>/src/main/java/me/sailex/secondbrain/commands/CommandManager.java
empty definition using pc, found symbol in pc: _empty_/NPCCreateCommand#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1441
uri: file://<WORKSPACE>/src/main/java/me/sailex/secondbrain/commands/CommandManager.java
text:
```scala
package me.sailex.secondbrain.commands;

import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.AllArgsConstructor;
import me.sailex.secondbrain.common.NPCService;
import me.sailex.secondbrain.common.Player2NpcSynchronizer;
import me.sailex.secondbrain.config.ConfigProvider;
import me.sailex.secondbrain.networking.NetworkHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Command manager class that registers all commands.
 */
@AllArgsConstructor
public class CommandManager {

	private final NPCService npcService;
	private final ConfigProvider configProvider;
	private final NetworkHandler networkHandler;
	private final Player2NpcSynchronizer synchronizer;

	public void registerAll() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			{
				dispatcher.register(buildRootCommand("thirdbrain"));
				dispatcher.register(buildRootCommand("secondbrain"));
			}
		);
	}

	private LiteralArgumentBuilder<ServerCommandSource> buildRootCommand(String literalName) {
		return literal(literalName)
			.requires(/*? >=1.21.11 {*/ source -> net.minecraft.server.command.CommandManager.MODERATORS_CHECK.allows(source.getPermissions()) /*?} else {*/ source -> source.hasPermissionLevel(2) /*?}*/)
			.then(new NPCCreateComma@@nd(npcService).getCommand())
			.then(new NPCMemoryCommand(npcService, configProvider).getCommand())
			.then(new NPCRemoveCommand(npcService, configProvider).getCommand())
			.then(new Player2ActionCommand(synchronizer).getCommand())
			.executes(context -> new GuiCommand(configProvider, networkHandler).execute(context));
	}
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/NPCCreateCommand#
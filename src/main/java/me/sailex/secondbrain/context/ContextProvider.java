package me.sailex.secondbrain.context;

import java.util.*;

import me.sailex.secondbrain.config.BaseConfig;
import me.sailex.secondbrain.model.context.*;
import me.sailex.secondbrain.util.LogUtil;
import me.sailex.secondbrain.util.MCDataUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Generates the context for the LLM requests based on the NPCs world environment.
 */
public class ContextProvider {
	private static final long DIAG_RATE_LIMIT_MS = 5000L;

	private final ServerPlayerEntity npcEntity;
	private final ChunkManager chunkManager;
	private WorldContext cachedContext;

	public ContextProvider(ServerPlayerEntity npcEntity, BaseConfig config) {
		this(npcEntity, config, Math.max(1, config.getContextChunkRadius()) * 16);
	}

	public ContextProvider(ServerPlayerEntity npcEntity, BaseConfig config, int contextRangeInBlocks) {
		this.npcEntity = npcEntity;
		this.chunkManager = new ChunkManager(npcEntity, Math.max(1, contextRangeInBlocks), config.getContextVerticalScanRange(), config.getChunkExpiryTime());
		buildContext();
	}

	/**
	 * Builds a context of the NPC entity world environment.
	 */
	public WorldContext buildContext() {
		synchronized (this) {
			long startNs = System.nanoTime();
			long stateStartNs = System.nanoTime();
			StateData state = getNpcState();
			long stateMs = millisSince(stateStartNs);

			long inventoryStartNs = System.nanoTime();
			InventoryData inventory = getInventoryState();
			long inventoryMs = millisSince(inventoryStartNs);

			long entitiesStartNs = System.nanoTime();
			NearbyEntitiesSnapshot nearbyEntities = getNearbyEntitiesSnapshot();
			long entitiesMs = millisSince(entitiesStartNs);

			WorldContext context = new WorldContext(
					state,
					inventory,
					List.copyOf(chunkManager.getNearbyBlocks()),
					nearbyEntities.entities()
			);
//			chunkManager.getNearbyBlocks().forEach(blockData -> LogUtil.debugInChat(blockData.toString()));
			this.cachedContext = context;
			long totalMs = millisSince(startNs);
			logContextDiagnostics(totalMs, stateMs, inventoryMs, entitiesMs, nearbyEntities.entityCount(), nearbyEntities.topEntitiesSummary());
			return context;
		}
	}

	private StateData getNpcState() {
		return new StateData(
				npcEntity.getBlockPos(),
				npcEntity.getHealth(),
				npcEntity.getHungerManager().getFoodLevel(),
				MCDataUtil.getBiome(npcEntity));
	}

	private InventoryData getInventoryState() {
		PlayerInventory inventory = npcEntity.getInventory();
		return new InventoryData(
				// armour
				getItemsInRange(inventory, 36, 39),
				// main inventory
				getItemsInRange(inventory, 9, 35),
				// hotbar
				getItemsInRange(inventory, 0, 8),
				// off-hand
				getItemsInRange(inventory, 40, 40)
		);
	}

	private List<ItemData> getItemsInRange(PlayerInventory inventory, int start, int end) {
		List<ItemData> items = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			ItemStack stack = inventory.getStack(i);
			addItemData(stack, items, i);
		}
		return items;
	}

	private void addItemData(ItemStack stack, List<ItemData> items, int slot) {
		if (!stack.isEmpty()) {
			items.add(new ItemData(getBlockName(stack), stack.getCount(), slot));
		}
	}

	private String getBlockName(ItemStack stack) {
		String translationKey = stack.getItem().getTranslationKey();
		return translationKey.substring(translationKey.lastIndexOf(".") + 1);
	}

	private NearbyEntitiesSnapshot getNearbyEntitiesSnapshot() {
		List<EntityData> nearbyEntities = new ArrayList<>();
		List<Entity> entities = MCDataUtil.getNearbyEntities(npcEntity);
		Map<String, Integer> typeCounts = new HashMap<>();
		entities.forEach(entity ->
			{
				nearbyEntities.add(new EntityData(entity.getId(), entity.getName().getString(), entity.isPlayer()));
				String typeId = entity.getType().toString();
				typeCounts.merge(typeId, 1, Integer::sum);
			}
		);
		String topEntities = typeCounts.entrySet().stream()
				.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
				.limit(5)
				.map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
				.reduce((a, b) -> a + "," + b)
				.orElse("none");
		return new NearbyEntitiesSnapshot(nearbyEntities.stream().toList(), entities.size(), topEntities);
	}

	public ChunkManager getChunkManager() {
		return chunkManager;
	}

	public WorldContext getCachedContext() {
		return cachedContext;
	}

	private void logContextDiagnostics(long totalMs, long stateMs, long inventoryMs, long entitiesMs, int entityCount, String topEntitiesSummary) {
		if (!LogUtil.isVerboseEnabled()) {
			return;
		}
		String npcName = npcEntity.getName().getString();
		String threadName = Thread.currentThread().getName();

		if (totalMs > 100) {
			LogUtil.warnRateLimited(
					"context.build.slow." + npcName,
					"[SB-DIAG] area=context npc=%s thread=%s metric=context_ms value=%d state_ms=%d inventory_ms=%d entities_ms=%d entity_count=%d nearby_types=%d"
							.formatted(npcName, threadName, totalMs, stateMs, inventoryMs, entitiesMs, entityCount, chunkManager.getNearbyBlocks().size()),
					DIAG_RATE_LIMIT_MS
			);
		}
		if (entityCount > 150) {
			LogUtil.warnRateLimited(
					"context.entities.high." + npcName,
					"[SB-DIAG] area=context npc=%s thread=%s metric=entity_count value=%d top_entities=%s"
							.formatted(npcName, threadName, entityCount, topEntitiesSummary),
					DIAG_RATE_LIMIT_MS
			);
		}
	}

	private long millisSince(long startNs) {
		return (System.nanoTime() - startNs) / 1_000_000L;
	}

	private record NearbyEntitiesSnapshot(List<EntityData> entities, int entityCount, String topEntitiesSummary) {}
}

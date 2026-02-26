package me.sailex.secondbrain.config;

import me.sailex.secondbrain.constant.Instructions;
import me.sailex.secondbrain.llm.LLMType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;

public class NPCConfig implements Configurable {

	private String npcName = "Steve";
	private UUID uuid = UUID.randomUUID();
	private boolean isActive = true;
	private String llmCharacter = Instructions.DEFAULT_CHARACTER_TRAITS;
	private LLMType llmType = LLMType.OLLAMA;
	private String llmModel = "llama3.2";
	private String voiceId = "not set";
	private String skinUrl = "";
	private int conversationRange = 64;
	private List<MemoryFragment> memoryFragments = new ArrayList<>();
	private List<ZoneBehavior> zoneBehaviors = new ArrayList<>();

	private boolean isTTS = false;

	public NPCConfig() {}

	public NPCConfig(String npcName) {
		this.npcName = npcName;
	}

	public NPCConfig(
		String npcName,
		String uuid,
		boolean isActive,
		String llmCharacter,
		LLMType llmType,
        String llmModel,
		boolean isTTS,
		String voiceId,
		String skinUrl,
		int conversationRange,
		List<MemoryFragment> memoryFragments,
		List<ZoneBehavior> zoneBehaviors
	) {
		this.npcName = npcName;
		this.uuid = UUID.fromString(uuid);
		this.isActive = isActive;
		this.llmCharacter = llmCharacter;
		this.llmType = llmType;
        this.llmModel = llmModel;
		this.isTTS = isTTS;
		this.voiceId = voiceId;
		this.skinUrl = skinUrl;
		setConversationRange(conversationRange);
		setMemoryFragments(memoryFragments);
		setZoneBehaviors(zoneBehaviors);
	}

	public static class Builder {

		private final NPCConfig npcConfig;

		public Builder(String npcName) {
			this.npcConfig = new NPCConfig(npcName);
		}

		public Builder uuid(UUID uuid) {
			npcConfig.setUuid(uuid);
			return this;
		}

		public Builder llmDefaultPrompt(String llmDefaultPrompt) {
			npcConfig.setLlmCharacter(llmDefaultPrompt);
			return this;
		}

		public Builder llmType(LLMType llmType) {
			npcConfig.setLlmType(llmType);
			return this;
		}

		public Builder voiceId(String voiceId) {
			npcConfig.setVoiceId(voiceId);
			return this;
		}

		public Builder skinUrl(String skinUrl) {
			npcConfig.setSkinUrl(skinUrl);
			return this;
		}

		public NPCConfig build() {
			return npcConfig;
		}

	}

	public static Builder builder(String npcName) {
		return new Builder(npcName);
	}

	public String getNpcName() {
		return npcName;
	}

	public boolean isActive() {
		return isActive;
	}

	public String getLlmCharacter() {
		return llmCharacter;
	}

	public LLMType getLlmType() {
		return llmType;
	}

	public String getLlmModel() {
        return llmModel;
    }

	public String getEffectiveLlmCharacter() {
		StringBuilder effectivePrompt = new StringBuilder(llmCharacter == null ? "" : llmCharacter);
		String unlockedMemoryPrompt = buildUnlockedMemoryPrompt();
		if (!unlockedMemoryPrompt.isBlank()) {
			effectivePrompt.append("\n\n").append(unlockedMemoryPrompt);
		}
		return effectivePrompt.toString();
	}

	public Optional<MemoryFragment> getMemoryFragment(String memoryId) {
		if (memoryId == null || memoryId.isBlank()) {
			return Optional.empty();
		}
		return getMemoryFragments().stream()
				.filter(fragment -> fragment.getId().equalsIgnoreCase(memoryId.trim()))
				.findFirst();
	}

	public String buildUnlockedMemoryPrompt() {
		StringBuilder unlockedFragments = new StringBuilder("Unlocked memory fragments:\n");
		getMemoryFragments().stream()
				.filter(MemoryFragment::isUnlocked)
				.forEach(fragment -> unlockedFragments
						.append(" - ").append(fragment.toSystemPrompt())
						.append("\n"));
		if (unlockedFragments.toString().equals("Unlocked memory fragments:\n")) {
			return "";
		}
		return unlockedFragments.toString().trim();
	}

	public List<MemoryFragment> getMemoryFragments() {
		if (memoryFragments == null) {
			memoryFragments = new ArrayList<>();
		}
		return memoryFragments;
	}

	public void setMemoryFragments(List<MemoryFragment> memoryFragments) {
		this.memoryFragments = memoryFragments == null ? new ArrayList<>() : new ArrayList<>(memoryFragments);
	}

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }


	public void setLlmCharacter(String llmCharacter) {
		this.llmCharacter = llmCharacter;
	}

	public void setLlmType(LLMType llmType) {
		this.llmType = llmType;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public void setNpcName(String npcName) {
		this.npcName = npcName;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getVoiceId() {
		return voiceId;
	}

	public void setVoiceId(String voiceId) {
		this.voiceId = voiceId;
	}

	public boolean isTTS() {
		return isTTS;
	}

	public void setTTS(boolean TTS) {
		isTTS = TTS;
	}

	public String getSkinUrl() {
		return skinUrl;
	}

	public void setSkinUrl(String skinUrl) {
		this.skinUrl = skinUrl;
	}

	public int getConversationRange() {
		return conversationRange;
	}

	public void setConversationRange(int conversationRange) {
		this.conversationRange = Math.max(2, Math.min(64, conversationRange));
	}

	public List<ZoneBehavior> getZoneBehaviors() {
		if (zoneBehaviors == null) {
			zoneBehaviors = new ArrayList<>();
		}
		return zoneBehaviors;
	}

	public void setZoneBehaviors(List<ZoneBehavior> zoneBehaviors) {
		this.zoneBehaviors = zoneBehaviors == null ? new ArrayList<>() : new ArrayList<>(zoneBehaviors);
	}

	@Override
	public String getConfigName() {
		return npcName.toLowerCase();
	}


    public static final StructEndec<NPCConfig> ENDEC = StructEndecBuilder.of(
			Endec.STRING.fieldOf("npcName", NPCConfig::getNpcName),
			Endec.STRING.fieldOf("uuid", config -> config.getUuid().toString()),
			Endec.BOOLEAN.fieldOf("isActive", NPCConfig::isActive),
			Endec.STRING.fieldOf("llmDefaultPrompt", NPCConfig::getLlmCharacter),
			Endec.forEnum(LLMType.class).fieldOf("llmType", NPCConfig::getLlmType),
            Endec.STRING.fieldOf("llmModel", NPCConfig::getLlmModel),
			Endec.BOOLEAN.fieldOf("isTTS", NPCConfig::isTTS),
			Endec.STRING.fieldOf("voiceId", NPCConfig::getVoiceId),
			Endec.STRING.fieldOf("skinUrl", NPCConfig::getSkinUrl),
			Endec.INT.fieldOf("conversationRange", NPCConfig::getConversationRange),
			MemoryFragment.ENDEC.listOf().fieldOf("memoryFragments", NPCConfig::getMemoryFragments),
			ZoneBehavior.ENDEC.listOf().fieldOf("zoneBehaviors", NPCConfig::getZoneBehaviors),
			NPCConfig::new
	);

    public static NPCConfig deepCopy(NPCConfig config) {
        return new NPCConfig(
                config.npcName,
                config.uuid.toString(),
                config.isActive,
                config.llmCharacter,
                config.llmType,
                config.llmModel,
                config.isTTS,
                config.voiceId,
                config.skinUrl,
				config.conversationRange,
				deepCopyMemoryFragments(config.getMemoryFragments()),
				deepCopyZoneBehaviors(config.getZoneBehaviors())
        );
    }

	private static List<MemoryFragment> deepCopyMemoryFragments(List<MemoryFragment> memoryFragments) {
		if (memoryFragments == null || memoryFragments.isEmpty()) {
			return Collections.emptyList();
		}
		List<MemoryFragment> copied = new ArrayList<>();
		for (MemoryFragment memoryFragment : memoryFragments) {
			copied.add(new MemoryFragment(
					memoryFragment.getId(),
					memoryFragment.getPrompt(),
					memoryFragment.isUnlocked()
			));
		}
		return copied;
	}

	private static List<ZoneBehavior> deepCopyZoneBehaviors(List<ZoneBehavior> zoneBehaviors) {
		if (zoneBehaviors == null || zoneBehaviors.isEmpty()) {
			return Collections.emptyList();
		}
		List<ZoneBehavior> copied = new ArrayList<>();
		for (ZoneBehavior zoneBehavior : zoneBehaviors) {
			copied.add(new ZoneBehavior(
					zoneBehavior.getName(),
					new ZoneCoordinate(
							zoneBehavior.getFrom().getX(),
							zoneBehavior.getFrom().getY(),
							zoneBehavior.getFrom().getZ()
					),
					new ZoneCoordinate(
							zoneBehavior.getTo().getX(),
							zoneBehavior.getTo().getY(),
							zoneBehavior.getTo().getZ()
					),
					zoneBehavior.getPriority(),
					zoneBehavior.getInstructions()
			));
		}
		return copied;
	}

	@Override
	public String toString() {
		return "NPCConfig{npcName=" + npcName +
				",uuid=" + uuid +
				",isActive=" + isActive +
				",llmType=" + llmType +
				",llmCharacter=" + llmCharacter +
				",memoryFragments=" + getMemoryFragments().size() +
				",conversationRange=" + conversationRange +
				",voiceId=" + voiceId +
				",zoneBehaviorCount=" + getZoneBehaviors().size() + "}";
	}

	public static class MemoryFragment {
		private String id = "memory_id";
		private String prompt = "";
		private boolean unlocked = false;

		public MemoryFragment() {}

		public MemoryFragment(
				String id,
				String prompt,
				boolean unlocked
		) {
			this.id = id == null ? "memory_id" : id;
			this.prompt = prompt == null ? "" : prompt;
			this.unlocked = unlocked;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id == null ? "memory_id" : id;
		}

		public String getPrompt() {
			return prompt;
		}

		public void setPrompt(String prompt) {
			this.prompt = prompt == null ? "" : prompt;
		}

		public boolean isUnlocked() {
			return unlocked;
		}

		public void setUnlocked(boolean unlocked) {
			this.unlocked = unlocked;
		}

		public String toSystemPrompt() {
			StringBuilder systemPrompt = new StringBuilder(getId());
			if (!getPrompt().isBlank()) {
				systemPrompt.append(": ").append(getPrompt());
			}
			return systemPrompt.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof MemoryFragment other)) {
				return false;
			}
			return unlocked == other.unlocked &&
					Objects.equals(id, other.id) &&
					Objects.equals(prompt, other.prompt);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, prompt, unlocked);
		}

		public static final StructEndec<MemoryFragment> ENDEC = StructEndecBuilder.of(
				Endec.STRING.fieldOf("id", MemoryFragment::getId),
				Endec.STRING.fieldOf("prompt", MemoryFragment::getPrompt),
				Endec.BOOLEAN.fieldOf("unlocked", MemoryFragment::isUnlocked),
				MemoryFragment::new
		);
	}

	public static class ZoneBehavior {
		private String name = "Zone";
		private ZoneCoordinate from = new ZoneCoordinate();
		private ZoneCoordinate to = new ZoneCoordinate();
		private int priority = 0;
		private String instructions = "";

		public ZoneBehavior() {}

		public ZoneBehavior(
				String name,
				ZoneCoordinate from,
				ZoneCoordinate to,
				int priority,
				String instructions
		) {
			this.name = name;
			this.from = from == null ? new ZoneCoordinate() : from;
			this.to = to == null ? new ZoneCoordinate() : to;
			this.priority = priority;
			this.instructions = instructions == null ? "" : instructions;
		}

		public boolean contains(BlockPos position) {
			int minX = Math.min(getFrom().getX(), getTo().getX());
			int maxX = Math.max(getFrom().getX(), getTo().getX());
			int minY = Math.min(getFrom().getY(), getTo().getY());
			int maxY = Math.max(getFrom().getY(), getTo().getY());
			int minZ = Math.min(getFrom().getZ(), getTo().getZ());
			int maxZ = Math.max(getFrom().getZ(), getTo().getZ());
			return position.getX() >= minX && position.getX() <= maxX &&
					position.getY() >= minY && position.getY() <= maxY &&
					position.getZ() >= minZ && position.getZ() <= maxZ;
		}

		public String getName() {
			return name;
		}

		public ZoneCoordinate getFrom() {
			if (from == null) {
				from = new ZoneCoordinate();
			}
			return from;
		}

		public ZoneCoordinate getTo() {
			if (to == null) {
				to = new ZoneCoordinate();
			}
			return to;
		}

		public int getPriority() {
			return priority;
		}

		public String getInstructions() {
			return instructions;
		}

		public void setName(String name) {
			this.name = name == null ? "Zone" : name;
		}

		public void setFrom(ZoneCoordinate from) {
			this.from = from == null ? new ZoneCoordinate() : from;
		}

		public void setTo(ZoneCoordinate to) {
			this.to = to == null ? new ZoneCoordinate() : to;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public void setInstructions(String instructions) {
			this.instructions = instructions == null ? "" : instructions;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ZoneBehavior other)) {
				return false;
			}
			return priority == other.priority &&
					Objects.equals(name, other.name) &&
					Objects.equals(from, other.from) &&
					Objects.equals(to, other.to) &&
					Objects.equals(instructions, other.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, from, to, priority, instructions);
		}

		public static final StructEndec<ZoneBehavior> ENDEC = StructEndecBuilder.of(
				Endec.STRING.fieldOf("name", ZoneBehavior::getName),
				ZoneCoordinate.ENDEC.fieldOf("from", ZoneBehavior::getFrom),
				ZoneCoordinate.ENDEC.fieldOf("to", ZoneBehavior::getTo),
				Endec.INT.fieldOf("priority", ZoneBehavior::getPriority),
				Endec.STRING.fieldOf("instructions", ZoneBehavior::getInstructions),
				ZoneBehavior::new
		);
	}

	public static class ZoneCoordinate {
		private int x = 0;
		private int y = 0;
		private int z = 0;

		public ZoneCoordinate() {}

		public ZoneCoordinate(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getZ() {
			return z;
		}

		public void setX(int x) {
			this.x = x;
		}

		public void setY(int y) {
			this.y = y;
		}

		public void setZ(int z) {
			this.z = z;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ZoneCoordinate other)) {
				return false;
			}
			return x == other.x && y == other.y && z == other.z;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y, z);
		}

		public static final StructEndec<ZoneCoordinate> ENDEC = StructEndecBuilder.of(
				Endec.INT.fieldOf("x", ZoneCoordinate::getX),
				Endec.INT.fieldOf("y", ZoneCoordinate::getY),
				Endec.INT.fieldOf("z", ZoneCoordinate::getZ),
				ZoneCoordinate::new
		);
	}
}

package me.sailex.secondbrain.util;

import me.sailex.secondbrain.auth.PlayerAuthorizer;
import me.sailex.secondbrain.config.ConfigProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class LogUtil {

	private static MinecraftServer server;
	private static ConfigProvider configProvider;

	public static void initialize(MinecraftServer server, ConfigProvider configProvider) {
		LogUtil.server = server;
		LogUtil.configProvider = configProvider;
	}

	private LogUtil() {}

	private static final Logger LOGGER = LogManager.getLogger(LogUtil.class);
	private static final MutableText PREFIX = Text.literal("[SecondBrain] ").formatted(Formatting.DARK_PURPLE);
    private static final Object WARN_RATE_LIMIT_LOCK = new Object();
    private static final Map<String, Long> WARN_LAST_LOGGED_AT_MS = new HashMap<>();

	private static MutableText formatDebug(String message) {
		return Text.literal(PREFIX.getString()).append(message).setStyle(Style.EMPTY.withFormatting(Formatting.DARK_GRAY));
	}

	public static MutableText formatInfo(String message) {
		return Text.literal("").append(PREFIX).append(message);
	}

	public static MutableText formatError(String message) {
		return Text.literal("").append(PREFIX).append(message).setStyle(Style.EMPTY.withFormatting(Formatting.RED));
	}

	public static String formatExceptionMessage(String message) {
		int messageBegin = message.indexOf(": ");
		if (messageBegin != -1) {
			return message.substring(messageBegin + 1);
		}
		return message;
	}

	public static void debugInChat(String message) {
		log(formatDebug(message));
	}

	public static void infoInChat(String message) {
		log(formatInfo(message));
	}

	public static void errorInChat(String message) {
		log(formatError(formatExceptionMessage(message)));
	}

	public static void info(String message) {
		if (isVerboseEnabled()) LOGGER.info(formatInfo(message).getString());
	}

    public static boolean isVerboseEnabled() {
        return configProvider != null
                && configProvider.getBaseConfig() != null
                && configProvider.getBaseConfig().isVerbose();
    }

    public static void warn(String message) {
        LOGGER.warn(formatInfo(message).getString());
    }

    public static void warnRateLimited(String key, String message, long minIntervalMs) {
        if (!isVerboseEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean shouldLog;
        synchronized (WARN_RATE_LIMIT_LOCK) {
            Long lastLoggedAt = WARN_LAST_LOGGED_AT_MS.get(key);
            shouldLog = lastLoggedAt == null || now - lastLoggedAt >= minIntervalMs;
            if (shouldLog) {
                WARN_LAST_LOGGED_AT_MS.put(key, now);
            }
        }
        if (shouldLog) {
            warn(message);
        }
    }

	public static void error(String message) {
		LOGGER.error(formatError(message).getString());
	}

	public static void error(String message, Throwable e) {
		LOGGER.error(formatError(message).getString(), e);
	}

	public static void error(Throwable e) {
		LOGGER.error(e.getMessage(), e);
	}

	private static void log(MutableText formattedMessage) {
		if (server != null) {
			server.getPlayerManager().getPlayerList().stream()
					.filter(PlayerAuthorizer::hasOperatorPermission)
					.forEach(player -> player.sendMessage(formattedMessage, false));
		} else {
			LOGGER.error("{}server is null - cant log to ingame chat!", PREFIX.getString());
		}
	}
}

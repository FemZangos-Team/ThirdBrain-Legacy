package me.sailex.secondbrain.auth;

import io.wispforest.owo.network.ServerAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerAuthorizer {

    /**
     * Checks whether a command source has operator-level permissions, using the version-appropriate API.
     *
     * @param source the command source being evaluated
     * @return true when the source is allowed to run operator-only actions
     */
    public static boolean hasOperatorPermission(ServerCommandSource source) {
        /*? >=1.21.11 {*/
        /*return net.minecraft.server.command.CommandManager.MODERATORS_CHECK.allows(source.getPermissions());
        *//*?} else {*/
        return source.hasPermissionLevel(2);
        /*?}*/
    }

    /**
     * Checks whether a player has operator-level permissions, using the version-appropriate API.
     *
     * @param player the player being evaluated
     * @return true when the player is allowed to receive operator-only actions and messages
     */
    public static boolean hasOperatorPermission(ServerPlayerEntity player) {
        /*? >=1.21.11 {*/
        /*return net.minecraft.server.command.CommandManager.MODERATORS_CHECK.allows(player.getPermissions());
        *//*?} else {*/
        return player.hasPermissionLevel(2);
        /*?}*/
    }

    /**
     * The player is considered authorized if they have the "operator" permission level.
     *
     * @param serverAccess the context providing server-related access, including the player's details
     * @return true if the player is authorized, otherwise false
     */
    public boolean isAuthorized(ServerAccess serverAccess) {
        return hasOperatorPermission(serverAccess.player());
    }

    public boolean isLocalConnection(ServerAccess serverAccess) {
        String address = serverAccess.player().networkHandler.getConnectionAddress().toString();
        return serverAccess.runtime().isSingleplayer() || address.equals("127.0.0.1");
    }

}

package whisperlyric.carpetpermissions;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carpet Permissions — adds LuckPerms/fabric-permissions-api based
 * permission checks to Carpet mod commands.
 *
 * <p>Ported to Minecraft 26.1.2 (unobfuscated, Mojang official mappings).</p>
 *
 * <h3>How to use</h3>
 * <ol>
 *   <li>Install LuckPerms (or any fabric-permissions-api compatible provider).</li>
 *   <li>Start the server. A player executes a Carpet command.</li>
 *   <li>Check the server log for the exact permission node being checked,
 *       e.g. {@code carpet.commands.PlayerCommand}.</li>
 *   <li>Grant the permission in LuckPerms:
 *       {@code /lp user <player> permission set carpet.commands.PlayerCommand true}</li>
 * </ol>
 */
public class CarpetPermissions implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("carpet-permissions");

    @Override
    public void onInitialize() {
        LOGGER.info("[carpet-permissions] Loaded! Permission checks active for Carpet commands.");
        LOGGER.info("[carpet-permissions] Players must have explicit LuckPerms permissions to use Carpet commands.");
        LOGGER.info("[carpet-permissions] Console and command blocks use Carpet's default permission system.");
        LOGGER.info("[carpet-permissions] Check the server log when a player runs a Carpet command to see the required permission node.");
    }
}

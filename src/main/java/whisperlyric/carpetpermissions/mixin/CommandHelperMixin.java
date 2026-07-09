package whisperlyric.carpetpermissions.mixin;

import carpet.utils.CommandHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/**
 * Mixin into Carpet's {@link CommandHelper#canUseCommand} to inject
 * permission checks via fabric-permissions-api.
 *
 * <p>Minecraft 26.1.2 — unobfuscated Mojang mappings.
 * Carpet 26.1 delegates all {@code canUseCommand} calls through
 * {@code CommandHelper}, so intercepting here covers everything.</p>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Derive the permission node from the calling command class name
 *       (via stack trace). The node looks like {@code carpet.commands.PlayerCommand}.</li>
 *   <li>If the source is a player, call {@code Permissions.check(player, node)}
 *       instead of Carpet's built-in level check.</li>
 *   <li>Non-player sources (console, command blocks) fall through to
 *       Carpet's original logic.</li>
 * </ol>
 *
 * <h3>Configuring permissions in LuckPerms</h3>
 * Grant a player or group a permission node matching the command class:
 * <pre>{@code
 * /lp group default permission set carpet.commands.PlayerCommand true
 * /lp group default permission set carpet.commands.InfoCommand true
 * }</pre>
 * Check the server log after a player runs a command — the exact
 * permission node being checked is logged at INFO level.
 */
@Mixin(CommandHelper.class)
public class CommandHelperMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("carpet-permissions");

    /**
     * Intercept {@code CommandHelper.canUseCommand} at the very beginning.
     *
     * <p>When the source is a player, this replaces Carpet's default
     * command-level check with a fabric-permissions-api check against
     * the calling command's class name.</p>
     *
     * @param source       the command source ({@code CommandSourceStack})
     * @param commandLevel Carpet's raw command level value
     *                     (Boolean, "true", "false", "ops", "0"-"4", etc.)
     * @param cir          Mixin callback; set return value to short-circuit
     */
    @Inject(
        method = "canUseCommand",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void injectPermissionCheck(
            CommandSourceStack source,
            Object commandLevel,
            CallbackInfoReturnable<Boolean> cir) {

        // Non-player sources: let Carpet handle it normally
        if (!source.isPlayer()) {
            return;
        }

        // Derive the permission node from the calling Carpet command class.
        // Stack trace layout:
        //   [0] = java.lang.Thread.getStackTrace()
        //   [1] = this mixin method
        //   [2] = CommandHelper.canUseCommand()  (original, redirected by Mixin)
        //   [3] = the Carpet command class that called canUseCommand
        String permissionNode;
        try {
            permissionNode = Thread.currentThread().getStackTrace()[3].getClassName();
        } catch (Exception e) {
            LOGGER.warn("[carpet-permissions] Failed to derive permission node from stack trace", e);
            return; // fall through to Carpet's original logic
        }

        // Resolve the player entity
        Entity player;
        try {
            player = Objects.requireNonNull(source.getPlayer());
        } catch (Exception e) {
            LOGGER.warn("[carpet-permissions] source.isPlayer()=true but getPlayer() returned null", e);
            return; // fall through
        }

        // Perform the permission check via fabric-permissions-api
        boolean allowed;
        try {
            allowed = Permissions.check(player, permissionNode);
        } catch (Exception e) {
            LOGGER.error("[carpet-permissions] Permissions.check() threw for node '{}'", permissionNode, e);
            return; // fall through
        }

        // Log the result so server admins can configure LuckPerms accordingly
        if (allowed) {
            LOGGER.info("[carpet-permissions] ALLOWED  | {} | node='{}'",
                    player.getName().getString(), permissionNode);
        } else {
            LOGGER.info("[carpet-permissions] DENIED   | {} | node='{}' | Grant in LuckPerms: /lp user {} permission set {} true",
                    player.getName().getString(), permissionNode,
                    player.getName().getString(), permissionNode);
        }

        cir.setReturnValue(allowed);
    }
}

package whisperlyric.carpetpermissions.mixin;

import carpet.utils.CommandHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/**
 * Mixin into Carpet's {@link CommandHelper#canUseCommand} to inject
 * permission checks via fabric-permissions-api.
 *
 * <h3>26.1.2 Porting Notes</h3>
 * <ul>
 *   <li>Minecraft 26.1.2 is <b>unobfuscated</b> — Mojang official names are used.</li>
 *   <li>{@code ServerCommandSource} (Yarn) → {@code CommandSourceStack} (Mojang official).</li>
 *   <li>{@code isExecutedByPlayer()} (Yarn) → {@code isPlayer()} (Mojang official).</li>
 *   <li>Carpet's own API ({@code carpet.utils.CommandHelper}) retains its package name.</li>
 * </ul>
 *
 * <h3>Uncertainties (verify against actual 26.1.2 runtime)</h3>
 * <ul>
 *   <li>The {@code canUseCommand} method signature: the second parameter
 *       ({@code Object commandLevel}) may have changed to a typed parameter
 *       in newer Carpet versions. Check the actual Carpet JAR for 26.1.2.</li>
 *   <li>{@code Permissions.check()} in fabric-permissions-api v0.7.0 may have
 *       a {@code CommandSourceStack} overload — consider using
 *       {@code Permissions.check(source, permission)} if available.</li>
 *   <li>The stack-trace-based permission string derivation (index [3]) depends
 *       on the exact call-chain depth in Carpet 26.1.2. Test with actual
 *       Carpet commands to verify the index is correct.</li>
 * </ul>
 */
@Mixin(CommandHelper.class)
public class CommandHelperMixin {

    /**
     * Injects a permission check at the head of {@code CommandHelper.canUseCommand}.
     *
     * <p>When a player executes a Carpet command, this replaces the default
     * command-level check with a permissions-API check. The permission node
     * is derived from the calling command class's fully-qualified name.</p>
     *
     * <p>Non-player sources (console, command blocks) pass through unchanged.</p>
     *
     * @param source       the command source (Mojang: {@code CommandSourceStack})
     * @param commandLevel the Carpet command level (type may vary — check 26.1.2 JAR)
     * @param cir          mixin callback; set return value to short-circuit
     */
    @Inject(
        method = "canUseCommand",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void canUseCommand(
            CommandSourceStack source,
            Object commandLevel,
            CallbackInfoReturnable<Boolean> cir) {

        // Derive the permission node from the calling command's class name.
        // Stack trace at depth [3] should be the Carpet command class that
        // called CommandHelper.canUseCommand().
        //   [0] = Thread.getStackTrace()
        //   [1] = CommandHelperMixin.canUseCommand()  (this mixin)
        //   [2] = CommandHelper.canUseCommand()       (target, via mixin dispatch)
        //   [3] = <calling carpet command class>
        String permissionString = Thread.currentThread().getStackTrace()[3].getClassName();

        // Only apply permission checks to player sources.
        // isPlayer() is the Mojang-official equivalent of Yarn's isExecutedByPlayer().
        if (source.isPlayer()) {
            // Permissions.check(Entity, String) — fabric-permissions-api v0.6.1 / v0.7.0
            // If v0.7.0 provides Permissions.check(CommandSourceStack, String),
            // use that overload instead for better type safety:
            //   cir.setReturnValue(Permissions.check(source, permissionString));
            cir.setReturnValue(
                Permissions.check(
                    (Entity) Objects.requireNonNull(source.getPlayer()),
                    permissionString
                )
            );
        }
    }
}

package whisperlyric.carpetpermissions;

import net.fabricmc.api.ModInitializer;

/**
 * Carpet Permissions — adds permission checks to Carpet mod commands
 * via fabric-permissions-api.
 *
 * Ported to Minecraft 26.1.2 (unobfuscated, Mojang official mappings).
 */
public class CarpetPermissions implements ModInitializer {

    @Override
    public void onInitialize() {
        // Initialization is handled by the mixin.
        // No additional setup needed — fabric-permissions-api
        // auto-registers with any available permission provider.
    }
}

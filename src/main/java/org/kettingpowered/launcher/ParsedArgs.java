package org.kettingpowered.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author C0D3 M4513R
 * @param args list of unparsed args
 * @param installOnly Only install the server. Don't start it.
 * @param enableServerUpdator should the server be updated
 * @param enableLauncherUpdator should the launcher (this!) be updated
 * @param launchTarget what launchTarget will we pass to forge?
 * @param minecraftVersion what minecraftVersion should the server be?
 * @param forgeVersion what forgeVersion should the server be?
 * @param kettingVersion what kettingVersion should the server be?
 */
public record ParsedArgs(
        @NotNull List<String> args,
        boolean installOnly,
        boolean enableServerUpdator,
        boolean enableLauncherUpdator,
        @Nullable String launchTarget,
        @Nullable String minecraftVersion,
        @Nullable String forgeVersion,
        @Nullable String kettingVersion
) {}

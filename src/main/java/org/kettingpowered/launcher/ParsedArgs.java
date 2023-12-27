package org.kettingpowered.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ParsedArgs(@NotNull List<String> args, boolean enableServerUpdator, boolean enableLauncherUpdator, @NotNull String launchTarget, @Nullable String minecraftVersion) {
}

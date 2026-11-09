package com.fuelingimagination.content.artisan;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ArtisanModeHandlers {
    private static final ArtisanModeHandler EMPTY = new ArtisanModeHandler() { };
    private static final Map<ArtisanWorkMode, ArtisanModeHandler> HANDLERS = new EnumMap<>(ArtisanWorkMode.class);

    static {
        HANDLERS.put(ArtisanWorkMode.MANUFACTURE, new ArtisanCraftingHandler());
        HANDLERS.put(ArtisanWorkMode.REPAIR, new DamagedBlockRepairHandler());
        HANDLERS.put(ArtisanWorkMode.MODIFY, new ModificationHandler());
    }

    /** Register during common setup via enqueueWork. Handlers are shared; store per-table data in the BE. */
    public static void register(ArtisanWorkMode mode, ArtisanModeHandler handler) {
        HANDLERS.put(Objects.requireNonNull(mode), Objects.requireNonNull(handler));
    }

    public static ArtisanModeHandler get(ArtisanWorkMode mode) {
        return HANDLERS.getOrDefault(mode, EMPTY);
    }

    public static boolean hasHandler(ArtisanWorkMode mode) { return HANDLERS.containsKey(mode); }

    private ArtisanModeHandlers() { }
}

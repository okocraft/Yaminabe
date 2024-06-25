package net.okocraft.yaminabe.core.modules;

import net.kyori.adventure.key.Key;
import net.okocraft.yaminabe.core.module.YaminabeModule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Modules {

    public static final Map<Key, YaminabeModule.Factory> FACTORIES;

    static {
        var factories = new LinkedHashMap<Key, YaminabeModule.Factory>();

        FACTORIES = Collections.unmodifiableMap(factories);
    }
}

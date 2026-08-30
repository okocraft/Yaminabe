package net.okocraft.yaminabe.common;

import java.util.function.Consumer;

public interface YaminabeReloader {

    void reload(Consumer<Notification> consumer);

    enum Notification {
        CONFIG_RELOADED,
        FAILED_TO_RELOAD_CONFIG,
        LANGUAGE_RELOADED,
        FAILED_TO_RELOAD_LANGUAGES
    }
}

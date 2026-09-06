package net.okocraft.yaminabe.common.restart.countdown;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface RestartCountdownAudience {

    void showBossBar(BossBar bossBar);

    void hideBossBar(BossBar bossBar);

    void sendMessage(Component message);
}

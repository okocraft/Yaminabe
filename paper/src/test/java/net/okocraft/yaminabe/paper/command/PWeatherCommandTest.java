package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.WeatherType;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PWeatherCommandTest {

    private static final String PERMISSION = "yaminabe.command.pweather";
    private static final String SET_PERMISSION = PERMISSION + ".set";
    private static final String RESET_PERMISSION = PERMISSION + ".reset";
    private static final String QUERY_PERMISSION = PERMISSION + ".query";

    private final CommandTester tester = CommandTester.of(PWeatherCommand.createPWeatherCommand());

    private Player player;
    private CommandSourceStack source;

    @BeforeEach
    void setUp() {
        this.player = Mockito.mock(Player.class);
        Mockito.when(this.player.getName()).thenReturn("Player");
        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION, SET_PERMISSION, RESET_PERMISSION, QUERY_PERMISSION);
    }

    @Test
    void testClearWeatherIsFixed() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather clear"));

        Mockito.verify(this.player).setPlayerWeather(WeatherType.CLEAR);
        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_SET.apply("clear", "Player"));
    }

    @Test
    void testRainWeatherIsFixed() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather rain"));

        Mockito.verify(this.player).setPlayerWeather(WeatherType.DOWNFALL);
        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_SET.apply("rain", "Player"));
    }

    @Test
    void testThunderIsNotSupported() {
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "pweather thunder"));
    }

    @Test
    void testReset() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather reset"));

        Mockito.verify(this.player).resetPlayerWeather();
        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_RESET.apply("Player"));
    }

    @Test
    void testQueryNormalWeather() throws Exception {
        Mockito.when(this.player.getPlayerWeather()).thenReturn(null);

        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_QUERY_NORMAL.apply("Player"));
    }

    @Test
    void testQueryClearWeather() throws Exception {
        Mockito.when(this.player.getPlayerWeather()).thenReturn(WeatherType.CLEAR);

        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_QUERY_FIXED.apply("Player", "clear"));
    }

    @Test
    void testQueryRainWeather() throws Exception {
        Mockito.when(this.player.getPlayerWeather()).thenReturn(WeatherType.DOWNFALL);

        Assertions.assertEquals(1, this.tester.execute(this.source, "pweather query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PWEATHER_QUERY_FIXED.apply("Player", "rain"));
    }

    @Test
    void testConsoleMustSpecifyTargets() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION, SET_PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "pweather clear"));
        Mockito.verify(console).sendMessage(CommandMessages.PWEATHER_TARGET_REQUIRED);
    }

    @Test
    void testSetIsHiddenWithoutSetPermission() {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, PERMISSION);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(TestSources.of(player), "pweather clear"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testCommandIsHiddenWithoutRootPermission() {
        Player player = Mockito.mock(Player.class);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(TestSources.of(player), "pweather query"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PTimeCommandTest {

    private static final String PERMISSION = "yaminabe.command.ptime";
    private static final String SET_PERMISSION = PERMISSION + ".set";
    private static final String RESET_PERMISSION = PERMISSION + ".reset";
    private static final String QUERY_PERMISSION = PERMISSION + ".query";

    private final CommandTester tester = CommandTester.of(PTimeCommand.createPTimeCommand());

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
    void testNumericTimeIsFixed() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set 6000"));

        Mockito.verify(this.player).setPlayerTime(6000, false);
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_SET.apply("6000t", "Player"));
    }

    @Test
    void testTimeIsNormalizedToOneDay() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set 1d"));

        Mockito.verify(this.player).setPlayerTime(0, false);
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_SET.apply("0t", "Player"));
    }

    @Test
    void testVanillaTimeMarkersAreFixed() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set day"));
        Mockito.verify(this.player).setPlayerTime(1000, false);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set noon"));
        Mockito.verify(this.player).setPlayerTime(6000, false);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set night"));
        Mockito.verify(this.player).setPlayerTime(13000, false);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set midnight"));
        Mockito.verify(this.player).setPlayerTime(18000, false);
    }

    @Test
    void testReset() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime reset"));

        Mockito.verify(this.player).resetPlayerTime();
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_RESET.apply("Player"));
    }

    @Test
    void testQueryNormalTime() throws Exception {
        Mockito.when(this.player.isPlayerTimeRelative()).thenReturn(true);
        Mockito.when(this.player.getPlayerTimeOffset()).thenReturn(0L);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_QUERY_NORMAL.apply("Player"));
    }

    @Test
    void testQueryFixedTime() throws Exception {
        Mockito.when(this.player.isPlayerTimeRelative()).thenReturn(false);
        Mockito.when(this.player.getPlayerTimeOffset()).thenReturn(18000L);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_QUERY_FIXED.apply("Player", "18000t"));
    }

    @Test
    void testQueryRelativeTimeSetByAnotherPlugin() throws Exception {
        Mockito.when(this.player.isPlayerTimeRelative()).thenReturn(true);
        Mockito.when(this.player.getPlayerTimeOffset()).thenReturn(5000L);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_QUERY_RELATIVE.apply("Player", "+5000t"));
    }

    @Test
    void testConsoleMustSpecifyTargets() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION, SET_PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "ptime set day"));
        Mockito.verify(console).sendMessage(CommandMessages.PTIME_TARGET_REQUIRED);
    }

    @Test
    void testSetIsHiddenWithoutSetPermission() {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, PERMISSION);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(TestSources.of(player), "ptime set day"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testCommandIsHiddenWithoutRootPermission() {
        Player player = Mockito.mock(Player.class);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(TestSources.of(player), "ptime query"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

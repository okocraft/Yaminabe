package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class PTimeCommandTest {

    private static final String PERMISSION = "yaminabe.command.ptime";
    private static final String SET_PERMISSION = PERMISSION + ".set";
    private static final String SET_OTHERS_PERMISSION = SET_PERMISSION + ".others";
    private static final String RESET_PERMISSION = PERMISSION + ".reset";
    private static final String QUERY_PERMISSION = PERMISSION + ".query";

    private static final EntityScheduler DIRECT_SCHEDULER = (entity, task) -> {
        task.run();
        return true;
    };

    private final CommandTester tester = CommandTester.of(PTimeCommand.createPTimeCommand(DIRECT_SCHEDULER));

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
    void testTimeIsNormalizedToVisualDay() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime set 1d"));

        Mockito.verify(this.player).setPlayerTime(0, false);
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_SET.apply("0t", "Player"));
    }

    @Test
    void testVanillaTimeMarkersAreMapped() throws Exception {
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
    void testResetRestoresWorldTime() throws Exception {
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
    void testQueryRelativeTimeSetOutsideYaminabe() throws Exception {
        Mockito.when(this.player.isPlayerTimeRelative()).thenReturn(true);
        Mockito.when(this.player.getPlayerTimeOffset()).thenReturn(5000L);

        Assertions.assertEquals(1, this.tester.execute(this.source, "ptime query"));

        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_QUERY_RELATIVE.apply("Player", "+5000t"));
    }

    @Test
    void testNonPlayerExecutorMustSpecifyTargets() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION, SET_PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "ptime set day"));
        Mockito.verify(console).sendMessage(CommandMessages.PTIME_TARGET_REQUIRED);
    }

    @Test
    void testTargetingAnotherPlayerRequiresOthersPermission() {
        Player other = Mockito.mock(Player.class);

        Assertions.assertFalse(PTimeCommand.canTargetOthers(this.source, List.of(this.player, other), SET_OTHERS_PERMISSION));
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_OTHERS_PREVENTED);
    }

    @Test
    void testTargetingAnotherPlayerIsAllowedWithOthersPermission() {
        Player other = Mockito.mock(Player.class);
        TestSources.grant(this.player, SET_OTHERS_PERMISSION);

        Assertions.assertTrue(PTimeCommand.canTargetOthers(this.source, List.of(this.player, other), SET_OTHERS_PERMISSION));
        Mockito.verify(this.player, Mockito.never()).sendMessage(CommandMessages.PTIME_OTHERS_PREVENTED);
    }

    @Test
    void testExecuteAsAnotherPlayerStillRequiresOthersPermission() {
        Player executor = Mockito.mock(Player.class);
        CommandSourceStack executeAsSource = TestSources.of(this.player, executor);

        Assertions.assertFalse(PTimeCommand.canTargetOthers(executeAsSource, List.of(executor), SET_OTHERS_PERMISSION));
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_OTHERS_PREVENTED);
    }

    @Test
    void testNonPlayerSenderAlwaysNeedsOthersPermission() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Player executor = Mockito.mock(Player.class);
        CommandSourceStack consoleSource = TestSources.of(console, executor);

        Assertions.assertFalse(PTimeCommand.canTargetOthers(consoleSource, List.of(executor), SET_OTHERS_PERMISSION));
        Mockito.verify(console).sendMessage(CommandMessages.PTIME_OTHERS_PREVENTED);
    }

    @Test
    void testRetiredPlayerIsNotModifiedOrReportedAsSuccess() throws Exception {
        EntityScheduler scheduler = Mockito.mock(EntityScheduler.class);
        CommandTester tester = CommandTester.of(PTimeCommand.createPTimeCommand(scheduler));

        Assertions.assertEquals(0, tester.execute(this.source, "ptime set day"));

        Mockito.verify(scheduler).execute(Mockito.eq(this.player), Mockito.any(Runnable.class));
        Mockito.verify(this.player, Mockito.never()).setPlayerTime(Mockito.anyLong(), Mockito.anyBoolean());
        Mockito.verify(this.player).sendMessage(CommandMessages.PTIME_NO_TARGETS);
    }

    @Test
    void testSubcommandsAreHiddenWithoutTheirPermissions() {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, PERMISSION);
        CommandSourceStack source = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(source, "ptime set day"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(source, "ptime reset"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(source, "ptime query"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testCommandIsHiddenWithoutRootPermission() {
        Player player = Mockito.mock(Player.class);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(TestSources.of(player), "ptime query"));
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

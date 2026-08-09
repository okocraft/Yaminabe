package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

class WorkstationCommandsTest {

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testMenuIsOpened(WorkstationCommands workstation) throws Exception {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, workstation.getPermission());
        InventoryView menu = Mockito.mock(InventoryView.class);

        CommandTester tester = testerOpening(workstation, player, menu);

        Assertions.assertEquals(1, tester.execute(TestSources.of(player), workstation.getCommandName()));

        Mockito.verify(player).openInventory(menu);
        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testConsoleCannotOpenMenu(WorkstationCommands workstation) throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, workstation.getPermission());

        CommandTester tester = testerFailingToOpen(workstation);

        Assertions.assertEquals(0, tester.execute(TestSources.ofSenderOnly(console), workstation.getCommandName()));

        Mockito.verify(console).sendMessage(workstation.getPlayerOnlyMessage().apply("/" + workstation.getCommandName()));
    }

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testCommandTakesNoArgument(WorkstationCommands workstation) {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, workstation.getPermission());

        CommandTester tester = testerFailingToOpen(workstation);
        CommandSourceStack source = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> tester.execute(source, workstation.getCommandName() + " now"));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testCommandIsHiddenWithUnsetPermission(WorkstationCommands workstation) {
        assertHidden(workstation, Mockito.mock(Player.class));
    }

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testCommandIsHiddenWithDeniedPermission(WorkstationCommands workstation) {
        Player player = Mockito.mock(Player.class);
        TestSources.deny(player, workstation.getPermission());

        assertHidden(workstation, player);
    }

    @ParameterizedTest
    @EnumSource(WorkstationCommands.class)
    void testCommandIsHiddenWithAnotherPermission(WorkstationCommands workstation) {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, "yaminabe.command");

        assertHidden(workstation, player);
    }

    private static void assertHidden(WorkstationCommands workstation, Player player) {
        CommandTester tester = testerFailingToOpen(workstation);
        CommandSourceStack hiddenSource = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> tester.execute(hiddenSource, workstation.getCommandName()));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
        Mockito.verify(player, Mockito.never()).openInventory(Mockito.any(InventoryView.class));
    }

    /**
     * The real menu cannot be made here, as building it needs a player backed by a live server, so what the command
     * opens is a stand-in.
     */
    private static CommandTester testerOpening(WorkstationCommands workstation, Player expectedPlayer, InventoryView menu) {
        return CommandTester.of(workstation.createCommand(player -> {
            Assertions.assertSame(expectedPlayer, player);
            return menu;
        }));
    }

    private static CommandTester testerFailingToOpen(WorkstationCommands workstation) {
        return CommandTester.of(workstation.createCommand(player -> Assertions.fail("no menu should be opened")));
    }
}

package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

class DisposalCommandTest {

    private static final String PERMISSION = "yaminabe.command.disposal";

    private final InventoryView menu = Mockito.mock(InventoryView.class);
    private final List<Component> requestedTitles = new ArrayList<>();

    /**
     * The real menu cannot be made here, as building it needs a player backed by a live server, so what the command
     * opens is a stand-in that records what it was asked for.
     */
    private final CommandTester tester = CommandTester.of(DisposalCommand.createDisposalCommand((player, title) -> {
        Assertions.assertSame(this.player, player);
        this.requestedTitles.add(title);
        return this.menu;
    }));

    private Player player;
    private CommandSourceStack source;

    @BeforeEach
    void setUp() {
        this.player = Mockito.mock(Player.class);
        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION);
    }

    @Test
    void testDisposalMenuIsOpened() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "disposal"));

        Assertions.assertEquals(List.of(CommandMessages.DISPOSAL_TITLE.asComponent()), this.requestedTitles);
        Mockito.verify(this.player).sendMessage(CommandMessages.DISPOSAL_OPENING);
        Mockito.verify(this.player).openInventory(this.menu);
    }

    @Test
    void testConsoleCannotOpenDisposalMenu() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "disposal"));

        Mockito.verify(console).sendMessage(CommandMessages.DISPOSAL_PLAYER_ONLY.apply("/disposal"));
    }

    @Test
    void testCommandTakesNoArgument() {
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "disposal now"));

        Mockito.verify(this.player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testCommandIsHiddenWithUnsetPermission() {
        this.assertHidden(Mockito.mock(Player.class));
    }

    @Test
    void testCommandIsHiddenWithDeniedPermission() {
        Player player = Mockito.mock(Player.class);
        TestSources.deny(player, PERMISSION);

        this.assertHidden(player);
    }

    @Test
    void testCommandIsHiddenWithAnotherPermission() {
        Player player = Mockito.mock(Player.class);
        TestSources.grant(player, "yaminabe.command");

        this.assertHidden(player);
    }

    private void assertHidden(Player player) {
        CommandSourceStack hiddenSource = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "disposal"));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
        Mockito.verify(player, Mockito.never()).openInventory(Mockito.any(InventoryView.class));
    }
}

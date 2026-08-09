package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class HatCommandTest {

    private static final String PERMISSION = "yaminabe.command.hat";
    private static final String ALLOW_DIAMOND_BLOCK = PERMISSION + ".allow-type.diamond_block";
    private static final String IGNORE_BINDING = PERMISSION + ".ignore-binding";

    private static final ItemStack HAT = ItemStack.of(Material.DIAMOND_BLOCK);
    private static final ItemStack WORN = ItemStack.of(Material.GOLD_BLOCK);

    private static ItemStack bindingCursed() {
        ItemStack item = ItemStack.of(Material.GOLD_BLOCK);
        item.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);
        return item;
    }

    private final CommandTester tester = CommandTester.of(HatCommand.createHatCommand());

    private Player player;
    private PlayerInventory inventory;
    private CommandSourceStack source;

    @BeforeEach
    void setUp() {
        this.player = Mockito.mock(Player.class);
        this.inventory = Mockito.mock(PlayerInventory.class);

        Mockito.when(this.player.getInventory()).thenReturn(this.inventory);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(ItemStack.empty());
        Mockito.when(this.inventory.getHelmet()).thenReturn(ItemStack.empty());

        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION);
    }

    @Test
    void testItemInHandIsWorn() throws Exception {
        TestSources.grant(this.player, ALLOW_DIAMOND_BLOCK);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(HAT);
        Mockito.when(this.inventory.getHelmet()).thenReturn(WORN);

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory).setHelmet(HAT);
        Mockito.verify(this.inventory).setItemInMainHand(WORN);
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_PLACED);
    }

    @Test
    void testEmptyHandIsNotWorn() throws Exception {
        TestSources.grant(this.player, ALLOW_DIAMOND_BLOCK);

        Assertions.assertEquals(0, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory, Mockito.never()).setHelmet(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_FAIL);
    }

    @Test
    void testItemOfDisallowedTypeIsNotWorn() throws Exception {
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(HAT);

        Assertions.assertEquals(0, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory, Mockito.never()).setHelmet(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_PREVENTED);
    }

    @Test
    void testBindingCursedHatIsNotReplaced() throws Exception {
        TestSources.grant(this.player, ALLOW_DIAMOND_BLOCK);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(HAT);
        Mockito.when(this.inventory.getHelmet()).thenReturn(bindingCursed());

        Assertions.assertEquals(0, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory, Mockito.never()).setHelmet(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_CURSE);
    }

    @Test
    void testBindingCursedHatIsReplacedWithPermission() throws Exception {
        TestSources.grant(this.player, ALLOW_DIAMOND_BLOCK, IGNORE_BINDING);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(HAT);
        Mockito.when(this.inventory.getHelmet()).thenReturn(bindingCursed());

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory).setHelmet(HAT);
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_PLACED);
    }

    @Test
    void testEmptyHeadIsFilledWithItemInHand() throws Exception {
        TestSources.grant(this.player, ALLOW_DIAMOND_BLOCK);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(HAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat"));

        Mockito.verify(this.inventory).setHelmet(HAT);
        Mockito.verify(this.inventory).setItemInMainHand(ItemStack.empty());
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_PLACED);
    }

    @Test
    void testWornHatIsRemoved() throws Exception {
        Mockito.when(this.inventory.getHelmet()).thenReturn(WORN);

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat remove"));

        Mockito.verify(this.inventory).setHelmet(null);
        Mockito.verify(this.inventory).addItem(WORN);
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_REMOVED);
    }

    @Test
    void testNoHatIsRemoved() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "hat remove"));

        Mockito.verify(this.inventory, Mockito.never()).setHelmet(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_EMPTY);
    }

    @Test
    void testBindingCursedHatIsNotRemoved() throws Exception {
        Mockito.when(this.inventory.getHelmet()).thenReturn(bindingCursed());

        Assertions.assertEquals(0, this.tester.execute(this.source, "hat remove"));

        Mockito.verify(this.inventory, Mockito.never()).setHelmet(Mockito.any());
        Mockito.verify(this.inventory, Mockito.never()).addItem(Mockito.any(ItemStack.class));
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_CURSE);
    }

    @Test
    void testBindingCursedHatIsRemovedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_BINDING);
        ItemStack cursed = bindingCursed();
        Mockito.when(this.inventory.getHelmet()).thenReturn(cursed);

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat remove"));

        Mockito.verify(this.inventory).setHelmet(null);
        Mockito.verify(this.inventory).addItem(cursed);
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_REMOVED);
    }

    @Test
    void testRemovedHatIsDroppedWhenInventoryIsFull() throws Exception {
        World world = Mockito.mock(World.class);
        Location location = new Location(world, 0, 0, 0);
        Mockito.when(this.player.getWorld()).thenReturn(world);
        Mockito.when(this.player.getLocation()).thenReturn(location);

        Mockito.when(this.inventory.getHelmet()).thenReturn(WORN);
        Mockito.when(this.inventory.addItem(WORN)).thenReturn(new HashMap<>(Map.of(0, WORN)));

        Assertions.assertEquals(1, this.tester.execute(this.source, "hat remove"));

        Mockito.verify(this.inventory).setHelmet(null);
        Mockito.verify(world).dropItem(location, WORN);
        Mockito.verify(this.player).sendMessage(CommandMessages.HAT_REMOVED);
    }

    @Test
    void testConsoleCannotWearHat() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "hat"));

        Mockito.verify(console).sendMessage(CommandMessages.HAT_PLAYER_ONLY.apply("/hat"));
    }

    @Test
    void testConsoleCannotRemoveHat() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "hat remove"));

        Mockito.verify(console).sendMessage(CommandMessages.HAT_PLAYER_ONLY.apply("/hat"));
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

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "hat"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "hat remove"));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

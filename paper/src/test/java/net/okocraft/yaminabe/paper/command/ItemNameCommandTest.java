package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class ItemNameCommandTest {

    private static final String PERMISSION = "yaminabe.command.itemname";
    private static final String ALLOW_DIAMOND_BLOCK = PERMISSION + ".allow-type.diamond_block";
    private static final String COLOR_FORMAT = PERMISSION + ".format.color";
    private static final String DECORATION_FORMAT = PERMISSION + ".format.decoration";
    private static final String IGNORE_LENGTH_LIMIT = PERMISSION + ".ignore-length-limit";

    private static final int MAX_NAME_LENGTH = 200;

    /**
     * An item name is italicized unless it is told not to be.
     */
    private static Component named(String name, @Nullable NamedTextColor color) {
        return Component.text(name, color).decoration(TextDecoration.ITALIC, false);
    }

    private final CommandTester tester = CommandTester.of(ItemNameCommand.createItemNameCommand());

    private Player player;
    private PlayerInventory inventory;
    private ItemStack item;
    private CommandSourceStack source;

    @BeforeEach
    void setUp() {
        this.player = Mockito.mock(Player.class);
        this.inventory = Mockito.mock(PlayerInventory.class);
        this.item = ItemStack.of(Material.DIAMOND_BLOCK);

        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(this.item);
        Mockito.when(this.player.getInventory()).thenReturn(this.inventory);

        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION, ALLOW_DIAMOND_BLOCK);
    }

    @Test
    void testHeldItemIsRenamed() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname Hello"));

        Component name = named("Hello", null);
        Assertions.assertEquals(name, this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_RENAMED.apply(name));
    }

    @Test
    void testColorTagIsLeftAsTypedWithoutPermission() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname <red>Hello"));

        Assertions.assertEquals(named("<red>Hello", null), this.item.getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testColorTagIsResolvedWithPermission() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname <red>Hello"));

        Assertions.assertEquals(named("Hello", NamedTextColor.RED), this.item.getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testExplicitItalicIsKept() throws Exception {
        TestSources.grant(this.player, DECORATION_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname <italic>Hello"));

        Assertions.assertEquals(
            Component.text("Hello").decoration(TextDecoration.ITALIC, true),
            this.item.getData(DataComponentTypes.CUSTOM_NAME)
        );
    }

    @Test
    void testTagOutOfTheOfferedGroupsIsLeftAsTyped() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT, DECORATION_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname Hello<newline>World"));

        Assertions.assertEquals(named("Hello<newline>World", null), this.item.getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testNameIsClearedWithoutArgument() throws Exception {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Hello"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname"));

        Assertions.assertNull(this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_CLEARED);
    }

    @Test
    void testNameIsClearedWithBlankArgument() throws Exception {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Hello"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname   "));

        Assertions.assertNull(this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_CLEARED);
    }

    @Test
    void testNameOfMaxLengthIsAccepted() throws Exception {
        String name = "a".repeat(MAX_NAME_LENGTH);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname " + name));

        Assertions.assertEquals(named(name, null), this.item.getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testTooLongNameIsRejected() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "itemname " + "a".repeat(MAX_NAME_LENGTH + 1)));

        Assertions.assertNull(this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_TOO_LONG.apply(MAX_NAME_LENGTH));
    }

    /**
     * The limit is counted on the MiniMessage source as it is typed, not on the rendered text.
     */
    @Test
    void testTooLongNameIsRejectedEvenIfItIsRenderedShorter() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT);

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemname <red>" + "a".repeat(MAX_NAME_LENGTH - 4)));

        Assertions.assertNull(this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_TOO_LONG.apply(MAX_NAME_LENGTH));
    }

    @Test
    void testTooLongNameIsAcceptedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_LENGTH_LIMIT);
        String name = "a".repeat(MAX_NAME_LENGTH + 1);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemname " + name));

        Assertions.assertEquals(named(name, null), this.item.getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testEmptyHandIsNotRenamed() throws Exception {
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(ItemStack.empty());

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemname Hello"));

        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_NO_ITEM);
    }

    @Test
    void testItemOfDisallowedTypeIsNotRenamed() throws Exception {
        ItemStack stone = ItemStack.of(Material.STONE);
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(stone);

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemname Hello"));

        Assertions.assertNull(stone.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_PREVENTED.apply(stone.effectiveName()));
    }

    @Test
    void testNameOfItemOfDisallowedTypeIsNotCleared() throws Exception {
        ItemStack stone = ItemStack.of(Material.STONE);
        stone.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Hello"));
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(stone);

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemname"));

        Assertions.assertEquals(Component.text("Hello"), stone.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMNAME_PREVENTED.apply(stone.effectiveName()));
    }

    @Test
    void testCurrentNameIsSuggested() throws Exception {
        this.tester.execute(this.source, "itemname Hello");

        Assertions.assertEquals(List.of("Hello"), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testCurrentNameIsSuggestedWithTheTagsItWasTypedWith() {
        TestSources.grant(this.player, COLOR_FORMAT);
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", NamedTextColor.RED));

        Assertions.assertEquals(List.of("<red>Hello"), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testCurrentNameIsSuggestedOnlyWhenItStartsWithWhatIsTyped() {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", null));

        Assertions.assertEquals(List.of("Hello"), this.tester.suggest(this.source, "itemname he"));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname World"));
    }

    @Test
    void testNoNameIsSuggestedForItemWithoutName() {
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForEmptyHand() {
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(ItemStack.empty());

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForItemOfDisallowedType() {
        ItemStack stone = ItemStack.of(Material.STONE);
        stone.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", null));
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(stone);

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForNameUsingDisallowedTag() {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", NamedTextColor.RED));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForNameUsingTagOutOfTheOfferedGroups() {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", null).insertion("Hello"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    /**
     * A suggestion is only a shortcut for retyping, so the length limit is not lifted by the permission.
     */
    @Test
    void testNoNameIsSuggestedForTooLongName() {
        TestSources.grant(this.player, IGNORE_LENGTH_LIMIT);
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("a".repeat(MAX_NAME_LENGTH + 1), null));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForNameHoldingSectionSign() {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("§cHello", null));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedForNameHoldingLineBreak() {
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello\nWorld", null));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemname "));
    }

    @Test
    void testNoNameIsSuggestedToConsole() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);
        this.item.setData(DataComponentTypes.CUSTOM_NAME, named("Hello", null));

        Assertions.assertEquals(List.of(), this.tester.suggest(TestSources.ofSenderOnly(console), "itemname "));
    }

    @Test
    void testItemHeldByExecutorIsRenamed() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, PERMISSION, ALLOW_DIAMOND_BLOCK);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(sender, this.player), "itemname Hello"));

        Component name = named("Hello", null);
        Assertions.assertEquals(name, this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(sender).sendMessage(CommandMessages.ITEMNAME_RENAMED.apply(name));
        Mockito.verify(this.player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testPermissionIsCheckedAgainstSenderInsteadOfExecutor() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, PERMISSION, COLOR_FORMAT);

        Assertions.assertEquals(0, this.tester.execute(TestSources.of(sender, this.player), "itemname Hello"));

        Assertions.assertNull(this.item.getData(DataComponentTypes.CUSTOM_NAME));
        Mockito.verify(sender).sendMessage(CommandMessages.ITEMNAME_PREVENTED.apply(this.item.effectiveName()));
    }

    @Test
    void testConsoleCannotRenameItem() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "itemname Hello"));

        Mockito.verify(console).sendMessage(CommandMessages.ITEMNAME_PLAYER_ONLY.apply("/itemname"));
    }

    @Test
    void testConsoleCannotClearName() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "itemname"));

        Mockito.verify(console).sendMessage(CommandMessages.ITEMNAME_PLAYER_ONLY.apply("/itemname"));
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
        Mockito.when(player.getInventory()).thenReturn(this.inventory);
        CommandSourceStack hiddenSource = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "itemname"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "itemname Hello"));
        Assertions.assertEquals(List.of(), this.tester.suggest(hiddenSource, "itemname "));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

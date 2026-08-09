package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

class ItemLoreCommandTest {

    private static final String PERMISSION = "yaminabe.command.itemlore";
    private static final String ALLOW_DIAMOND_BLOCK = PERMISSION + ".allow-type.diamond_block";
    private static final String COLOR_FORMAT = PERMISSION + ".format.color";
    private static final String DECORATION_FORMAT = PERMISSION + ".format.decoration";
    private static final String IGNORE_LENGTH_LIMIT = PERMISSION + ".ignore-length-limit";
    private static final String IGNORE_LINE_LIMIT = PERMISSION + ".ignore-line-limit";

    private static final int MAX_TEXT_LENGTH = 200;
    private static final int MAX_LINES = 10;
    private static final int MAX_LINES_OF_COMPONENT = 256;

    /**
     * A lore line is italicized unless it is told not to be.
     */
    private static Component line(String text, @Nullable NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component line(String text) {
        return line(text, null);
    }

    private static List<Component> lines(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(number -> line("Line " + number)).toList();
    }

    private final CommandTester tester = CommandTester.of(ItemLoreCommand.createItemLoreCommand());

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

    private void setLore(Component... lines) {
        this.setLore(Arrays.asList(lines));
    }

    private void setLore(List<Component> lines) {
        this.item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
    }

    /**
     * The lore of an item that has none is an empty one, as the lore data component has a default value.
     */
    private static List<Component> loreOf(ItemStack item) {
        ItemLore lore = item.getData(DataComponentTypes.LORE);
        return lore != null ? lore.lines() : List.of();
    }

    private List<Component> lore() {
        return loreOf(this.item);
    }

    @Test
    void testTextIsAddedToLore() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add Hello"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_ADDED.apply(line("Hello")));
    }

    @Test
    void testTextIsAppendedToExistingLore() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add World"));

        Assertions.assertEquals(List.of(line("Hello"), line("World")), this.lore());
    }

    @Test
    void testBlankTextIsAddedAsBlankLine() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add  "));

        Assertions.assertEquals(List.of(line(" ")), this.lore());
    }

    @Test
    void testColorTagIsLeftAsTypedWithoutPermission() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add <red>Hello"));

        Assertions.assertEquals(List.of(line("<red>Hello")), this.lore());
    }

    @Test
    void testColorTagIsResolvedWithPermission() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add <red>Hello"));

        Assertions.assertEquals(List.of(line("Hello", NamedTextColor.RED)), this.lore());
    }

    @Test
    void testExplicitItalicIsKept() throws Exception {
        TestSources.grant(this.player, DECORATION_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add <italic>Hello"));

        Assertions.assertEquals(List.of(Component.text("Hello").decoration(TextDecoration.ITALIC, true)), this.lore());
    }

    @Test
    void testTagOutOfTheOfferedGroupsIsLeftAsTyped() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT, DECORATION_FORMAT);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add Hello<newline>World"));

        Assertions.assertEquals(List.of(line("Hello<newline>World")), this.lore());
    }

    @Test
    void testTextOfMaxLengthIsAdded() throws Exception {
        String text = "a".repeat(MAX_TEXT_LENGTH);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add " + text));

        Assertions.assertEquals(List.of(line(text)), this.lore());
    }

    @Test
    void testTooLongTextIsNotAdded() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore add " + "a".repeat(MAX_TEXT_LENGTH + 1)));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_LONG.apply(MAX_TEXT_LENGTH));
    }

    /**
     * The limit is counted on the MiniMessage source as it is typed, not on the rendered text.
     */
    @Test
    void testTooLongTextIsNotAddedEvenIfItIsRenderedShorter() throws Exception {
        TestSources.grant(this.player, COLOR_FORMAT);

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore add <red>" + "a".repeat(MAX_TEXT_LENGTH - 4)));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_LONG.apply(MAX_TEXT_LENGTH));
    }

    @Test
    void testTooLongTextIsAddedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_LENGTH_LIMIT);
        String text = "a".repeat(MAX_TEXT_LENGTH + 1);

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add " + text));

        Assertions.assertEquals(List.of(line(text)), this.lore());
    }

    @Test
    void testTextIsNotAddedWhenLineLimitIsReached() throws Exception {
        this.setLore(lines(MAX_LINES));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore add Hello"));

        Assertions.assertEquals(lines(MAX_LINES), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_MANY_LINES.apply(MAX_LINES));
    }

    @Test
    void testLineLimitIsRaisedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_LINE_LIMIT);
        this.setLore(lines(MAX_LINES));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore add Hello"));

        Assertions.assertEquals(MAX_LINES + 1, this.lore().size());
    }

    /**
     * The lore data component itself does not hold more lines than this, so the permission cannot raise the limit
     * any further.
     */
    @Test
    void testLineLimitOfComponentIsNotRaisedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_LINE_LIMIT);
        this.setLore(lines(MAX_LINES_OF_COMPONENT));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore add Hello"));

        Assertions.assertEquals(MAX_LINES_OF_COMPONENT, this.lore().size());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_MANY_LINES.apply(MAX_LINES_OF_COMPONENT));
    }

    @Test
    void testLineIsSet() throws Exception {
        this.setLore(line("Hello"), line("World"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore set 2 Yaminabe"));

        Assertions.assertEquals(List.of(line("Hello"), line("Yaminabe")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_SET.apply(2, line("Yaminabe")));
    }

    @Test
    void testLineOutOfRangeIsNotSet() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore set 2 World"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_LINE.apply(2));
    }

    @Test
    void testLineIsNotSetWithoutLore() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore set 1 Hello"));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_LORE);
    }

    @Test
    void testTooLongTextIsNotSet() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore set 1 " + "a".repeat(MAX_TEXT_LENGTH + 1)));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_LONG.apply(MAX_TEXT_LENGTH));
    }

    /**
     * {@code set} does not add a line, so the number of lines is not checked.
     */
    @Test
    void testLineIsSetWhenLineLimitIsReached() throws Exception {
        this.setLore(lines(MAX_LINES));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore set 1 Hello"));

        Assertions.assertEquals(line("Hello"), this.lore().getFirst());
        Assertions.assertEquals(MAX_LINES, this.lore().size());
    }

    @Test
    void testLineIsInserted() throws Exception {
        this.setLore(line("Hello"), line("World"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore insert 1 Yaminabe"));

        Assertions.assertEquals(List.of(line("Yaminabe"), line("Hello"), line("World")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_INSERTED.apply(1, line("Yaminabe")));
    }

    @Test
    void testLineIsInsertedIntoLoreWithoutLine() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore insert 1 Hello"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
    }

    @Test
    void testLineAfterTheLastOneIsAppended() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore insert 2 World"));

        Assertions.assertEquals(List.of(line("Hello"), line("World")), this.lore());
    }

    @Test
    void testLinePastTheLastOneIsNotInserted() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore insert 3 World"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_LINE_OUT_OF_RANGE.apply(2));
    }

    @Test
    void testLineIsNotInsertedWhenLineLimitIsReached() throws Exception {
        this.setLore(lines(MAX_LINES));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore insert 1 Hello"));

        Assertions.assertEquals(lines(MAX_LINES), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_MANY_LINES.apply(MAX_LINES));
    }

    @Test
    void testTooLongTextIsNotInserted() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore insert 1 " + "a".repeat(MAX_TEXT_LENGTH + 1)));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_TOO_LONG.apply(MAX_TEXT_LENGTH));
    }

    @Test
    void testLineIsRemoved() throws Exception {
        this.setLore(line("Hello"), line("World"), line("Yaminabe"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore remove 2"));

        Assertions.assertEquals(List.of(line("Hello"), line("Yaminabe")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_REMOVED.apply(2));
    }

    @Test
    void testRemovingTheLastRemainingLineRemovesLore() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore remove 1"));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_REMOVED.apply(1));
    }

    @Test
    void testLineOutOfRangeIsNotRemoved() throws Exception {
        this.setLore(line("Hello"));

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore remove 2"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_LINE.apply(2));
    }

    @Test
    void testNoLineIsRemovedWithoutLore() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore remove 1"));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_LORE);
    }

    @Test
    void testLoreIsCleared() throws Exception {
        this.setLore(line("Hello"), line("World"));

        Assertions.assertEquals(1, this.tester.execute(this.source, "itemlore clear"));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_CLEARED);
    }

    @Test
    void testNoLoreIsCleared() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore clear"));

        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_LORE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void testLineNumberBelowOneIsNotAccepted(int lineNumber) {
        this.setLore(line("Hello"));

        for (String subCommand : List.of("set " + lineNumber + " World", "insert " + lineNumber + " World", "remove " + lineNumber)) {
            Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "itemlore " + subCommand));
        }

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
    }

    @ParameterizedTest
    @ValueSource(strings = {"add Hello", "set 1 Hello", "insert 1 Hello", "remove 1", "clear"})
    void testEmptyHandIsNotEdited(String subCommand) throws Exception {
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(ItemStack.empty());

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore " + subCommand));

        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_NO_ITEM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"add Hello", "set 1 Hello", "insert 1 Hello", "remove 1", "clear"})
    void testLoreOfItemOfDisallowedTypeIsNotEdited(String subCommand) throws Exception {
        ItemStack stone = ItemStack.of(Material.STONE);
        stone.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(line("Hello"))));
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(stone);

        Assertions.assertEquals(0, this.tester.execute(this.source, "itemlore " + subCommand));

        Assertions.assertEquals(List.of(line("Hello")), loreOf(stone));
        Mockito.verify(this.player).sendMessage(CommandMessages.ITEMLORE_PREVENTED.apply(stone.effectiveName()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"add Hello", "set 1 Hello", "insert 1 Hello", "remove 1", "clear"})
    void testConsoleCannotEditLore(String subCommand) throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "itemlore " + subCommand));

        Mockito.verify(console).sendMessage(CommandMessages.ITEMLORE_PLAYER_ONLY.apply("/itemlore"));
    }

    @Test
    void testLoreOfItemHeldByExecutorIsEdited() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, PERMISSION, ALLOW_DIAMOND_BLOCK);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(sender, this.player), "itemlore add Hello"));

        Assertions.assertEquals(List.of(line("Hello")), this.lore());
        Mockito.verify(sender).sendMessage(CommandMessages.ITEMLORE_ADDED.apply(line("Hello")));
        Mockito.verify(this.player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testPermissionIsCheckedAgainstSenderInsteadOfExecutor() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, PERMISSION, COLOR_FORMAT);

        Assertions.assertEquals(0, this.tester.execute(TestSources.of(sender, this.player), "itemlore add Hello"));

        Assertions.assertEquals(List.of(), this.lore());
        Mockito.verify(sender).sendMessage(CommandMessages.ITEMLORE_PREVENTED.apply(this.item.effectiveName()));
    }

    @Test
    void testLineNumbersOfExistingLinesAreSuggested() {
        this.setLore(lines(3));

        Assertions.assertEquals(List.of("1", "2", "3"), this.tester.suggest(this.source, "itemlore set "));
        Assertions.assertEquals(List.of("1", "2", "3"), this.tester.suggest(this.source, "itemlore remove "));
    }

    /**
     * {@code insert} also takes the line after the last one, which appends as {@code add} does.
     */
    @Test
    void testLineNumberAfterTheLastOneIsSuggestedForInsert() {
        this.setLore(lines(3));

        Assertions.assertEquals(List.of("1", "2", "3", "4"), this.tester.suggest(this.source, "itemlore insert "));
    }

    @Test
    void testLineNumbersAreSuggestedOnlyWhenTheyStartWithWhatIsTyped() {
        this.setLore(lines(12));

        Assertions.assertEquals(List.of("1", "10", "11", "12"), this.tester.suggest(this.source, "itemlore set 1"));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 13"));
    }

    @Test
    void testNoLineNumberIsSuggestedWithoutLore() {
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set "));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore remove "));
        Assertions.assertEquals(List.of("1"), this.tester.suggest(this.source, "itemlore insert "));
    }

    @Test
    void testNoLineNumberIsSuggestedForEmptyHand() {
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(ItemStack.empty());

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore insert "));
    }

    @Test
    void testNoLineNumberIsSuggestedForItemOfDisallowedType() {
        ItemStack stone = ItemStack.of(Material.STONE);
        stone.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(line("Hello"))));
        Mockito.when(this.inventory.getItemInMainHand()).thenReturn(stone);

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set "));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore insert "));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore remove "));
    }

    @Test
    void testCurrentLineIsSuggested() {
        this.setLore(line("Hello"), line("World"));

        Assertions.assertEquals(List.of("World"), this.tester.suggest(this.source, "itemlore set 2 "));
    }

    @Test
    void testCurrentLineIsSuggestedWithTheTagsItWasTypedWith() {
        TestSources.grant(this.player, COLOR_FORMAT);
        this.setLore(line("Hello", NamedTextColor.RED));

        Assertions.assertEquals(List.of("<red>Hello"), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    @Test
    void testCurrentLineIsSuggestedOnlyWhenItStartsWithWhatIsTyped() {
        this.setLore(line("Hello"));

        Assertions.assertEquals(List.of("Hello"), this.tester.suggest(this.source, "itemlore set 1 he"));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 World"));
    }

    @Test
    void testNoLineIsSuggestedForLineOutOfRange() {
        this.setLore(line("Hello"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 2 "));
    }

    @Test
    void testNoLineIsSuggestedForLineUsingDisallowedTag() {
        this.setLore(line("Hello", NamedTextColor.RED));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    @Test
    void testNoLineIsSuggestedForLineUsingTagOutOfTheOfferedGroups() {
        this.setLore(line("Hello").insertion("Hello"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    /**
     * A suggestion is only a shortcut for retyping, so the length limit is not lifted by the permission.
     */
    @Test
    void testNoLineIsSuggestedForTooLongLine() {
        TestSources.grant(this.player, IGNORE_LENGTH_LIMIT);
        this.setLore(line("a".repeat(MAX_TEXT_LENGTH + 1)));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    @Test
    void testNoLineIsSuggestedForLineHoldingSectionSign() {
        this.setLore(line("§cHello"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    @Test
    void testNoLineIsSuggestedForLineHoldingLineBreak() {
        this.setLore(line("Hello\nWorld"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore set 1 "));
    }

    /**
     * Only {@code set} edits a line that is already there, so only it suggests one.
     */
    @Test
    void testNoLineIsSuggestedForTextThatIsNotEdited() {
        this.setLore(line("Hello"));

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore add "));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "itemlore insert 1 "));
    }

    @Test
    void testNothingIsSuggestedToConsole() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);
        this.setLore(line("Hello"));
        CommandSourceStack consoleSource = TestSources.ofSenderOnly(console);

        Assertions.assertEquals(List.of(), this.tester.suggest(consoleSource, "itemlore set "));
        Assertions.assertEquals(List.of(), this.tester.suggest(consoleSource, "itemlore set 1 "));
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

        for (String input : List.of("itemlore", "itemlore add Hello", "itemlore set 1 Hello", "itemlore insert 1 Hello", "itemlore remove 1", "itemlore clear")) {
            Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, input));
            Assertions.assertEquals(List.of(), this.tester.suggest(hiddenSource, input));
        }

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

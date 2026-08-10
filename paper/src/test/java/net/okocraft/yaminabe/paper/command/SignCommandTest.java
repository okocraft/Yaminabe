package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.okocraft.yaminabe.paper.command.SignCommand.SignChangeNotifier;
import net.okocraft.yaminabe.paper.platform.RegionScheduler;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.DyeColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SignCommandTest {

    private static final String PERMISSION = "yaminabe.command.sign";
    private static final String AT_PERMISSION = PERMISSION + ".at";
    private static final String IGNORE_LENGTH_LIMIT_PERMISSION = PERMISSION + ".ignore-length-limit";
    private static final String IGNORE_WAXED_PERMISSION = PERMISSION + ".ignore-waxed";

    private static final int LINES = 4;

    /**
     * A sign is edited on the thread that owns it, which a test has no reason to leave the calling thread for.
     */
    private static final RegionScheduler SCHEDULER = (location, task) -> {
        task.run();
        return true;
    };

    private static final SignChangeNotifier UNWATCHED = (sign, player, lines, side) -> lines;

    /**
     * The position argument the real command takes cannot resolve a position against a source that no server handed
     * out, so a test reads a plain {@code x y z} instead.
     */
    private static final ArgumentType<BlockPositionResolver> POSITION_ARGUMENT = reader -> {
        int x = reader.readInt();
        reader.expect(' ');
        int y = reader.readInt();
        reader.expect(' ');
        int z = reader.readInt();
        return source -> Position.block(x, y, z);
    };

    private final World world = Mockito.mock(World.class);
    private final Block block = Mockito.mock(Block.class);
    private final Sign sign = Mockito.mock(Sign.class);
    private final SignSide front = Mockito.mock(SignSide.class);
    private final SignSide back = Mockito.mock(SignSide.class);

    private Player player;
    private CommandSourceStack source;
    private CommandTester tester;

    @BeforeEach
    void setUp() {
        this.player = Mockito.mock(Player.class);
        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION);

        Mockito.when(this.block.getState()).thenReturn(this.sign);
        Mockito.when(this.sign.getSide(Side.FRONT)).thenReturn(this.front);
        Mockito.when(this.sign.getSide(Side.BACK)).thenReturn(this.back);
        Mockito.when(this.sign.getInteractableSideFor(this.player)).thenReturn(Side.FRONT);
        this.written(this.front);
        this.written(this.back);

        Mockito.when(this.player.getWorld()).thenReturn(this.world);
        Mockito.when(this.player.getLocation()).thenReturn(new Location(this.world, 0, 0, 0));
        Mockito.when(this.player.getEyeLocation()).thenReturn(new Location(this.world, 0, 0, 0));
        this.lookAt(this.block);

        this.tester = CommandTester.of(SignCommand.createSignCommand(SCHEDULER, UNWATCHED, POSITION_ARGUMENT));
    }

    private void lookAt(Block target) {
        RayTraceResult result = target == null ? null : new RayTraceResult(new Vector(), target, null);

        Mockito.when(
            this.world.rayTraceBlocks(
                Mockito.any(Location.class), Mockito.any(Vector.class), Mockito.anyDouble(),
                Mockito.any(FluidCollisionMode.class), Mockito.anyBoolean()
            )
        ).thenReturn(result);
    }

    // Editing lines

    @Test
    void testLineIsSet() throws Exception {
        this.written(this.front, "first", "second");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 2 rewritten"));

        this.assertWritten(this.front, "first", "rewritten", "", "");
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_SET.apply(2, Component.text("rewritten")));
        Mockito.verify(this.sign).update(true);
    }

    @Test
    void testLineIsSetInMiniMessage() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".format.color");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 <red>Hello"));

        this.assertWritten(this.front, Component.text("Hello", NamedTextColor.RED), Component.empty(), Component.empty(), Component.empty());
    }

    @Test
    void testCommandToRunOnAClickIsWritten() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".format.click");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 <click:run_command:'/spawn'>Spawn"));

        this.assertWritten(
            this.front,
            Component.text("Spawn").clickEvent(ClickEvent.runCommand("/spawn")),
            Component.empty(), Component.empty(), Component.empty()
        );
    }

    @Test
    void testTagIsLeftAsTypedWithoutFormatPermission() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 <red>Hi"));

        this.assertWritten(this.front, "<red>Hi", "", "", "");
    }

    @Test
    void testLineIsNotItalicized() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 plain"));

        // Unlike a lore line, a line of a sign is not italic to begin with, so nothing is set on it.
        Mockito.verify(this.front).line(0, Component.text("plain"));
    }

    @Test
    void testLineIsInserted() throws Exception {
        this.written(this.front, "first", "second");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign insert 1 above"));

        this.assertWritten(this.front, "above", "first", "second", "");
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_INSERTED.apply(1, Component.text("above")));
    }

    @Test
    void testLineIsNotInsertedWhenTheLastLineIsWrittenOn() throws Exception {
        this.written(this.front, "first", "second", "third", "fourth");

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign insert 1 above"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_LAST_LINE_NOT_EMPTY.apply(4));
    }

    @Test
    void testLineIsRemoved() throws Exception {
        this.written(this.front, "first", "second", "third", "fourth");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign remove 2"));

        this.assertWritten(this.front, "first", "third", "fourth", "");
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_REMOVED.apply(2));
    }

    @Test
    void testSignIsCleared() throws Exception {
        this.written(this.front, "first", "second");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign clear"));

        this.assertWritten(this.front, "", "", "", "");
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_CLEARED);
    }

    @Test
    void testEmptySignIsNotCleared() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "sign clear"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_ALREADY_EMPTY);
        Mockito.verify(this.sign, Mockito.never()).update(true);
    }

    @Test
    void testLineIsCleared() throws Exception {
        this.written(this.front, "first", "second");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign clear 1"));

        this.assertWritten(this.front, "", "second", "", "");
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_CLEARED_LINE.apply(1));
    }

    @Test
    void testEmptyLineIsNotCleared() throws Exception {
        this.written(this.front, "first");

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign clear 2"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_LINE_ALREADY_EMPTY.apply(2));
    }

    @Test
    void testLineNumberOutOfRangeIsRejected() {
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "sign set 5 too far"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "sign set 0 too far"));
    }

    // The length of a line

    @Test
    void testTooLongLineIsRejected() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "sign set 1 0123456789abcdef"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_TOO_LONG.apply(15));
    }

    @Test
    void testLineIsMeasuredAsItIsShown() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".format.color");

        // The tags are not what is shown on the sign, so they are not counted.
        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 <red>0123456789abcde"));

        this.assertWritten(this.front, Component.text("0123456789abcde", NamedTextColor.RED), Component.empty(), Component.empty(), Component.empty());
    }

    @Test
    void testTooLongLineIsAcceptedWithPermission() throws Exception {
        TestSources.grant(this.player, IGNORE_LENGTH_LIMIT_PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 0123456789abcdef"));

        this.assertWritten(this.front, "0123456789abcdef", "", "", "");
    }

    // Which side is edited

    @Test
    void testTheSideFacingThePlayerIsEdited() throws Exception {
        Mockito.when(this.sign.getInteractableSideFor(this.player)).thenReturn(Side.BACK);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 behind"));

        this.assertNothingWritten(this.front);
        this.assertWritten(this.back, "behind", "", "", "");
    }

    @Test
    void testTheGivenSideIsEdited() throws Exception {
        Mockito.when(this.sign.getInteractableSideFor(this.player)).thenReturn(Side.BACK);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign front set 1 in front"));

        this.assertWritten(this.front, "in front", "", "", "");
        this.assertNothingWritten(this.back);
    }

    // A sign that cannot be found

    @Test
    void testNothingIsEditedWithoutASignToLookAt() throws Exception {
        this.lookAt(null);

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign set 1 nowhere"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_NOT_LOOKED_AT);
    }

    @Test
    void testNothingIsEditedWhenAnotherBlockIsLookedAt() throws Exception {
        Block stone = Mockito.mock(Block.class);
        Mockito.when(stone.getState()).thenReturn(Mockito.mock(BlockState.class));
        this.lookAt(stone);

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign set 1 nowhere"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_NOT_LOOKED_AT);
    }

    @Test
    void testConsoleCannotEditASignItLooksAt() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "sign set 1 nowhere"));

        Mockito.verify(console).sendMessage(CommandMessages.SIGN_PLAYER_ONLY.apply("/sign"));
    }

    // A sign given by its position

    @Test
    void testSignAtAPositionIsEdited() throws Exception {
        TestSources.grant(this.player, AT_PERMISSION);
        this.putSignAt(10, 64, 20);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign at 10 64 20 back set 1 far away"));

        this.assertWritten(this.back, "far away", "", "", "");
    }

    @Test
    void testFrontIsEditedWhenNoSideIsGivenForAPosition() throws Exception {
        TestSources.grant(this.player, AT_PERMISSION);
        Mockito.when(this.sign.getInteractableSideFor(this.player)).thenReturn(Side.BACK);
        this.putSignAt(10, 64, 20);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign at 10 64 20 set 1 far away"));

        this.assertWritten(this.front, "far away", "", "", "");
        this.assertNothingWritten(this.back);
    }

    @Test
    void testNothingIsEditedWithoutASignAtThePosition() throws Exception {
        TestSources.grant(this.player, AT_PERMISSION);
        this.putSignAt(10, 64, 20);
        Mockito.when(this.block.getState()).thenReturn(Mockito.mock(BlockState.class));

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign at 10 64 20 set 1 far away"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_NOT_FOUND.apply(10, 64, 20));
    }

    @Test
    void testNothingIsEditedInAnUnloadedChunk() throws Exception {
        TestSources.grant(this.player, AT_PERMISSION);
        this.putSignAt(10, 64, 20);
        Mockito.when(this.world.isChunkLoaded(0, 1)).thenReturn(false);

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign at 10 64 20 set 1 far away"));

        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_CHUNK_NOT_LOADED.apply(10, 64, 20));
        Mockito.verify(this.world, Mockito.never()).getBlockAt(Mockito.any(Location.class));
    }

    @Test
    void testPositionIsHiddenWithoutPermission() {
        this.putSignAt(10, 64, 20);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "sign at 10 64 20 set 1 far away"));
    }

    // The thread the sign is edited on

    @Test
    void testEditIsLeftToTheThreadThatOwnsTheSign() throws Exception {
        List<Runnable> handedOver = new ArrayList<>();
        RegionScheduler scheduler = (location, task) -> {
            handedOver.add(task);
            return false;
        };
        CommandTester tester = CommandTester.of(SignCommand.createSignCommand(scheduler, UNWATCHED, POSITION_ARGUMENT));

        Assertions.assertEquals(1, tester.execute(this.source, "sign set 1 later"));
        this.assertNothingWritten(this.front);

        handedOver.getFirst().run();

        this.assertWritten(this.front, "later", "", "", "");
    }

    // Waxed signs

    @Test
    void testWaxedSignIsNotEdited() throws Exception {
        Mockito.when(this.sign.isWaxed()).thenReturn(true);

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign set 1 waxed"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_IS_WAXED);
    }

    @Test
    void testWaxedSignIsEditedWithPermission() throws Exception {
        Mockito.when(this.sign.isWaxed()).thenReturn(true);
        TestSources.grant(this.player, IGNORE_WAXED_PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign set 1 waxed"));

        this.assertWritten(this.front, "waxed", "", "", "");
    }

    // What a listening plugin has to say

    @Test
    void testCancelledChangeLeavesTheSignAsItIs() throws Exception {
        CommandTester tester = CommandTester.of(SignCommand.createSignCommand(SCHEDULER, (sign, player, lines, side) -> null, POSITION_ARGUMENT));

        Assertions.assertEquals(0, tester.execute(this.source, "sign set 1 rejected"));

        this.assertNothingWritten(this.front);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_EDIT_PREVENTED);
        Mockito.verify(this.sign, Mockito.never()).update(true);
    }

    @Test
    void testRewrittenLinesAreWritten() throws Exception {
        CommandTester tester = CommandTester.of(
            SignCommand.createSignCommand(SCHEDULER, (sign, player, lines, side) -> List.of(Component.text("rewritten"), Component.empty(), Component.empty(), Component.empty()), POSITION_ARGUMENT)
        );

        Assertions.assertEquals(1, tester.execute(this.source, "sign set 1 typed"));

        this.assertWritten(this.front, "rewritten", "", "", "");
    }

    // The other things a sign carries

    @Test
    void testTextIsMadeToGlow() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".glowing");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign glowing true"));

        Mockito.verify(this.front).setGlowingText(true);
        Mockito.verify(this.sign).update(true);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_GLOWING_ENABLED);
    }

    @Test
    void testGlowingIsHiddenWithoutPermission() {
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(this.source, "sign glowing true"));
    }

    @Test
    void testColorIsSet() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".color");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign back color light_blue"));

        Mockito.verify(this.back).setColor(DyeColor.LIGHT_BLUE);
        Mockito.verify(this.front, Mockito.never()).setColor(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_COLOR_SET.apply("light_blue"));
    }

    @Test
    void testUnknownColorIsRejected() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".color");

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign color turquoise"));

        Mockito.verify(this.front, Mockito.never()).setColor(Mockito.any());
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_INVALID_COLOR.apply("turquoise"));
    }

    @Test
    void testSignIsWaxed() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".waxed");

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign waxed true"));

        Mockito.verify(this.sign).setWaxed(true);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_WAXED_ENABLED);
    }

    @Test
    void testWaxedSignIsNotUnwaxedWithoutPermission() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".waxed");
        Mockito.when(this.sign.isWaxed()).thenReturn(true);

        Assertions.assertEquals(0, this.tester.execute(this.source, "sign waxed false"));

        Mockito.verify(this.sign, Mockito.never()).setWaxed(Mockito.anyBoolean());
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_IS_WAXED);
    }

    @Test
    void testWaxedSignIsUnwaxedWithPermission() throws Exception {
        TestSources.grant(this.player, PERMISSION + ".waxed", IGNORE_WAXED_PERMISSION);
        Mockito.when(this.sign.isWaxed()).thenReturn(true);

        Assertions.assertEquals(1, this.tester.execute(this.source, "sign waxed false"));

        Mockito.verify(this.sign).setWaxed(false);
        Mockito.verify(this.player).sendMessage(CommandMessages.SIGN_WAXED_DISABLED);
    }

    // Tab completion

    @Test
    void testLineNumbersAreSuggested() {
        Assertions.assertEquals(List.of("1", "2", "3", "4"), this.tester.suggest(this.source, "sign set "));
    }

    @Test
    void testCurrentLineIsSuggested() {
        this.written(this.front, "first", "second");

        Assertions.assertEquals(List.of("second"), this.tester.suggest(this.source, "sign set 2 "));
    }

    @Test
    void testCurrentLineIsNotSuggestedForAPosition() {
        TestSources.grant(this.player, AT_PERMISSION);
        this.written(this.front, "first", "second");
        this.putSignAt(10, 64, 20);

        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "sign at 10 64 20 set 2 "));
    }

    @Test
    void testColorsAreSuggested() {
        TestSources.grant(this.player, PERMISSION + ".color");

        Assertions.assertEquals(List.of("light_blue", "light_gray"), this.tester.suggest(this.source, "sign color light"));
    }

    // The permission the command itself needs

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

    private void assertHidden(Player player) {
        CommandSourceStack hiddenSource = TestSources.of(player);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "sign set 1 hidden"));

        Mockito.verify(player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    private void putSignAt(int x, int y, int z) {
        Mockito.when(this.source.getLocation()).thenReturn(new Location(this.world, 0, 0, 0));
        Mockito.when(this.world.isChunkLoaded(x >> 4, z >> 4)).thenReturn(true);
        Mockito.when(this.world.getBlockAt(Mockito.any(Location.class))).thenReturn(this.block);
    }

    /**
     * Has the given side read as already having the given lines written on it, and blank lines for the ones not given.
     */
    private void written(SignSide side, String... lines) {
        List<Component> read = linesOf(lines);

        Mockito.when(side.lines()).thenReturn(read);
        Mockito.when(side.line(Mockito.anyInt())).thenAnswer(invocation -> read.get(invocation.getArgument(0)));
    }

    private void assertWritten(SignSide side, String... lines) {
        this.assertWritten(side, linesOf(lines).toArray(Component[]::new));
    }

    /**
     * Asserts that the given lines are the whole of what was written on the given side, as a sign is written on a line
     * at a time and a line left out of the assertion would otherwise go unnoticed.
     */
    private void assertWritten(SignSide side, Component... lines) {
        for (int line = 0; line < lines.length; line++) {
            Mockito.verify(side).line(line, lines[line]);
        }

        Mockito.verify(side, Mockito.times(lines.length)).line(Mockito.anyInt(), Mockito.any());
    }

    private void assertNothingWritten(SignSide side) {
        Mockito.verify(side, Mockito.never()).line(Mockito.anyInt(), Mockito.any());
    }

    private static List<Component> linesOf(String... lines) {
        List<Component> components = new ArrayList<>(Collections.nCopies(LINES, Component.empty()));

        for (int line = 0; line < lines.length; line++) {
            if (!lines[line].isEmpty()) {
                components.set(line, Component.text(lines[line]));
            }
        }

        return List.copyOf(components);
    }
}

package net.okocraft.yaminabe.paper.command;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class SkullCommandTest {

    private static final String PERMISSION = "yaminabe.command.skull";
    private static final String ONLINE = PERMISSION + ".online";
    private static final String OFFLINE = PERMISSION + ".offline";
    private static final String TEXTURE = PERMISSION + ".texture";

    private static final String OWNER_NAME = "Owner";
    private static final String ONLINE_NAME = "Online";
    private static final String OFFLINE_NAME = "Offline";

    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static String textureValue(String url) {
        return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes(StandardCharsets.UTF_8));
    }

    private static Player onlinePlayer(String name) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn(name);
        Mockito.when(player.getPlayerProfile()).thenReturn(new CraftPlayerProfile(UUID.randomUUID(), name));
        Mockito.when(player.canSee(Mockito.any(Player.class))).thenReturn(true);
        return player;
    }

    private static UUID uniqueIdOf(Player player) {
        return player.getPlayerProfile().getId();
    }

    private final CommandTester tester = CommandTester.of(SkullCommand.createSkullCommand());

    private Player player;
    private PlayerInventory inventory;
    private Server server;
    private CommandSourceStack source;

    @BeforeEach
    void setUp() {
        this.player = onlinePlayer(OWNER_NAME);
        this.inventory = Mockito.mock(PlayerInventory.class);
        this.server = Mockito.mock(Server.class);

        Mockito.when(this.player.getInventory()).thenReturn(this.inventory);
        Mockito.when(this.player.getServer()).thenReturn(this.server);
        Mockito.when(this.inventory.addItem(Mockito.any(ItemStack.class))).thenReturn(new HashMap<>());

        this.source = TestSources.of(this.player);
        TestSources.grant(this.player, PERMISSION);
    }

    private ResolvableProfile givenProfile() {
        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        Mockito.verify(this.inventory).addItem(captor.capture());

        ItemStack given = captor.getValue();
        Assertions.assertEquals(Material.PLAYER_HEAD, given.getType());

        ResolvableProfile profile = given.getData(DataComponentTypes.PROFILE);
        Assertions.assertNotNull(profile);
        return profile;
    }

    private static String textureOf(ResolvableProfile profile) {
        return profile.properties().stream()
            .filter(property -> property.getName().equals("textures"))
            .map(ProfileProperty::getValue)
            .findFirst()
            .orElseThrow();
    }

    private void assertNothingGiven() {
        Mockito.verify(this.inventory, Mockito.never()).addItem(Mockito.any(ItemStack.class));
    }

    @Test
    void testOwnSkullIsGiven() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "skull"));

        ResolvableProfile profile = this.givenProfile();
        Assertions.assertEquals(OWNER_NAME, profile.name());
        Assertions.assertEquals(uniqueIdOf(this.player), profile.uuid());
        Assertions.assertFalse(profile.dynamic());
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(OWNER_NAME));
    }

    @Test
    void testBlankArgumentIsReadAsNoArgument() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "skull   "));

        Assertions.assertEquals(OWNER_NAME, this.givenProfile().name());
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(OWNER_NAME));
    }

    @Test
    void testSurroundingSpacesAreIgnored() throws Exception {
        TestSources.grant(this.player, OFFLINE);

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull  " + OFFLINE_NAME + "  "));

        Assertions.assertEquals(OFFLINE_NAME, this.givenProfile().name());
    }

    @Test
    void testNoCustomNameIsSet() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "skull"));

        ArgumentCaptor<ItemStack> captor = ArgumentCaptor.forClass(ItemStack.class);
        Mockito.verify(this.inventory).addItem(captor.capture());

        Assertions.assertNull(captor.getValue().getData(DataComponentTypes.CUSTOM_NAME));
    }

    @Test
    void testOwnNameDoesNotNeedPermission() throws Exception {
        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + OWNER_NAME.toLowerCase()));

        Assertions.assertEquals(OWNER_NAME, this.givenProfile().name());
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(OWNER_NAME));
    }

    @Test
    void testSkullOfOnlinePlayerIsGiven() throws Exception {
        TestSources.grant(this.player, ONLINE);
        Player online = onlinePlayer(ONLINE_NAME);
        Mockito.when(this.server.getPlayerExact(ONLINE_NAME)).thenReturn(online);

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + ONLINE_NAME));

        ResolvableProfile profile = this.givenProfile();
        Assertions.assertEquals(ONLINE_NAME, profile.name());
        Assertions.assertEquals(uniqueIdOf(online), profile.uuid());
        Assertions.assertFalse(profile.dynamic());
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(ONLINE_NAME));
    }

    @Test
    void testSkullOfOnlinePlayerNeedsPermission() throws Exception {
        Player online = onlinePlayer(ONLINE_NAME);
        Mockito.when(this.server.getPlayerExact(ONLINE_NAME)).thenReturn(online);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + ONLINE_NAME));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_ONLINE_PREVENTED);
    }

    @Test
    void testSkullOfOfflinePlayerIsResolvedByTheServer() throws Exception {
        TestSources.grant(this.player, OFFLINE);

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + OFFLINE_NAME));

        ResolvableProfile profile = this.givenProfile();
        Assertions.assertEquals(OFFLINE_NAME, profile.name());
        Assertions.assertNull(profile.uuid());
        Assertions.assertTrue(profile.dynamic());
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(OFFLINE_NAME));
    }

    @Test
    void testSkullOfOfflinePlayerNeedsPermission() throws Exception {
        TestSources.grant(this.player, ONLINE);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + OFFLINE_NAME));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_OFFLINE_PREVENTED);
    }

    @Test
    void testSkullOfHashIsGiven() throws Exception {
        TestSources.grant(this.player, TEXTURE);

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + HASH.toUpperCase()));

        ResolvableProfile profile = this.givenProfile();
        Assertions.assertNull(profile.name());
        Assertions.assertEquals(textureValue("https://textures.minecraft.net/texture/" + HASH), textureOf(profile));
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(HASH.substring(0, 8)));
    }

    @Test
    void testSkullOfTextureValueIsGivenAsItIs() throws Exception {
        TestSources.grant(this.player, TEXTURE);
        String value = textureValue("http://textures.minecraft.net/texture/" + HASH);

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + value));

        ResolvableProfile profile = this.givenProfile();
        Assertions.assertNull(profile.name());
        Assertions.assertEquals(value, textureOf(profile));
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_GIVEN.apply(HASH.substring(0, 8)));
    }

    @Test
    void testTextureNeedsPermission() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + HASH));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_TEXTURE_PREVENTED);
    }

    @Test
    void testTextureValueWithAnotherTextureIsGiven() throws Exception {
        TestSources.grant(this.player, TEXTURE);
        String value = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + HASH
            + "\",\"metadata\":{\"model\":\"slim\"}},\"CAPE\":{\"url\":\"https://textures.minecraft.net/texture/" + HASH
            + "\"}}}").getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull " + value));

        Assertions.assertEquals(value, textureOf(this.givenProfile()));
    }

    @Test
    void testTextureValueWithAnotherTextureOfAnotherHostIsNotAccepted() throws Exception {
        TestSources.grant(this.player, TEXTURE);
        String value = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + HASH
            + "\"},\"CAPE\":{\"url\":\"https://example.com/cape.png\"}}}").getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + value));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_INVALID_OWNER);
    }

    @Test
    void testTextureOfAnotherHostIsNotAccepted() throws Exception {
        TestSources.grant(this.player, TEXTURE);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + textureValue("https://example.com/texture/" + HASH)));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_INVALID_OWNER);
    }

    @Test
    void testInvalidOwnerIsNotAccepted() throws Exception {
        TestSources.grant(this.player, TEXTURE);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull not a name"));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_INVALID_OWNER);
    }

    @Test
    void testTextureIsNotMentionedWithoutPermission() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(this.source, "skull not a name"));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_INVALID_NAME);
    }

    @Test
    void testSkullIsDroppedWhenInventoryIsFull() throws Exception {
        World world = Mockito.mock(World.class);
        Location location = new Location(world, 0, 0, 0);
        Mockito.when(this.player.getWorld()).thenReturn(world);
        Mockito.when(this.player.getLocation()).thenReturn(location);

        ItemStack leftover = ItemStack.of(Material.PLAYER_HEAD);
        Mockito.when(this.inventory.addItem(Mockito.any(ItemStack.class))).thenReturn(new HashMap<>(Map.of(0, leftover)));

        Assertions.assertEquals(1, this.tester.execute(this.source, "skull"));

        Mockito.verify(world).dropItem(location, leftover);
    }

    @Test
    void testOnlinePlayersAreSuggestedWithPermission() {
        TestSources.grant(this.player, ONLINE);
        Player online = onlinePlayer(ONLINE_NAME);
        Mockito.doReturn(List.of(this.player, online)).when(this.server).getOnlinePlayers();

        Assertions.assertEquals(List.of(ONLINE_NAME, OWNER_NAME), this.tester.suggest(this.source, "skull O"));
    }

    @Test
    void testOnlyOwnNameIsSuggestedWithoutPermission() {
        Assertions.assertEquals(List.of(OWNER_NAME), this.tester.suggest(this.source, "skull "));
        Assertions.assertEquals(List.of(), this.tester.suggest(this.source, "skull A"));
    }

    @Test
    void testHiddenPlayerIsTreatedAsOffline() throws Exception {
        TestSources.grant(this.player, ONLINE);
        Player hidden = onlinePlayer(ONLINE_NAME);
        Mockito.when(this.server.getPlayerExact(ONLINE_NAME)).thenReturn(hidden);
        Mockito.when(this.player.canSee(hidden)).thenReturn(false);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + ONLINE_NAME));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_OFFLINE_PREVENTED);
    }

    @Test
    void testHiddenPlayerIsNotSuggested() {
        TestSources.grant(this.player, ONLINE);
        Player hidden = onlinePlayer(ONLINE_NAME);
        Mockito.doReturn(List.of(this.player, hidden)).when(this.server).getOnlinePlayers();
        Mockito.when(this.player.canSee(hidden)).thenReturn(false);

        Assertions.assertEquals(List.of(OWNER_NAME), this.tester.suggest(this.source, "skull O"));
    }

    @Test
    void testSkullIsGivenToExecutor() throws Exception {
        Player sender = onlinePlayer("Sender");
        TestSources.grant(sender, PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(sender, this.player), "skull"));

        Assertions.assertEquals(OWNER_NAME, this.givenProfile().name());
        Mockito.verify(sender).sendMessage(CommandMessages.SKULL_GIVEN.apply(OWNER_NAME));
        Mockito.verify(this.player, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }

    @Test
    void testPermissionIsCheckedAgainstSenderInsteadOfExecutor() throws Exception {
        Player sender = onlinePlayer("Sender");
        TestSources.grant(sender, PERMISSION, OFFLINE);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(sender, this.player), "skull " + OFFLINE_NAME));

        Assertions.assertTrue(this.givenProfile().dynamic());
        Mockito.verify(sender).sendMessage(CommandMessages.SKULL_GIVEN.apply(OFFLINE_NAME));
    }

    @Test
    void testTooLongTextureValueIsNotAccepted() throws Exception {
        TestSources.grant(this.player, TEXTURE);
        String value = textureValue("https://textures.minecraft.net/texture/" + HASH).repeat(20);

        Assertions.assertEquals(0, this.tester.execute(this.source, "skull " + value));

        this.assertNothingGiven();
        Mockito.verify(this.player).sendMessage(CommandMessages.SKULL_INVALID_OWNER);
    }

    @Test
    void testConsoleCannotGetSkull() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(0, this.tester.execute(TestSources.ofSenderOnly(console), "skull"));

        Mockito.verify(console).sendMessage(CommandMessages.SKULL_PLAYER_ONLY.apply("/skull"));
    }

    @Test
    void testCommandIsHiddenWithoutPermission() {
        Player hidden = Mockito.mock(Player.class);
        CommandSourceStack hiddenSource = TestSources.of(hidden);

        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "skull"));
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(hiddenSource, "skull " + OWNER_NAME));

        Mockito.verify(hidden, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}

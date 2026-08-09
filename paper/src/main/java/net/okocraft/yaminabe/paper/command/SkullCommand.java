package net.okocraft.yaminabe.paper.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NotNullByDefault
final class SkullCommand {

    private static final String COMMAND_NAME = "skull";
    private static final String PERMISSION = "yaminabe.command.skull";
    private static final String ONLINE_PERMISSION = PERMISSION + ".online";
    private static final String OFFLINE_PERMISSION = PERMISSION + ".offline";
    private static final String TEXTURE_PERMISSION = PERMISSION + ".texture";

    private static final String OWNER_ARGUMENT = "owner";

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-fA-F]{17,64}");
    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile("https?://textures\\.minecraft\\.net/texture/([0-9a-fA-F]{1,64})");

    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";
    private static final String TEXTURE_PROPERTY = "textures";
    private static final String SKIN_TEXTURE = "SKIN";

    private static final int MAX_TEXTURE_VALUE_LENGTH = 1024;
    private static final int SHORT_HASH_LENGTH = 8;

    static LiteralCommandNode<CommandSourceStack> createSkullCommand() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> give(context, null))
            .then(
                Commands.argument(OWNER_ARGUMENT, StringArgumentType.greedyString())
                    .suggests(SkullCommand::suggestOwners)
                    .executes(context -> give(context, context.getArgument(OWNER_ARGUMENT, String.class)))
            )
            .build();
    }

    private static CompletableFuture<Suggestions> suggestOwners(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        Stream<String> names =
            context.getSource().getSender().hasPermission(ONLINE_PERMISSION) ?
                player.getServer().getOnlinePlayers().stream().filter(player::canSee).map(Player::getName) :
                Stream.of(player.getName());

        names.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining)).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int give(CommandContext<CommandSourceStack> context, @Nullable String owner) {
        CommandSender sender = context.getSource().getSender();

        if (!(context.getSource().getExecutor() instanceof Player player)) {
            sender.sendMessage(CommandMessages.SKULL_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return 0;
        }

        String argument = owner != null ? owner.strip() : "";

        if (argument.isEmpty() || argument.equalsIgnoreCase(player.getName())) {
            return giveSkull(sender, player, ResolvableProfile.resolvableProfile(player.getPlayerProfile()), player.getName());
        }

        if (NAME_PATTERN.matcher(argument).matches()) {
            Player online = visibleOnlinePlayer(player, argument);

            if (online != null) {
                if (!sender.hasPermission(ONLINE_PERMISSION)) {
                    sender.sendMessage(CommandMessages.SKULL_ONLINE_PREVENTED);
                    return 0;
                }
                return giveSkull(sender, player, ResolvableProfile.resolvableProfile(online.getPlayerProfile()), online.getName());
            }

            if (!sender.hasPermission(OFFLINE_PERMISSION)) {
                sender.sendMessage(CommandMessages.SKULL_OFFLINE_PREVENTED);
                return 0;
            }
            return giveSkull(sender, player, ResolvableProfile.resolvableProfile().name(argument).build(), argument);
        }

        boolean textureAllowed = sender.hasPermission(TEXTURE_PERMISSION);
        Texture texture = readTexture(argument);

        if (texture == null) {
            sender.sendMessage(textureAllowed ? CommandMessages.SKULL_INVALID_OWNER : CommandMessages.SKULL_INVALID_NAME);
            return 0;
        }

        if (!textureAllowed) {
            sender.sendMessage(CommandMessages.SKULL_TEXTURE_PREVENTED);
            return 0;
        }

        ResolvableProfile profile = ResolvableProfile.resolvableProfile()
            .addProperty(new ProfileProperty(TEXTURE_PROPERTY, texture.value()))
            .build();

        return giveSkull(sender, player, profile, texture.shortHash());
    }

    private static @Nullable Player visibleOnlinePlayer(Player player, String name) {
        Player online = player.getServer().getPlayerExact(name);
        return online != null && player.canSee(online) ? online : null;
    }

    private static int giveSkull(CommandSender sender, Player player, ResolvableProfile profile, String owner) {
        ItemStack skull = ItemStack.of(Material.PLAYER_HEAD);
        skull.setData(DataComponentTypes.PROFILE, profile);

        player.getInventory().addItem(skull).values()
            .forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));

        sender.sendMessage(CommandMessages.SKULL_GIVEN.apply(owner));
        return Command.SINGLE_SUCCESS;
    }

    private static @Nullable Texture readTexture(String argument) {
        if (HASH_PATTERN.matcher(argument).matches()) {
            String hash = argument.toLowerCase(Locale.ROOT);
            String value = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_URL_PREFIX + hash + "\"}}}";
            return new Texture(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)), hash);
        }

        if (argument.isEmpty() || MAX_TEXTURE_VALUE_LENGTH < argument.length()) {
            return null;
        }

        String hash = readHashFromTextureValue(argument);
        return hash != null ? new Texture(argument, hash) : null;
    }

    private static @Nullable String readHashFromTextureValue(String value) {
        String decoded;

        try {
            decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }

        JsonElement root;

        try {
            root = JsonParser.parseString(decoded);
        } catch (JsonParseException e) {
            return null;
        }

        JsonElement textures = readMember(root, "textures");

        if (textures == null || !textures.isJsonObject()) {
            return null;
        }

        String skinHash = null;

        for (Map.Entry<String, JsonElement> texture : textures.getAsJsonObject().entrySet()) {
            String hash = readHashFromTextureUrl(texture.getValue());

            if (hash == null) {
                return null;
            }

            if (texture.getKey().equals(SKIN_TEXTURE)) {
                skinHash = hash;
            }
        }

        return skinHash;
    }

    private static @Nullable String readHashFromTextureUrl(JsonElement texture) {
        JsonElement url = readMember(texture, "url");

        if (url == null || !url.isJsonPrimitive()) {
            return null;
        }

        Matcher matcher = TEXTURE_URL_PATTERN.matcher(url.getAsString());
        return matcher.matches() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private static @Nullable JsonElement readMember(@Nullable JsonElement element, String name) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        return object.has(name) ? object.get(name) : null;
    }

    private record Texture(String value, String hash) {

        private String shortHash() {
            return this.hash.substring(0, Math.min(SHORT_HASH_LENGTH, this.hash.length()));
        }
    }
}

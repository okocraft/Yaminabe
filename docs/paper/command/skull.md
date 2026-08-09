# `/skull`

Gives a player head. This is a port of the EssentialsX `/skull` command.

## Usage

| Command                  | Description                                                          |
|--------------------------|----------------------------------------------------------------------|
| `/skull`                 | Gives your own head.                                                 |
| `/skull <player>`        | Gives the head of the given player.                                  |
| `/skull <hash>`          | Gives a head wearing the skin of the given texture hash.             |
| `/skull <texture value>` | Gives a head wearing the skin of the given Base64 encoded texture.   |

This command can only be run by an in-game player. The head is always a newly created one; an item held in the hand is
never modified.

Under `/execute as <player>`, the head goes to that player, but every permission below is checked against whoever issued
the command, and the messages go back to them.

A head that does not fit in the inventory is dropped at the player's feet.

### The `<owner>` argument

Which of the four forms above is used is decided by the shape of the argument.

- One to sixteen characters of `A-Z`, `a-z`, `0-9` and `_` is read as a player name. Your own name is read as `/skull`
  without an argument is, whatever case it is typed in.
- Seventeen to sixty-four hexadecimal characters is read as the hash of a skin hosted at `textures.minecraft.net`. A
  hash is never sixteen characters or fewer, which is what tells it apart from a player name.
- Anything else is read as a Base64 encoded texture value, the form a head is usually distributed in, and is accepted if
  it decodes to an object holding a `textures.SKIN.url`. Every texture the value holds, the cape as well as the skin,
  must carry a URL that points at `textures.minecraft.net`. The value is then stored as it was given, so what it holds
  beyond the skin URL, such as the skin model or the cape, is kept. A value longer than 1024 characters is rejected, as
  a value that describes a skin is far shorter than that.

An argument that is none of the three is rejected with a message, which only mentions the two texture forms if the
sender may use them.

### How the owner is resolved

A name that belongs to a player who is online is resolved right away, using the profile the server already holds, so
the head wears their current skin. A player you cannot see, such as one hidden by a vanish plugin, does not count as
online here, so that this command does not tell you who is on the server invisibly.

A name that belongs to nobody online is stored as a name alone, the way vanilla stores
`/give @s player_head[profile="Notch"]`, and is left for the client to resolve when it renders the head, as vanilla
leaves it. This command itself never asks the Mojang API, so it never blocks the server and never fails on a name that
cannot be resolved: an unknown name simply leaves the head with the default skin. Such a head also carries the tooltip
line vanilla gives an unresolved profile, until it is resolved.

Pressing tab on the `<owner>` argument suggests the names of the players who are online and who you can see, or only
your own name without `yaminabe.command.skull.online`. A hash and a texture value are not suggested.

### The name of the head

This command does not set a custom name. A head that carries a player name is named by vanilla, as
「◯◯の頭」 is, and a head created from a hash or a texture value keeps the plain name of a player head. Use
[`/itemname`](itemname.md) to name it. The message this command replies with names a head created from a texture after
the first eight characters of its hash, as a whole hash is too long to read.

## Permissions

| Permission                       | Description                                                       |
|----------------------------------|-------------------------------------------------------------------|
| `yaminabe.command.skull`         | Required to run `/skull`, which gives your own head.              |
| `yaminabe.command.skull.online`  | Allows getting the head of another player who is online.          |
| `yaminabe.command.skull.offline` | Allows getting the head of a player who is not online.            |
| `yaminabe.command.skull.texture` | Allows getting a head from a texture hash or a texture value.     |

The three permissions under `yaminabe.command.skull` are independent of one another: which one is read is decided by the
argument alone, so a name that belongs to nobody online is never covered by `online`, whether or not that name belongs
to a player who has ever joined.

## Differences from EssentialsX

- EssentialsX modifies the player head held in the hand if there is one, and only spawns a new head otherwise. Yaminabe
  always gives a new head, so the permissions EssentialsX has for spawning (`essentials.skull.spawn`) and for
  overwriting an owner that is already set (`essentials.skull.modify`) have no counterpart here.
- EssentialsX takes the player to give the head to as a second argument, gated behind `essentials.skull.others` and
  `essentials.skull.spawn.others`. Yaminabe only gives the head to whoever runs the command; giving it to somebody else
  is done with `/execute as`.
- EssentialsX sets the name of the head to `Skull of <owner>`. Yaminabe sets no name and leaves the head named the way
  vanilla names it.
- EssentialsX asks the Mojang API for the profile of a name that is not online, on a task off the main thread. Yaminabe
  does not ask it, and stores the name for the client to resolve instead, as vanilla does.
- EssentialsX only accepts a texture value that is exactly 180 characters long. Yaminabe accepts a value of up to 1024
  characters that decodes to a skin URL, so that a longer value, such as a signed one, is accepted as well.
- EssentialsX suggests and resolves every player who is online. Yaminabe leaves out the ones the sender cannot see.
- EssentialsX gates a texture value behind the same permission as a player name (`essentials.skull.others`). Yaminabe
  gates it behind `yaminabe.command.skull.texture`, and separates an online player from an offline one as well.
- EssentialsX registers the aliases `playerskull` and `head`, among others. Yaminabe registers none, as `head` is
  already an alias of [`/hat`](hat.md).

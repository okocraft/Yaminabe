# `/seen`

Shows a recorded player's identity and last connection observation on this Paper/Folia server.

| Command | Description |
|---------|-------------|
| `/seen <name>` | Exact, case-insensitive lookup of the last known Minecraft account name. |
| `/seen <uuid>` | Lookup by canonical UUID, including offline players. |

Requires `yaminabe.command.seen`. No aliases are registered. The sender's permission is checked,
including when `/execute` changes the command executor, and checked again when the asynchronous
result is delivered. Targets are names or UUIDs, not selectors.

Output includes the name and UUID, then online status with the latest recorded join, offline status
with the latest confirmed departure, or an unconfirmed departure with the latest recorded join.
Times use UTC ISO-8601. Players hidden from a viewer through Bukkit's visibility API are not reported
online to that viewer. The console can see online status.

If several UUIDs share a last known name, the command lists their identities and requires a UUID;
it never guesses an account. Unknown players return no record rather than triggering an external
profile lookup or creating data. Storage errors have their own failure message.

Only connections observed after storage is enabled are recorded. EssentialsX userdata is not
imported automatically. A clean plugin disable closes tracking sessions; an abnormal termination
leaves their departure unconfirmed rather than treating an old departure as the latest one.

Unlike EssentialsX, this command does not expose IPs, alternate accounts, name history, bans,
locations or first-login data. See [Player storage](../player-storage.md) for the storage contract and
planned follow-up features.

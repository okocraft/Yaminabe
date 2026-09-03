#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import tempfile
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen
from zipfile import ZipFile

JENKINS_BUILD_API = (
    "https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/api/json"
    "?tree=number,artifacts[fileName,relativePath]"
)
JENKINS_ARTIFACT_URL = "https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/{path}"

# Java Edition 26.2 command labels. Vanilla aliases are included because an
# EssentialsX alias can shadow them too.
VANILLA_COMMAND_LABELS = frozenset({
    "advancement", "attribute", "ban", "ban-ip", "banlist", "bossbar",
    "clear", "clone", "damage", "data", "datapack", "debug",
    "defaultgamemode", "deop", "dialog", "difficulty", "effect", "enchant",
    "execute", "experience", "fetchprofile", "fill", "fillbiome", "forceload",
    "function", "gamemode", "gamerule", "give", "help", "item", "jfr",
    "kick", "kill", "list", "locate", "loot", "me", "msg", "op",
    "pardon", "pardon-ip", "particle", "place", "playsound", "publish",
    "random", "recipe", "reload", "return", "ride", "rotate", "save-all",
    "save-off", "save-on", "say", "schedule", "scoreboard", "seed", "setblock",
    "setidletimeout", "setworldspawn", "spawnpoint", "spectate", "spreadplayers",
    "stop", "stopsound", "summon", "swing", "tag", "team", "teammsg",
    "tell", "tellraw", "test", "tick", "time", "title", "tm", "teleport",
    "tp", "transfer", "trigger", "unpublish", "version", "w", "waypoint",
    "weather", "whitelist", "worldborder", "xp",
})

# Root labels and aliases already provided by Yaminabe.
YAMINABE_COMMAND_LABELS = frozenset({
    "anvil",
    "cartographytable",
    "disposal", "trash",
    "grindstone",
    "hat", "head",
    "item", "i",
    "itemlore", "lore", "ilore",
    "itemname", "iname",
    "loom",
    "ptime",
    "pweather",
    "sign", "editsign",
    "skull",
    "smithingtable",
    "stonecutter",
    "workbench", "craft",
})

TARGET_LABELS = VANILLA_COMMAND_LABELS | YAMINABE_COMMAND_LABELS
COMMAND_ENTRY = re.compile(r"^  ([A-Za-z0-9_-]+):\s*(?:#.*)?$")
ALIASES = re.compile(r"^    aliases:\s*(.*?)\s*$")
TOP_LEVEL_KEY = re.compile(r"^[^\s#][^:]*:\s*(?:#.*)?$")


def get_bytes(url: str) -> bytes:
    request = Request(url, headers={"User-Agent": "Yaminabe-EssentialsX-Patcher/1"})
    with urlopen(request, timeout=60) as response:
        return response.read()


def latest_core_artifact() -> tuple[int, str, str]:
    build = json.loads(get_bytes(JENKINS_BUILD_API))
    candidates = []

    for artifact in build.get("artifacts", []):
        filename = artifact.get("fileName", "")
        if not filename.startswith("EssentialsX-") or not filename.endswith(".jar"):
            continue
        if any(classifier in filename for classifier in ("-javadoc", "-sources", "-unshaded")):
            continue
        candidates.append((filename, artifact.get("relativePath", "")))

    if len(candidates) != 1:
        names = ", ".join(filename for filename, _ in candidates) or "none"
        raise RuntimeError(f"expected exactly one EssentialsX core artifact, found: {names}")

    filename, relative_path = candidates[0]
    if not relative_path:
        raise RuntimeError("EssentialsX core artifact has no relativePath")

    return int(build["number"]), filename, relative_path


def clean_alias(value: str) -> str:
    return value.strip().strip("'\"").lower()


def aliases_in(block: list[str]) -> set[str]:
    result: set[str] = set()

    for index, line in enumerate(block):
        match = ALIASES.match(line.rstrip("\r\n"))
        if match is None:
            continue

        value = match.group(1).strip()
        if value.startswith("[") and value.endswith("]"):
            for raw_alias in value[1:-1].split(","):
                alias = clean_alias(raw_alias)
                if alias:
                    result.add(alias)
        elif value:
            alias = clean_alias(value.split(" #", 1)[0])
            if alias:
                result.add(alias)
        else:
            for child in block[index + 1:]:
                child_text = child.rstrip("\r\n")
                if not child_text.startswith("      "):
                    break
                item = child_text.strip()
                if item.startswith("-"):
                    alias = clean_alias(item[1:])
                    if alias:
                        result.add(alias)
        break

    return result


def remove_aliases(block: list[str], labels: frozenset[str]) -> tuple[list[str], list[str]]:
    for index, line in enumerate(block):
        match = ALIASES.match(line.rstrip("\r\n"))
        if match is None:
            continue

        newline = "\r\n" if line.endswith("\r\n") else "\n" if line.endswith("\n") else ""
        value = match.group(1).strip()

        if value.startswith("[") and value.endswith("]"):
            raw_aliases = value[1:-1].split(",")
            kept = []
            removed = []
            for raw_alias in raw_aliases:
                alias = clean_alias(raw_alias)
                if not alias:
                    continue
                if alias in labels:
                    removed.append(alias)
                else:
                    kept.append(raw_alias.strip())

            if not removed:
                return block, []

            updated = list(block)
            if kept:
                updated[index] = f"    aliases: [{', '.join(kept)}]{newline}"
            else:
                del updated[index]
            return updated, sorted(set(removed))

        if value:
            alias = clean_alias(value.split(" #", 1)[0])
            if alias not in labels:
                return block, []
            updated = list(block)
            del updated[index]
            return updated, [alias]

        child_end = index + 1
        while child_end < len(block) and block[child_end].rstrip("\r\n").startswith("      "):
            child_end += 1

        removed = []
        kept_children = []
        for child in block[index + 1:child_end]:
            item = child.rstrip("\r\n").strip()
            if item.startswith("-"):
                alias = clean_alias(item[1:])
                if alias in labels:
                    removed.append(alias)
                    continue
            kept_children.append(child)

        if not removed:
            return block, []

        updated = list(block[:index])
        if kept_children:
            updated.append(line)
            updated.extend(kept_children)
        updated.extend(block[child_end:])
        return updated, sorted(set(removed))

    return block, []


def patch_plugin_yml(
    text: str,
) -> tuple[str, list[tuple[str, list[str]]], list[tuple[str, list[str]]], list[str]]:
    lines = text.splitlines(keepends=True)

    try:
        commands_start = next(
            index for index, line in enumerate(lines)
            if line.rstrip("\r\n") == "commands:"
        )
    except StopIteration as exc:
        raise RuntimeError("plugin.yml has no commands section") from exc

    commands_end = len(lines)
    for index in range(commands_start + 1, len(lines)):
        text_line = lines[index].rstrip("\r\n")
        if text_line and TOP_LEVEL_KEY.match(text_line):
            commands_end = index
            break

    entry_starts: list[tuple[int, str]] = []
    for index in range(commands_start + 1, commands_end):
        match = COMMAND_ENTRY.match(lines[index].rstrip("\r\n"))
        if match is not None:
            entry_starts.append((index, match.group(1).lower()))

    if not entry_starts:
        raise RuntimeError("plugin.yml commands section has no command entries")

    removed_entries: list[tuple[str, list[str]]] = []
    removed_aliases: list[tuple[str, list[str]]] = []
    remaining: list[str] = []
    output = lines[:commands_start + 1]
    cursor = commands_start + 1

    for position, (start, command_name) in enumerate(entry_starts):
        end = entry_starts[position + 1][0] if position + 1 < len(entry_starts) else commands_end
        output.extend(lines[cursor:start])

        block = lines[start:end]
        aliases = aliases_in(block)

        if command_name in TARGET_LABELS:
            matched = sorted(({command_name} | aliases) & TARGET_LABELS)
            removed_entries.append((command_name, matched))
        else:
            patched_block, matched_aliases = remove_aliases(block, TARGET_LABELS)
            if matched_aliases:
                removed_aliases.append((command_name, matched_aliases))
            remaining.append(command_name)
            output.extend(patched_block)
        cursor = end

    output.extend(lines[cursor:])

    if not removed_entries and not removed_aliases:
        raise RuntimeError("no EssentialsX command labels matched the removal labels")

    return "".join(output), removed_entries, removed_aliases, remaining


def patch_jar(
    source: Path,
    destination: Path,
) -> tuple[list[tuple[str, list[str]]], list[tuple[str, list[str]]], list[str]]:
    with ZipFile(source, "r") as input_jar:
        try:
            plugin_yml = input_jar.read("plugin.yml").decode("utf-8")
        except KeyError as exc:
            raise RuntimeError("EssentialsX jar has no plugin.yml") from exc

        patched_yml, removed_entries, removed_aliases, remaining = patch_plugin_yml(plugin_yml)
        destination.parent.mkdir(parents=True, exist_ok=True)

        with ZipFile(destination, "w") as output_jar:
            output_jar.comment = input_jar.comment
            for info in input_jar.infolist():
                data = patched_yml.encode("utf-8") if info.filename == "plugin.yml" else input_jar.read(info.filename)
                output_jar.writestr(info, data)

    return removed_entries, removed_aliases, remaining


def write_actions_summary(
    build_number: int,
    filename: str,
    destination: Path,
    removed_entries: list[tuple[str, list[str]]],
    removed_aliases: list[tuple[str, list[str]]],
    remaining: list[str],
) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    lines = [
        "# EssentialsX command patch",
        "",
        f"- Jenkins build: `#{build_number}`",
        f"- Source artifact: `{filename}`",
        f"- Patched jar: `{destination.name}`",
        f"- Removed command entries: **{len(removed_entries)}**",
        f"- Commands with removed aliases: **{len(removed_aliases)}**",
        f"- Remaining command entries: **{len(remaining)}**",
        "",
        "## Removed command entries",
        "",
        "| Command | Conflicting labels |",
        "| --- | --- |",
    ]
    lines.extend(
        f"| `{command_name}` | {', '.join(f'`{label}`' for label in matched)} |"
        for command_name, matched in sorted(removed_entries)
    )
    lines.extend([
        "",
        "## Removed aliases",
        "",
        "| Command | Removed aliases |",
        "| --- | --- |",
    ])
    lines.extend(
        f"| `{command_name}` | {', '.join(f'`{label}`' for label in matched)} |"
        for command_name, matched in sorted(removed_aliases)
    )
    lines.extend([
        "",
        "## Remaining command entries",
        "",
        "<details>",
        f"<summary>Show {len(remaining)} commands</summary>",
        "",
        "```text",
        *sorted(remaining),
        "```",
        "",
        "</details>",
        "",
    ])

    with Path(summary_path).open("a", encoding="utf-8") as summary:
        summary.write("\n".join(lines))


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Download the latest EssentialsX dev core jar and remove conflicting command declarations."
    )
    parser.add_argument("--output-dir", type=Path, default=Path("build/essentialsx"))
    args = parser.parse_args()

    build_number, filename, relative_path = latest_core_artifact()
    artifact_url = JENKINS_ARTIFACT_URL.format(path=quote(relative_path, safe="/"))

    print(f"Downloading Jenkins build #{build_number}: {filename}")
    with tempfile.TemporaryDirectory() as temp_dir:
        source = Path(temp_dir) / filename
        source.write_bytes(get_bytes(artifact_url))

        destination = args.output_dir / f"{source.stem}-patched.jar"
        removed_entries, removed_aliases, remaining = patch_jar(source, destination)

    print("Removed EssentialsX command entries:")
    for command_name, matched in sorted(removed_entries):
        print(f"  - {command_name}: {', '.join(matched)}")
    print("Removed EssentialsX command aliases:")
    for command_name, matched in sorted(removed_aliases):
        print(f"  - {command_name}: {', '.join(matched)}")
    print("Remaining EssentialsX command entries:")
    for command_name in sorted(remaining):
        print(f"  - {command_name}")
    print(f"Patched jar: {destination}")

    write_actions_summary(build_number, filename, destination, removed_entries, removed_aliases, remaining)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
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


def patch_plugin_yml(text: str) -> tuple[str, list[tuple[str, list[str]]]]:
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

    removed: list[tuple[str, list[str]]] = []
    output = lines[:commands_start + 1]
    cursor = commands_start + 1

    for position, (start, command_name) in enumerate(entry_starts):
        end = entry_starts[position + 1][0] if position + 1 < len(entry_starts) else commands_end
        output.extend(lines[cursor:start])

        block = lines[start:end]
        labels = {command_name} | aliases_in(block)
        matched = sorted(labels & TARGET_LABELS)
        if matched:
            removed.append((command_name, matched))
        else:
            output.extend(block)
        cursor = end

    output.extend(lines[cursor:])

    if not removed:
        raise RuntimeError("no EssentialsX command entries matched the removal labels")

    return "".join(output), removed


def patch_jar(source: Path, destination: Path) -> list[tuple[str, list[str]]]:
    with ZipFile(source, "r") as input_jar:
        try:
            plugin_yml = input_jar.read("plugin.yml").decode("utf-8")
        except KeyError as exc:
            raise RuntimeError("EssentialsX jar has no plugin.yml") from exc

        patched_yml, removed = patch_plugin_yml(plugin_yml)
        destination.parent.mkdir(parents=True, exist_ok=True)

        with ZipFile(destination, "w") as output_jar:
            output_jar.comment = input_jar.comment
            for info in input_jar.infolist():
                data = patched_yml.encode("utf-8") if info.filename == "plugin.yml" else input_jar.read(info.filename)
                output_jar.writestr(info, data)

    return removed


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
        removed = patch_jar(source, destination)

    print("Removed EssentialsX command entries:")
    for command_name, matched in removed:
        print(f"  - {command_name}: {', '.join(matched)}")
    print(f"Patched jar: {destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

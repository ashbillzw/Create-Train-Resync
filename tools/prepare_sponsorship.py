"""Prepare world/data/trainresync_sponsorship.dat offline. Requires NBT 1.5.1."""
import argparse
from collections import Counter
import io
from nbt import nbt
from datetime import date, datetime, time, timedelta, timezone
import gzip
import json
import os
from pathlib import Path
import re
import tempfile
from uuid import UUID

HEADER = re.compile(r"^\[([^\]]+)\] \[([^\]]+)/INFO\](?: \[([^\]]*)\])?: (.*)$")
AUTH = re.compile(r"UUID of player (\w+) is ([\da-fA-F-]{36})$")
LOGIN = re.compile(r"(\w+)\[[^\r\n]*\] logged in with entity id -?\d+ at \(")
MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split()


def timestamp(stamp, path, zone):
    if " " in stamp:
        day, clock = stamp.split()
        day = date(int(day[5:]), MONTHS.index(day[2:5]) + 1, int(day[:2]))
    elif re.match(r"^\d{4}-\d{2}-\d{2}", path.name):
        day, clock = date.fromisoformat(path.name[:10]), stamp
    else:
        return None
    return int(datetime.combine(day, time.fromisoformat(clock), zone).timestamp() * 1000)


def scan(path, records, zone):
    # Only charge intervals with both endpoints in this log. A crash or missing
    # archive must not turn days spent offline into playtime.
    pending, online = {}, {}
    def finish(name, end):
        if name in online:
            uuid, start = online.pop(name)
            if end > start:
                records[uuid]["intervals"].append((start, end))

    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as lines:
        for line in lines:
            if not any(token in line for token in ("UUID of player ", " logged in with entity id ",
                    " lost connection:", " left the game", "Stopping server", "Starting minecraft server")):
                continue
            header = HEADER.fullmatch(line.rstrip("\r\n"))
            if not header:
                continue
            stamp, thread, logger, message = header.groups()
            if thread.startswith("User Authenticator") and (match := AUTH.fullmatch(message)):
                pending[match[1]] = str(UUID(match[2]))
                continue
            if thread != "Server thread":
                continue
            instant = timestamp(stamp, path, zone)
            if instant is None:
                continue
            if (not logger or "PlayerList" in logger) and (match := LOGIN.match(message)):
                name = match[1]
                online.pop(name, None)
                uuid = pending.pop(name, None)
                if uuid:
                    entry = records.setdefault(uuid, {"firstSeen": instant, "intervals": []})
                    entry["firstSeen"] = min(entry["firstSeen"], instant)
                    online[name] = uuid, instant
            elif (not logger or "ServerGamePacketListener" in logger or "MinecraftServer" in logger) and (
                    match := re.fullmatch(r"(\w+) (?:lost connection:.*|left the game)", message)):
                finish(match[1], instant)
            elif message == "Stopping server":
                for name in list(online):
                    finish(name, instant)
                pending.clear()
            elif message.startswith("Starting minecraft server version"):
                online.clear()
                pending.clear()


def active_ticks(intervals):
    """Count the union of confirmed online intervals across the entire archive."""
    milliseconds = end = 0
    for start, stop in sorted(intervals):
        milliseconds += max(0, stop - max(start, end))
        end = max(end, stop)
    return milliseconds // 50


def player_files(world, directory, extension):
    result = {}
    for path in (world / directory).glob("*" + extension):
        try:
            result[str(UUID(path.stem))] = path
        except ValueError:
            continue
    return result


def advancement_time(path):
    dates = (date for value in json.loads(path.read_bytes()).values() if isinstance(value, dict)
             for date in value.get("criteria", {}).values())
    return min((int(datetime.strptime(d, "%Y-%m-%d %H:%M:%S %z").timestamp() * 1000)
                for d in dates), default=None)


def legacy_record(path):
    with gzip.open(path, "rb") as source:
        raw = source.read()
    if b"trainresync:sponsorship" not in raw:
        return {}
    root = nbt.NBTFile(buffer=io.BytesIO(raw))
    return root.get("ForgeData", {}).get("PlayerPersisted", {}).get("trainresync:sponsorship", {})


def prepare(world, logs, output, zone):
    current = world / "data" / "trainresync_sponsorship.dat"
    if current.is_file():
        root = nbt.NBTFile(str(current))
        players = root["data"]["players"]
    else:
        root = nbt.NBTFile()
        version = nbt.NBTFile(str(world/"level.dat"))["Data"]["DataVersion"].value
        root["DataVersion"] = nbt.TAG_Int(version)
        root["data"] = nbt.TAG_Compound()
        players = nbt.TAG_Compound()
        root["data"]["players"] = players
    kept = len(players)
    # Always rebuild missing players from the full set of logs, never add archived totals.
    history = {}
    for path in sorted(logs.iterdir()):
        if path.name.endswith((".log", ".log.gz")) and not path.name.startswith("debug"):
            scan(path, history, zone)
    stats = player_files(world, "stats", ".json")
    advancements = player_files(world, "advancements", ".json")
    playerdata = player_files(world, "playerdata", ".dat")
    counts = Counter()
    now = int(datetime.now().timestamp() * 1000)
    for uuid in sorted(history.keys() | stats.keys() | advancements.keys() | playerdata.keys()):
        if uuid in players:
            continue
        legacy = legacy_record(playerdata[uuid]) if uuid in playerdata else {}
        entry = history.get(uuid, {})
        dates = [now]
        if "firstSeen" in entry:
            dates.append(entry["firstSeen"])
        if "firstSeen" in legacy and legacy["firstSeen"].value > 0:
            dates.append(legacy["firstSeen"].value)
        if uuid in advancements:
            earned = advancement_time(advancements[uuid])
            if earned is not None:
                dates.append(earned)
        ticks = 0
        if uuid in stats:
            vanilla = json.loads(stats[uuid].read_bytes()).get("stats", {})
            ticks = vanilla.get("minecraft:custom", {}).get("minecraft:play_time", 0)
        if ticks > 0:
            counts["vanilla"] += 1
        else:
            ticks = active_ticks(entry.get("intervals", []))
            counts["logs" if ticks else "zero"] += 1
        if "spectatorTicks" in legacy:
            ticks = max(0, ticks - legacy["spectatorTicks"].value)
        record = nbt.TAG_Compound()
        record["firstSeen"] = nbt.TAG_Long(min(dates))
        record["playedTicks"] = nbt.TAG_Long(ticks)
        players[uuid] = record

    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=output.parent, delete=False) as dest:
        temp = Path(dest.name)
    try:
        root.write_file(filename=str(temp))
        os.replace(temp, output)
    finally:
        temp.unlink(missing_ok=True)
    print(f"{world.name}: {len(players)} players; kept {kept}; vanilla {counts['vanilla']}; "
          f"logs {counts['logs']}; no duration {counts['zero']}; {output}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path, help="stopped world or a consistent snapshot")
    parser.add_argument("logs", type=Path, help="complete logs directory for that same server")
    parser.add_argument("output", type=Path, help="destination trainresync_sponsorship.dat")
    parser.add_argument("--utc-offset", type=int, default=8)
    args = parser.parse_args()
    if not (args.world/"level.dat").is_file():
        parser.error("world must contain level.dat")
    if args.output.name != "trainresync_sponsorship.dat":
        parser.error("output must be named trainresync_sponsorship.dat")
    prepare(args.world, args.logs, args.output, timezone(timedelta(hours=args.utc_offset)))


if __name__ == "__main__":
    main()


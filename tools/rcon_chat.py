"""Collect public player chat from the two Niijima servers over LAN RCON."""
import argparse
import json
import os
from pathlib import Path
import re
import socket
import sqlite3
import struct
import subprocess
import sys
import threading
import time


PORTS = {"create-cn": 25601, "train-cn": 25575}
MENTION = re.compile(r"@astra(?![a-z0-9_])", re.I)


def password():
    script = r"""
    $ErrorActionPreference='Stop'
    Add-Type -AssemblyName System.Security
    $p = Join-Path $env:LOCALAPPDATA 'NiijimaRcon\local-servers.dpapi'
    $b = [Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($p), $null, 'CurrentUser')
    [Console]::OpenStandardOutput().Write($b, 0, $b.Length)
    """
    return subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script],
                          check=True, stdout=subprocess.PIPE).stdout


def request(stream, kind, body):
    stream.write(struct.pack("<iii", 10 + len(body), 1, kind) + body + b"\0\0")
    stream.flush()
    header = stream.read(4)
    if len(header) != 4:
        raise ConnectionError("RCON disconnected")
    size, = struct.unpack("<i", header)
    if not 10 <= size <= 65536:
        raise ValueError("invalid RCON response length")
    data = stream.read(size)
    if len(data) != size or data[-2:] != b"\0\0":
        raise ConnectionError("incomplete RCON response")
    seq, packet_type = struct.unpack_from("<ii", data)
    if seq != 1 or packet_type != (2 if kind == 3 else 0):
        raise ValueError("RCON request rejected")
    return data[8:-2].decode("utf-8")


def open_db(path):
    db = sqlite3.connect(path, timeout=10)
    db.execute("CREATE TABLE IF NOT EXISTS cursors(server PRIMARY KEY, epoch, pos INTEGER)")
    db.execute("""CREATE TABLE IF NOT EXISTS chat(
        server, epoch, seq INTEGER, time INTEGER, uuid, name, text, mention INTEGER,
        UNIQUE(server,epoch,seq))""")
    return db


def receive(db, server, stream):
    epoch, pos = db.execute("SELECT epoch,pos FROM cursors WHERE server=?", (server,)).fetchone() or ("", 0)
    command = f"niijima-chat since {pos} {epoch}".strip()
    raw = request(stream, 2, command.encode())
    if not raw.startswith("{"):
        raise ValueError("niijima-chat command unavailable")
    page = json.loads(raw)
    fresh = []
    with db:
        for msg in page["messages"]:
            mention = bool(MENTION.search(msg["text"]))
            row = db.execute("INSERT OR IGNORE INTO chat VALUES(?,?,?,?,?,?,?,?)",
                             (server, page["epoch"], msg["id"], msg["time"], msg["uuid"],
                              msg["name"], msg["text"], mention))
            if row.rowcount:
                fresh.append(dict(server=server, mention=mention, **msg))
        db.execute("INSERT OR REPLACE INTO cursors VALUES(?,?,?)", (server, page["epoch"], page["next"]))
        if fresh:
            db.execute("""DELETE FROM chat WHERE server=? AND rowid <
                (SELECT rowid FROM chat WHERE server=? ORDER BY rowid DESC LIMIT 1 OFFSET 999)""", (server, server))
    if page["gap"]:
        print(f"{server}: server restarted or chat history expired", file=sys.stderr, flush=True)
    for msg in fresh:
        print(json.dumps(msg, ensure_ascii=False), flush=True)
    return page["next"] < page["latest"]


def listen(server, secret, path, stop):
    db = open_db(path)
    last_error = None
    while not stop.is_set():
        try:
            with socket.create_connection(("192.168.1.222", PORTS[server]), timeout=4) as sock, sock.makefile("rwb") as stream:
                request(stream, 3, secret)
                while not stop.is_set():
                    more = receive(db, server, stream)
                    if last_error is not None:
                        print(f"{server}: listening", file=sys.stderr, flush=True)
                    last_error = None
                    if not more:
                        stop.wait(2)
        except (OSError, ValueError, KeyError, sqlite3.Error) as exc:
            if str(exc) != last_error:
                print(f"{server}: {exc}", file=sys.stderr, flush=True)
                last_error = str(exc)
            stop.wait(3)
    db.close()


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="mode", required=True)
    sub.add_parser("listen")
    recent = sub.add_parser("recent")
    recent.add_argument("--mentions", action="store_true")
    recent.add_argument("--after", type=int, default=0, help="Unix time in milliseconds")
    reply = sub.add_parser("reply")
    reply.add_argument("server", choices=PORTS)
    reply.add_argument("message")
    args = parser.parse_args()
    path = Path(os.environ["LOCALAPPDATA"]) / "NiijimaRcon" / "chat.sqlite"
    if args.mode == "recent":
        with open_db(path) as db:
            rows = db.execute("""SELECT server,time,uuid,name,text FROM chat
                WHERE time >= ? AND mention >= ? ORDER BY time,rowid""", (args.after, int(args.mentions)))
            for row in rows:
                print(json.dumps(dict(zip(("server", "time", "uuid", "name", "text"), row)), ensure_ascii=False))
        return
    secret = password()
    if args.mode == "reply":
        with socket.create_connection(("192.168.1.222", PORTS[args.server]), timeout=4) as sock, sock.makefile("rwb") as stream:
            request(stream, 3, secret)
            command = "tellraw @a " + json.dumps({"text": "[astra] " + args.message}, ensure_ascii=False)
            print(request(stream, 2, command.encode()))
        return
    stop = threading.Event()
    workers = [threading.Thread(target=listen, args=(server, secret, path, stop)) for server in PORTS]
    for worker in workers:
        worker.start()
    try:
        while all(worker.is_alive() for worker in workers):
            time.sleep(0.5)
    except KeyboardInterrupt:
        pass
    finally:
        stop.set()
        for worker in workers:
            worker.join()


if __name__ == "__main__":
    main()

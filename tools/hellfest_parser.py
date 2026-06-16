#!/usr/bin/env python3
"""Parse the Hellfest 2026 Clashfinder into the app's bundled JSON."""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
import unicodedata
import urllib.request
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable

SOURCE_URL = "https://clashfinder.com/s/rohellfest/"
STAGES = {"MAINSTAGE 1", "MAINSTAGE 2", "WARZONE", "VALLEY", "ALTAR", "TEMPLE"}
DATE_PATTERN = re.compile(r"Clashfinder\s+(\d{1,2})/(\d{1,2})/(\d{2})", re.I)
TIME_PATTERN = re.compile(r"^(\d{2}):(\d{2})\s*-\s*(\d{2}):(\d{2})$")
DEFAULT_INPUT = Path("tools/input/rohellfest.html")
DEFAULT_OUTPUT = Path("composeApp/src/commonMain/composeResources/files/hellfest_2026.json")
PARIS_SUMMER = timezone(timedelta(hours=2))


@dataclass(frozen=True)
class Token:
    kind: str
    value: str


class ClashfinderHtmlParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.tokens: list[Token] = []
        self.capture: str | None = None
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        classes = set(dict(attrs).get("class", "").split())
        kind = None
        if "headingDayName" in classes:
            kind = "date"
        elif "stageName" in classes:
            kind = "stage"
        elif "actNm" in classes:
            kind = "artist"
        elif "actTime" in classes:
            kind = "time"
        if kind:
            self.capture = kind
            self.parts = []

    def handle_data(self, data: str) -> None:
        if self.capture:
            self.parts.append(data)

    def handle_endtag(self, tag: str) -> None:
        if self.capture and tag in {"span", "p", "h6"}:
            value = html.unescape("".join(self.parts)).strip()
            if value:
                self.tokens.append(Token(self.capture, value))
            self.capture = None
            self.parts = []


def normalize(value: str) -> str:
    ascii_value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode()
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", ascii_value.lower())).strip("-")


def parse_date(value: str) -> date | None:
    match = DATE_PATTERN.search("Clashfinder " + value if "/" in value else value)
    if not match:
        return None
    day, month, year = map(int, match.groups())
    return date(2000 + year, month, day)


def parse_tokens(tokens: Iterable[Token]) -> tuple[list[dict[str, str]], list[str]]:
    current_day: date | None = None
    current_stage: str | None = None
    pending_artist: str | None = None
    records: list[dict[str, str]] = []
    warnings: list[str] = []

    for token in tokens:
        if token.kind == "date":
            current_day = parse_date(token.value)
            current_stage = None
            pending_artist = None
        elif token.kind == "stage":
            current_stage = token.value.strip().upper()
            pending_artist = None
        elif token.kind == "artist":
            if pending_artist:
                warnings.append(f"Missing or malformed time for {pending_artist}")
            pending_artist = token.value.strip()
        elif token.kind == "time" and pending_artist:
            match = TIME_PATTERN.match(token.value.strip())
            if not match:
                warnings.append(f"Malformed time '{token.value}' for {pending_artist}")
                pending_artist = None
                continue
            if not current_day or not current_stage:
                warnings.append(f"Missing day/stage for {pending_artist}")
                pending_artist = None
                continue
            if current_stage not in STAGES:
                pending_artist = None
                continue

            sh, sm, eh, em = map(int, match.groups())
            start_day = current_day + (timedelta(days=1) if sh < 6 else timedelta())
            end_day = start_day
            start_dt = datetime.combine(start_day, time(sh, sm), PARIS_SUMMER)
            end_dt = datetime.combine(end_day, time(eh, em), PARIS_SUMMER)
            if end_dt <= start_dt:
                end_dt += timedelta(days=1)

            day_string = current_day.isoformat()
            start_label = f"{sh:02d}{sm:02d}"
            records.append(
                {
                    "id": f"{day_string}-{normalize(current_stage)}-{normalize(pending_artist)}-{start_label}",
                    "artist": pending_artist,
                    "normalizedArtist": normalize(pending_artist).replace("-", " "),
                    "stage": current_stage,
                    "day": day_string,
                    "start": start_dt.isoformat(),
                    "end": end_dt.isoformat(),
                    "source": "clashfinder",
                    "sourceUrl": SOURCE_URL,
                }
            )
            pending_artist = None

    if pending_artist:
        warnings.append(f"Missing or malformed time for {pending_artist}")
    records.sort(key=lambda item: (item["start"], item["stage"]))
    return records, warnings


def parse_html(content: str) -> tuple[list[dict[str, str]], list[str]]:
    parser = ClashfinderHtmlParser()
    parser.feed(content)
    return parse_tokens(parser.tokens)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "HellfestPlanner/0.1"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="replace")


def validate(records: list[dict[str, str]], warnings: list[str]) -> None:
    print(f"Parsed {len(records)} sets.")
    if len(records) < 150:
        print("WARNING: fewer than 150 acts were parsed.", file=sys.stderr)
    missing = [
        item.get("id", "<unknown>")
        for item in records
        if not all(item.get(field) for field in ("artist", "stage", "start", "end"))
    ]
    if missing:
        print(f"WARNING: {len(missing)} sets have missing required fields.", file=sys.stderr)
    for warning in warnings:
        print(f"WARNING: {warning}", file=sys.stderr)


def main() -> int:
    args = argparse.ArgumentParser()
    args.add_argument("--input", type=Path, default=None, help="Read saved Clashfinder HTML")
    args.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args.add_argument("--url", default=SOURCE_URL)
    args.add_argument("--save-source", type=Path, default=DEFAULT_INPUT)
    options = args.parse_args()

    if options.input:
        content = options.input.read_text(encoding="utf-8")
    else:
        try:
            content = fetch(options.url)
            options.save_source.parent.mkdir(parents=True, exist_ok=True)
            options.save_source.write_text(content, encoding="utf-8")
        except Exception as error:
            if DEFAULT_INPUT.exists():
                print(f"Fetch failed ({error}); using {DEFAULT_INPUT}.", file=sys.stderr)
                content = DEFAULT_INPUT.read_text(encoding="utf-8")
            else:
                raise

    records, warnings = parse_html(content)
    options.output.parent.mkdir(parents=True, exist_ok=True)
    options.output.write_text(json.dumps(records, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    validate(records, warnings)
    return 0 if records else 1


if __name__ == "__main__":
    raise SystemExit(main())

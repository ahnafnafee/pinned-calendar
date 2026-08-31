#!/usr/bin/env python3
"""Play listing updates over the Android Publisher API.

Three operations, all against an edit that is committed atomically:
  --listings              sync every localized store/listings/<locale>.md file
  --screenshots            replace the en-US phone screenshots with store/screenshots/*.png
                           (carousel order = filename sort order)
  --promote CODE           promote the release with versionCode CODE from the internal
                           track to production, carrying its release notes along

Credentials come from the PLAY_SERVICE_ACCOUNT_JSON environment variable (the key file's
JSON content, not a path), matching the secret the Release workflow already uses.
Requires: google-api-python-client, google-auth.
"""
import argparse
import json
import os
import sys
from pathlib import Path

PACKAGE = "dev.ahnafnafee.pinnedcalendar"
LANGUAGE = "en-US"
REPO = Path(__file__).resolve().parents[1]
SCREENSHOTS = REPO / "store" / "screenshots"
LISTINGS = REPO / "store" / "listings"
EXPECTED_LISTING_LOCALES = frozenset({
    "ar", "bn-BD", "de-DE", "en-US", "es-ES", "fr-FR", "hi-IN", "id",
    "it-IT", "ja-JP", "ko-KR", "pt-BR", "tr-TR", "vi", "zh-CN",
})
LISTING_LIMITS = {
    "title": 30,
    "shortDescription": 80,
    "fullDescription": 4_000,
}


def publisher():
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    raw = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        sys.exit("PLAY_SERVICE_ACCOUNT_JSON is not set")
    creds = service_account.Credentials.from_service_account_info(
        json.loads(raw), scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def parse_listing(path: Path) -> dict[str, str]:
    """Parse the header-driven Markdown format used by store/listings."""
    title = ""
    section = None
    sections = {"Short description": [], "Full description": []}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# ") and not title:
            title = line[2:].strip()
        elif line.startswith("## "):
            section = line[3:].strip()
            if section not in sections:
                sys.exit(f"{path.name}: unknown section '## {section}'")
        elif section is not None:
            sections[section].append(line)

    body = {
        "language": path.stem,
        "title": title,
        "shortDescription": "\n".join(sections["Short description"]).strip(),
        "fullDescription": "\n".join(sections["Full description"]).strip(),
    }
    for field, limit in LISTING_LIMITS.items():
        value = body[field]
        if not value:
            sys.exit(f"{path.name}: {field} is empty")
        if len(value) > limit:
            sys.exit(f"{path.name}: {field} is {len(value)} characters; Play allows {limit}")
    return body


def localized_listings() -> list[dict[str, str]]:
    paths = sorted(LISTINGS.glob("*.md"))
    locales = {path.stem for path in paths}
    missing = sorted(EXPECTED_LISTING_LOCALES - locales)
    extra = sorted(locales - EXPECTED_LISTING_LOCALES)
    if missing or extra:
        details = []
        if missing:
            details.append(f"missing: {', '.join(missing)}")
        if extra:
            details.append(f"unexpected: {', '.join(extra)}")
        sys.exit("store/listings locale set is incomplete (" + "; ".join(details) + ")")
    return [parse_listing(path) for path in paths]


def sync_listings(api, edit_id):
    from googleapiclient.errors import HttpError

    bodies = localized_listings()
    for listing in bodies:
        language = listing["language"]
        body = {key: value for key, value in listing.items() if key != "language"}
        try:
            api.edits().listings().patch(
                packageName=PACKAGE, editId=edit_id, language=language, body=body,
            ).execute()
            print(f"updated listing {language}")
        except HttpError as error:
            if error.resp.status != 404:
                raise
            api.edits().listings().update(
                packageName=PACKAGE,
                editId=edit_id,
                language=language,
                body={**body, "language": language},
            ).execute()
            print(f"created listing {language}")
    print(f"synced {len(bodies)} localized listings")


def sync_screenshots(api, edit_id):
    from googleapiclient.http import MediaFileUpload

    shots = sorted(SCREENSHOTS.glob("*.png"))
    if not shots:
        sys.exit(f"no screenshots found under {SCREENSHOTS}")
    api.edits().images().deleteall(
        packageName=PACKAGE, editId=edit_id, language=LANGUAGE, imageType="phoneScreenshots",
    ).execute()
    for shot in shots:
        api.edits().images().upload(
            packageName=PACKAGE, editId=edit_id, language=LANGUAGE, imageType="phoneScreenshots",
            media_body=MediaFileUpload(str(shot), mimetype="image/png"),
        ).execute()
        print(f"uploaded {shot.name}")
    print(f"replaced phone screenshots with {len(shots)} images")


def promote(api, edit_id, version_code):
    internal = api.edits().tracks().get(
        packageName=PACKAGE, editId=edit_id, track="internal",
    ).execute()
    release = next(
        (r for r in internal.get("releases", [])
         if str(version_code) in [str(c) for c in r.get("versionCodes", [])]),
        None,
    )
    if release is None:
        sys.exit(f"versionCode {version_code} is not on the internal track; upload it first")
    body = {
        "releases": [{
            "versionCodes": [str(version_code)],
            "status": "completed",
            **({"name": release["name"]} if "name" in release else {}),
            **({"releaseNotes": release["releaseNotes"]} if "releaseNotes" in release else {}),
        }],
    }
    api.edits().tracks().update(
        packageName=PACKAGE, editId=edit_id, track="production", body=body,
    ).execute()
    print(f"promoted versionCode {version_code} to production (full rollout)")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validate", action="store_true", help="validate localized listing files without contacting Play")
    parser.add_argument("--listings", action="store_true", help="sync localized listing text")
    parser.add_argument("--screenshots", action="store_true", help="replace phone screenshots")
    parser.add_argument("--promote", type=int, metavar="CODE", help="promote CODE from internal to production")
    args = parser.parse_args()
    if not args.validate and not args.listings and not args.screenshots and args.promote is None:
        parser.error("nothing to do: pass --validate, --listings, --screenshots, and/or --promote CODE")

    # Validate all listing files before opening an edit or touching Play.
    if args.validate or args.listings:
        listings = localized_listings()
        print(f"validated {len(listings)} localized listings")
        if args.validate and not args.listings and not args.screenshots and args.promote is None:
            return

    api = publisher()
    edit_id = api.edits().insert(packageName=PACKAGE, body={}).execute()["id"]
    if args.listings:
        sync_listings(api, edit_id)
    if args.screenshots:
        sync_screenshots(api, edit_id)
    if args.promote is not None:
        promote(api, edit_id, args.promote)
    api.edits().validate(packageName=PACKAGE, editId=edit_id).execute()
    result = api.edits().commit(packageName=PACKAGE, editId=edit_id).execute()
    print(f"edit {result['id']} committed")


if __name__ == "__main__":
    main()

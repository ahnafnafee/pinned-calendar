#!/usr/bin/env python3
"""Play listing updates over the Android Publisher API.

Two operations, both against an edit that is committed atomically:
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

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE = "dev.ahnafnafee.pinnedcalendar"
LANGUAGE = "en-US"
REPO = Path(__file__).resolve().parents[1]
SCREENSHOTS = REPO / "store" / "screenshots"


def publisher():
    raw = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        sys.exit("PLAY_SERVICE_ACCOUNT_JSON is not set")
    creds = service_account.Credentials.from_service_account_info(
        json.loads(raw), scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def sync_screenshots(api, edit_id):
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
    parser.add_argument("--screenshots", action="store_true", help="replace phone screenshots")
    parser.add_argument("--promote", type=int, metavar="CODE", help="promote CODE from internal to production")
    args = parser.parse_args()
    if not args.screenshots and args.promote is None:
        parser.error("nothing to do: pass --screenshots and/or --promote CODE")

    api = publisher()
    edit_id = api.edits().insert(packageName=PACKAGE, body={}).execute()["id"]
    if args.screenshots:
        sync_screenshots(api, edit_id)
    if args.promote is not None:
        promote(api, edit_id, args.promote)
    api.edits().validate(packageName=PACKAGE, editId=edit_id).execute()
    result = api.edits().commit(packageName=PACKAGE, editId=edit_id).execute()
    print(f"edit {result['id']} committed")


if __name__ == "__main__":
    main()

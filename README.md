# Hellfest Planner 2026

Android-first Kotlin Multiplatform + Compose MVP for building an offline Hellfest 2026 plan from the Clashfinder schedule.

## Features

- Bundled offline schedule with 183 sets across June 18-21, 2026.
- Day, stage, and artist filters with quick favorite toggles.
- Chronological personal plan, Today filter, Now/Next, and overlap warnings.
- Compact JSON plan export/import and friend comparison.
- Spotify matching for followed artists and top artists.
- Persistent on-device selections using Android `SharedPreferences`.
- Debug/status screen with bundled-data reload and selection reset.

## Run

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew :composeApp:installDebug
```

Or open the repository in Android Studio and run the `composeApp` configuration. The generated debug APK is:

```text
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Regenerate the schedule

Fetch the live Clashfinder page and write both the local source copy and bundled JSON:

```bash
python3 tools/hellfest_parser.py
```

Parse a previously saved page without network access:

```bash
python3 tools/hellfest_parser.py --input tools/input/rohellfest.html
```

The parser prints the total number of sets, warns below 150, reports malformed/missing fields, sorts by start then stage, and handles sets that cross midnight. Times before 06:00 are assigned to the next calendar date while retaining their festival lineup day.

Parser tests:

```bash
python3 -m unittest discover -s tools -p 'test_*.py' -v
```

Kotlin tests and Android build:

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug
```

## Spotify matching

Create a Spotify Developer app at <https://developer.spotify.com/dashboard>, then add this redirect URI:

```text
hellfestplanner://spotify-auth
```

In the app, open `Settings`, paste the Spotify `client_id`, tap `Connect`, approve the scopes, then return and tap `Sync`. The app requests only:

- `user-follow-read` to find Hellfest artists you follow.
- `user-top-read` to find Hellfest artists in your Spotify top artists.
- `user-library-read` to find Hellfest artists from your saved tracks.
- `playlist-modify-private` and `user-read-private` to create private Spotify playlists.

Matched bands get Spotify badges in the schedule, the `Schedule` screen has a `Spotify` filter chip after sync, and `Settings` can add all matched Spotify bands to your planner.

## JSON schema

The bundled file is [hellfest_2026.json](composeApp/src/commonMain/composeResources/files/hellfest_2026.json). Each entry contains:

```json
{
  "id": "2026-06-18-mainstage-1-we-came-as-romans-1630",
  "artist": "WE CAME AS ROMANS",
  "normalizedArtist": "we came as romans",
  "stage": "MAINSTAGE 1",
  "day": "2026-06-18",
  "start": "2026-06-18T16:30:00+02:00",
  "end": "2026-06-18T17:10:00+02:00",
  "source": "clashfinder",
  "sourceUrl": "https://clashfinder.com/s/rohellfest/"
}
```

Friend plans use compact JSON with `userName`, `festival`, `exportedAt`, and `selectedSetIds`.

## Known limitations

- Android is the shipped target; iOS UI/persistence has not been added.
- Clashfinder is community-maintained and lineup times may change.
- Friend sharing uses the device clipboard and paste rather than links or backend sync.
- Spotify artist matching is name-based, so unusual aliases or collaborations may need manual selection.
- Now/Next uses the device clock converted to `Europe/Paris`; there are no notifications.
- Build date is a fixed MVP metadata value rather than CI-generated.

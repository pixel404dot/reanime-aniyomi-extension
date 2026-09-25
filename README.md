# Re:ANIME Aniyomi / Anikku extension

Unofficial source for https://reanime.to

**Use Aniyomi or Anikku. This will not work in Tachiyomi / Mihon.**

## Install

1. Wait for GitHub Actions to finish on this repo (`Actions` tab).
2. Open the newest **Release**.
3. Download the `.apk`.
4. On your phone: allow install from unknown sources for your browser/Files.
5. Install the APK.
6. Open Aniyomi / Anikku → Browse → Extensions → find **Re:ANIME** under Untrusted → **Trust**.
7. Open Sources and test Popular / Latest / Search.

## What works

- Popular catalog
- Latest aired
- Search
- Anime details
- Episode list

## What does not work yet

Playback. Re:ANIME does not publish stream URLs on its public API. Watch/server endpoints return 401.

## Build locally

The workflow copies this module into [yuzono/anime-extensions](https://github.com/yuzono/anime-extensions) and runs:

```
./gradlew :src:en:reanime:assembleDebug
```

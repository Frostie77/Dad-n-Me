# Dad 'n Me — Android starter

An **uncompiled, first-pass test project** for loading the original Flash game in a standalone Ruffle-based Android app. Target: AYN Odin 2 Portal.

This is **separate from Arcane** and does not include the game SWF. On first launch, use **Select SWF File** to import your own copy privately.

## Test controller inputs

- Left stick or D-pad: movement (arrow keys)
- A: light attack (Flash A key)
- X: heavy attack (Flash S key)

The mapping and game load have **not yet been tested on the Odin**. They may need adjustment.

## Build on Windows

In PowerShell, with this folder open, run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build-test.ps1
```

The script downloads Ruffle assets, builds using Android SDK/JDK 17/Gradle, and installs the test APK if an authorized device is connected. The paths are tuned for the existing Arcane Windows development setup.

**Future milestone:** once we verify the SWF displays and controls work, integrate FrostFrontend (including the universal Speaker Test and game-specific control profile). Nothing here modifies Arcane or the released FrostFrontend library.

## Disclaimer

This is an unofficial compatibility project. Dad 'n Me, its assets and music belong to their original copyright holders. No game data is included. Ruffle is an independent open-source Flash runtime.

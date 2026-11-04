# Dad 'n Me — Android

An unofficial Android port of the classic Flash beat 'em up **Dad 'n Me**, powered by [Ruffle](https://ruffle.rs) and featuring [FrostFrontend](https://github.com/Frostie77/FrostFrontend).

Developed and tested on the **AYN Odin 2 Portal**.

## Download and installation

1. Download the [latest published Android APK](https://github.com/Frostie77/Dad-n-Me/releases/latest) and install it.
2. Get the original Flash game from [Dad 'n Me on Newgrounds](https://www.newgrounds.com/portal/view/254456), or try the [original Newgrounds SWF file](https://uploads.ungrounded.net/254000/254456_DadNMeNewgrounds.swf).
3. Open the Android app and tap **Select SWF File** to import your downloaded `.swf`. The app stores it privately; you only need to import it once.

If the SWF link opens in your browser instead of downloading, use **Save link as**. The original game file is **not included** in this repository or the APK.

## Features

- Controller support: left stick or D-pad movement, **A** for Light Attack, **X** for Heavy Attack
- Remappable attack buttons through **FrostFrontend → Controls**
- On-screen joystick or D-pad, plus **Light** and **Heavy** touch buttons
- **Original 4:3** and **Stretch to Screen** display modes
- **Sound → Speaker Test: Left → Right → Both**
- Custom flaming-mask Android launcher icon

Press **Start** to open FrostFrontend.

## Updates

The current public release is **v0.1.1**. FrostFrontend supports manual GitHub release checks and optional automatic checks on startup. When a newer stable version is available, it provides a link to the published GitHub release; it **never downloads or installs an APK automatically**. The earlier v0.1.0 build did not include update checking.

Future signed updates use the same package name and signing key, allowing installation over an existing public release without uninstalling it. Debug builds use a different signing certificate; don't uninstall an app with unsaved data without backing it up first.

## Development

The Android project uses Ruffle to run the original Flash game and includes FrostFrontend as a Git submodule. The SWF and compiled dependencies are not committed to the repository.

## Credits and licence

**Original game:** [Dad 'n Me on Newgrounds](https://www.newgrounds.com/portal/view/254456) (2005). Programming by **[Tom Fulp](https://tomfulp.newgrounds.com/)**, artwork by **[Dan Paladin](https://danpaladin.newgrounds.com/)**, and music by **[Dustball](https://dustball.newgrounds.com/)**.

The game and its assets belong to their respective rights holders. This is an unofficial, unaffiliated Android compatibility project.

Original Android wrapper source is licensed under [MIT](LICENSE). Third-party components, including Ruffle and FrostFrontend, retain their own licensing terms.

# Build Dad 'n Me's SWF-free Android release APK.
# This script never installs/uninstalls apps and never publishes the APK.
# Signing passwords are prompted for by Android tools; keep the key private.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$version = '0.1.1'
$apkName = "Dad-n-Me-v$version-Android.apk"
$releaseDir = Join-Path $PSScriptRoot 'release'
$unsigned = Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release-unsigned.apk'
$aligned = Join-Path $releaseDir 'dad-n-me-aligned-temp.apk'
$signed = Join-Path $releaseDir $apkName
$keystore = Join-Path $HOME '.android\DadNMe-Release.keystore'
$keyAlias = 'dadnme-release'
$assets = Join-Path $PSScriptRoot 'app\src\main\assets'
$ruffle = Join-Path $assets 'www\ruffle\ruffle.js'
$manifest = Join-Path $PSScriptRoot 'app\src\main\AndroidManifest.xml'

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
if (!(Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $env:JAVA_HOME = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
}
if (!(Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { throw 'JDK 17 not found.' }
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH

$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if (!(Test-Path $sdk)) { throw "Android SDK not found: $sdk" }
$env:ANDROID_HOME = $sdk
Set-Content -Path 'local.properties' -Value "sdk.dir=$($sdk.Replace('\','/'))"

$gradle = @(
    (Join-Path $PSScriptRoot 'gradlew.bat'),
    (Join-Path $PSScriptRoot '.bootstrap\gradle-8.11.1\bin\gradle.bat'),
    (Join-Path $HOME 'Downloads\Arcane-Armor-Collector-Android\.bootstrap\gradle-8.11.1\bin\gradle.bat')
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (!$gradle) {
    $installedGradle = Get-Command gradle.bat -ErrorAction SilentlyContinue
    if ($installedGradle) { $gradle = $installedGradle.Source }
}
if (!$gradle) { throw 'Gradle missing. Run build-test.ps1 once to prepare the project.' }
if (!(Test-Path $ruffle)) { throw 'Ruffle assets missing. Run build-test.ps1 once to prepare the project.' }

$manifestText = [System.IO.File]::ReadAllText($manifest)
if ($manifestText -notmatch 'android:icon\s*=\s*"@(?:mipmap|drawable)/[a-zA-Z0-9_]+"') {
    throw 'Launcher icon attribute missing. The approved icon must be included in the release.'
}
$iconRef = [regex]::Match($manifestText, 'android:icon\s*=\s*"@(?:mipmap|drawable)/([a-zA-Z0-9_]+)"').Groups[1].Value
if (-not @(Get-ChildItem (Join-Path $PSScriptRoot 'app\src\main\res') -File -Recurse | Where-Object { $_.BaseName -eq $iconRef }).Count) {
    throw "Launcher icon resource $iconRef not found. Refusing release."
}

# Never include an original game SWF in the public APK.
$gameAssets = @(Get-ChildItem $assets -File -Recurse -Filter '*.swf')
if ($gameAssets.Count -gt 0) {
    throw 'Game SWF found in app assets. Move it outside the project before building a public release.'
}

$buildToolsDir = Join-Path $sdk 'build-tools'
$tools = Get-ChildItem $buildToolsDir -Directory |
    Where-Object {
        (Test-Path (Join-Path $_.FullName 'zipalign.exe')) -and
        (Test-Path (Join-Path $_.FullName 'apksigner.bat'))
    } | Sort-Object Name -Descending | Select-Object -First 1
if (!$tools) { throw 'Android zipalign/apksigner not found in SDK build-tools.' }
$zipalign = Join-Path $tools.FullName 'zipalign.exe'
$apksigner = Join-Path $tools.FullName 'apksigner.bat'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (!(Test-Path $keytool)) { throw 'JDK keytool missing.' }

function Assert-GameFree([string]$apkPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
    try {
        $entries = @($archive.Entries | ForEach-Object { $_.FullName })
        if (@($entries | Where-Object { $_ -match '(?i)\.swf$' }).Count -gt 0) {
            throw "Public APK contains a SWF: $apkPath"
        }
        if (!($entries -contains 'assets/www/ruffle/ruffle.js')) {
            throw 'Ruffle JavaScript runtime missing from release APK.'
        }
        if (!($entries -contains 'assets/www/index.html')) {
            throw 'Game wrapper HTML missing from release APK.'
        }
    } finally { $archive.Dispose() }
}

Write-Host 'Building Dad n Me v0.1.1 release (original game excluded)...'
& $gradle ':app:assembleRelease'
if ($LASTEXITCODE -ne 0) { throw 'Release Gradle build failed.' }
if (!(Test-Path $unsigned)) { throw 'Unsigned release APK missing.' }
Assert-GameFree $unsigned

New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
if (!(Test-Path $keystore)) {
    New-Item -ItemType Directory -Path (Split-Path $keystore) -Force | Out-Null
    Write-Host ''
    Write-Host 'Creating Dad n Me permanent release signing key. Choose and remember a password.'
    & $keytool -genkeypair -keystore $keystore -alias $keyAlias -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -dname 'CN=Frostie77 Dad n Me Android Release'
    if ($LASTEXITCODE -ne 0) { throw 'Release key generation failed.' }
}
if (Test-Path $aligned) { Remove-Item $aligned -Force }

& $zipalign -p -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'APK zipalign failed.' }
Write-Host 'Signing release; enter the private keystore password when prompted.'
& $apksigner sign --ks $keystore --ks-key-alias $keyAlias --out $signed $aligned
if ($LASTEXITCODE -ne 0) { throw 'Release APK signing failed.' }

& $apksigner verify --verbose $signed
if ($LASTEXITCODE -ne 0) { throw 'Signed APK verification failed.' }
Assert-GameFree $signed
if (Test-Path $aligned) { Remove-Item $aligned -Force }
$hash = (Get-FileHash -Algorithm SHA256 $signed).Hash.ToLowerInvariant()
Set-Content -Path (Join-Path $releaseDir "Dad-n-Me-v$version-Android.sha256.txt") -Value "$hash  $apkName"

Write-Host ''
Write-Host 'PASS: signed, verified, SWF-free release APK.'
Write-Host "APK: $signed"
Write-Host "SHA256: $hash"
Write-Host "PRIVATE signing key (keep backed up securely): $keystore"
Write-Host 'Nothing was installed, uninstalled, or uploaded.'

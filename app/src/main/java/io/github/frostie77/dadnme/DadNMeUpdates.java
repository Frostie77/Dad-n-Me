package io.github.frostie77.dadnme;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Checks only public stable GitHub releases; never downloads or installs APKs.
 * Call check() on a background thread, not the Android UI thread.
 */
final class DadNMeUpdates {
    private static final String REPOSITORY = "Frostie77/Dad-n-Me";
    private static final String PACKAGE_NAME = "io.github.frostie77.dadnme";
    private static final String RELEASE_API =
        "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    private static final String RELEASE_BASE =
        "https://github.com/" + REPOSITORY + "/releases/tag/";
    private static final int MAX_RESPONSE_BYTES = 192 * 1024;

    static final class Result {
        final String status;
        final String latest;
        final String releaseUrl;
        final boolean available;
        Result(String status, String latest, String releaseUrl, boolean available) {
            this.status = status;
            this.latest = latest;
            this.releaseUrl = releaseUrl;
            this.available = available;
        }
    }

    private DadNMeUpdates() {}

    static String installedVersion(Context context) {
        try {
            String version = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
            return version == null ? "Unknown" : version;
        } catch (Exception ignored) { return "Unknown"; }
    }

    private static boolean publicBuild(Context context) {
        return PACKAGE_NAME.equals(context.getPackageName()) &&
            (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0;
    }

    static Result check(Context context) {
        if (!publicBuild(context)) {
            return new Result("GitHub update checking is available in the signed public APK.",
                "Public releases only", null, false);
        }
        try {
            JSONObject release = new JSONObject(readHttps(RELEASE_API));
            String tag = release.getString("tag_name");
            require(tag.matches("v(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)"),
                "Unexpected release tag");
            String version = tag.substring(1);
            require(!release.getBoolean("draft") && !release.getBoolean("prerelease"),
                "Not a stable public release");
            require(!release.isNull("published_at") &&
                !release.getString("published_at").isEmpty(), "Release not published");
            String releaseUrl = RELEASE_BASE + tag;
            require(releaseUrl.equals(release.getString("html_url")),
                "Unexpected release URL");

            String apkName = "Dad-n-Me-" + tag + "-Android.apk";
            JSONArray assets = release.getJSONArray("assets");
            require(assets.length() <= 100, "Too many assets");
            int valid = 0;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (!apkName.equals(asset.optString("name"))) continue;
                require("uploaded".equals(asset.getString("state")),
                    "APK upload incomplete");
                require(asset.getLong("size") > 0, "Empty APK");
                require(asset.getString("digest").matches("sha256:[a-fA-F0-9]{64}"),
                    "Missing APK SHA-256 digest");
                require(("https://github.com/" + REPOSITORY +
                    "/releases/download/" + tag + "/" + apkName)
                    .equals(asset.getString("browser_download_url")), "Unrecognised APK URL");
                valid++;
            }
            require(valid == 1, "Exactly one published Android APK expected");

            String installed = installedVersion(context);
            require(installed.matches("(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)"),
                "Installed version unavailable");
            boolean available = compare(version, installed) > 0;
            return new Result(available ? "A newer Android release is available." :
                    "You have the latest published Android release.",
                tag, available ? releaseUrl : null, available);
        } catch (Exception failure) {
            return new Result("Unable to verify updates. Check your connection and try again.",
                "Unavailable", null, false);
        }
    }

    private static int compare(String a, String b) {
        String[] aa = a.split("\\."), bb = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            long x = Long.parseLong(aa[i]), y = Long.parseLong(bb[i]);
            if (x != y) return x > y ? 1 : -1;
        }
        return 0;
    }

    private static void require(boolean valid, String explanation) throws Exception {
        if (!valid) throw new Exception(explanation);
    }

    private static String readHttps(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "DadNMe-Android-UpdateCheck");
        try {
            require(connection.getResponseCode() == 200, "GitHub unavailable");
            require(connection.getContentLengthLong() <= MAX_RESPONSE_BYTES,
                "Response too large");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int size;
                while ((size = input.read(buffer)) >= 0) {
                    require(output.size() + size <= MAX_RESPONSE_BYTES, "Response too large");
                    output.write(buffer, 0, size);
                }
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally { connection.disconnect(); }
    }
}

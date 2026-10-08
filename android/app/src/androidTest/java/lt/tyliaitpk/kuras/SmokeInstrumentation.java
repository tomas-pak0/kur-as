package lt.tyliaitpk.kuras;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Runs only on the CI emulator. It creates and revokes its own test sharing session. */
public class SmokeInstrumentation extends Instrumentation {
    private String editor;
    private final String api = "https://kur-as.t0m45-p4k0.chatgpt.site/api/share";
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private JSONObject post(JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(api).openConnection();
        try {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json");
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            if (connection.getResponseCode() != 200) throw new AssertionError("Test API returned " + connection.getResponseCode());
            try (java.io.InputStream input = connection.getInputStream()) { return new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8)); }
        } finally { connection.disconnect(); }
    }
    private String shell(String command) throws Exception {
        try (android.os.ParcelFileDescriptor.AutoCloseInputStream input =
                 new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private Location fix(double latitude, double longitude) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude); location.setLongitude(longitude); location.setAccuracy(5);
        location.setTime(System.currentTimeMillis()); location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return location;
    }
    @Override public void onStart() {
        Context context = getTargetContext(); Bundle result = new Bundle();
        LocationManager locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            shell("settings put global stay_on_while_plugged_in 0");
            shell("settings put system screen_off_timeout 15000");
            result.putString("screenOffTimeoutMs", shell("settings get system screen_off_timeout").trim());
            shell("input keyevent KEYCODE_WAKEUP");
            shell("wm dismiss-keyguard");
            Activity activity = startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Thread.sleep(25000);
            require(power.isInteractive(), "Screen slept while the application was visible");
            result.putString("foregroundScreenAfter25s", "awake");
            JSONObject session = post(new JSONObject().put("action", "create"));
            editor = session.getString("editor"); String viewer = session.getString("viewer");
            locations.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            locations.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            runOnMainSync(() -> activity.startForegroundService(new Intent(context, ShareService.class)
                .setAction(ShareService.START).putExtra(ShareService.TOKEN, editor)
                .putExtra(ShareService.INITIAL_LOCATION, fix(55.25, 24.75))));
            long deadline = System.currentTimeMillis() + 30000;
            while (context.getSharedPreferences("live_share", Context.MODE_PRIVATE).getLong("sent_at", 0) == 0 && System.currentTimeMillis() < deadline) Thread.sleep(500);
            long firstSent = context.getSharedPreferences("live_share", Context.MODE_PRIVATE).getLong("sent_at", 0);
            require(firstSent > 0, "First location was not received by the server");
            shell("input keyevent KEYCODE_HOME");
            Thread.sleep(22000);
            require(!power.isInteractive(), "The phone could not sleep in the background");
            Location sleepingFix = fix(55.26, 24.76);
            locations.setTestProviderLocation(LocationManager.GPS_PROVIDER, sleepingFix);
            deadline = System.currentTimeMillis() + 30000;
            boolean receivedWhileAsleep = false;
            while (System.currentTimeMillis() < deadline) {
                JSONObject received = post(new JSONObject().put("action", "view").put("token", viewer));
                if (Math.abs(received.getDouble("latitude") - 55.26) < .0001
                    && Math.abs(received.getDouble("longitude") - 24.76) < .0001) {
                    receivedWhileAsleep = true;
                    break;
                }
                Thread.sleep(1000);
            }
            require(receivedWhileAsleep, "The viewer did not receive a GPS fix generated after the screen slept");
            require(!power.isInteractive(), "Background location sharing woke the screen");
            require(context.getSharedPreferences("live_share", Context.MODE_PRIVATE).getLong("sent_at", 0) > firstSent, "Background upload stopped");
            result.putString("backgroundScreenAfter22s", "asleep");
            result.putString("newFixGeneratedAfterScreenSlept", "passed");
            result.putString("backgroundNewLocationReceived", "passed");
            shell("input keyevent KEYCODE_WAKEUP");
            shell("wm dismiss-keyguard");
            // singleTop resumes the existing activity; startActivitySync waits for a new instance.
            shell("am start -n lt.tyliaitpk.kuras/.MainActivity");
            Thread.sleep(20000);
            require(power.isInteractive(), "Screen slept after resuming the application");
            result.putString("resumedScreenAfter20s", "awake");
            result.putString("status", "passed");
            post(new JSONObject().put("action", "stop").put("token", editor));
            context.startService(new Intent(context, ShareService.class).setAction(ShareService.DISMISS).putExtra(ShareService.TOKEN, editor));
            editor = null;
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("status", "failed"); result.putString("error", error.getClass().getSimpleName() + ": " + error.getMessage());
            try { result.putString("powerDiagnostics", shell("dumpsys power | grep -E 'mWakefulness=|mStayOn=|mWakeLockSummary=|mUserActivitySummary=|mScreenOffTimeoutSetting='").trim().replace('\n', '|')); }
            catch (Exception ignored) { }
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (editor != null) {
                try { post(new JSONObject().put("action", "stop").put("token", editor)); } catch (Exception ignored) { }
                context.stopService(new Intent(context, ShareService.class));
            }
            try { locations.removeTestProvider(LocationManager.GPS_PROVIDER); } catch (Exception ignored) { }
        }
    }
}

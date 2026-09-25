package lt.tyliaitpk.kuras;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.CellSignalStrength;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShareService extends Service {
    static final String START = "lt.tyliaitpk.kuras.START_SHARE";
    static final String STOP = "lt.tyliaitpk.kuras.STOP_SHARE";
    static final String DISMISS = "lt.tyliaitpk.kuras.DISMISS_SHARE";
    static final String TOKEN = "editor_token";
    private static final String CHANNEL = "live_location";
    private static final int NOTIFICATION = 42;
    private static final String API = "https://kur-as.t0m45-p4k0.chatgpt.site/api/share";
    private static volatile String runningToken;
    private static volatile boolean cancelling;
    private ScheduledExecutorService worker;
    private LocationManager locations;
    private volatile Location lastLocation;
    private volatile String gpsText = "Ieškoma palydovų…";
    private volatile String editor;
    private volatile boolean tracking;

    static String status(Context context, String token) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("live_share", MODE_PRIVATE);
        if (token.equals(preferences.getString("stopped", "")))
            return "stopped";
        if (token.equals(preferences.getString("editor", "")) && preferences.getBoolean("pending_stop", false))
            return "stopping";
        if (token.equals(runningToken)) return cancelling ? "stopping" : "running";
        return "idle";
    }

    private final LocationListener listener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            Location previous = lastLocation;
            if (previous == null || location.getTime() >= previous.getTime()
                && (location.getTime() - previous.getTime() > 15000
                    || !previous.hasAccuracy() || !location.hasAccuracy()
                    || location.getAccuracy() <= previous.getAccuracy() * 1.5f)) {
                lastLocation = location;
            }
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) { }
    };

    private final GnssStatus.Callback gnss = new GnssStatus.Callback() {
        @Override public void onStarted() { gpsText = "Ieškoma palydovų…"; }
        @Override public void onStopped() { gpsText = "GPS imtuvas sustabdytas"; }
        @Override public void onSatelliteStatusChanged(GnssStatus status) {
            int visible = status.getSatelliteCount(), used = 0;
            float strength = 0;
            for (int i = 0; i < visible; i++) {
                if (status.usedInFix(i)) { used++; strength += status.getCn0DbHz(i); }
            }
            gpsText = "Matomi " + visible + " · naudojami " + used + (used > 0
                ? String.format(Locale.getDefault(), " · vid. %.1f dB-Hz", strength / used) : "");
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        locations = (LocationManager) getSystemService(LOCATION_SERVICE);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(
            CHANNEL, "Vietos bendrinimas", NotificationManager.IMPORTANCE_LOW));
        worker = Executors.newSingleThreadScheduledExecutor();
        worker.scheduleWithFixedDelay(this::sendPending, 0, 10, TimeUnit.SECONDS);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        String token = intent == null ? null : intent.getStringExtra(TOKEN);
        if (START.equals(action) && valid(token)) {
            if (token.equals(getSharedPreferences("live_share", MODE_PRIVATE).getString("editor", ""))
                && getSharedPreferences("live_share", MODE_PRIVATE).getBoolean("pending_stop", false)) {
                action = STOP;
            }
        }
        if (START.equals(action) && valid(token)) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                stopSelf(); return START_NOT_STICKY;
            }
            if (token.equals(editor) && tracking) return START_NOT_STICKY;
            stopTracking();
            editor = token; runningToken = token; cancelling = false; lastLocation = null;
            getSharedPreferences("live_share", MODE_PRIVATE).edit().putString("editor", token)
                .remove("stopped").remove("pending_stop").apply();
            try {
                if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification(false),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                else startForeground(NOTIFICATION, notification(false));
            } catch (SecurityException ex) {
                runningToken = null; editor = null; stopSelf(); return START_NOT_STICKY;
            }
            startTracking();
        } else if (STOP.equals(action)) {
            boolean alreadyRunning = editor != null;
            if (editor == null) editor = getSharedPreferences("live_share", MODE_PRIVATE).getString("editor", null);
            if (!valid(editor)) { stopSelf(); return START_NOT_STICKY; }
            runningToken = editor;
            cancelling = true;
            stopTracking();
            getSharedPreferences("live_share", MODE_PRIVATE).edit().putBoolean("pending_stop", true).apply();
            if (!alreadyRunning) {
                try {
                    if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification(true),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                    else startForeground(NOTIFICATION, notification(true));
                } catch (SecurityException ex) { stopSelf(); return START_NOT_STICKY; }
            }
            updateNotification(true);
        } else if (DISMISS.equals(action) && editor != null && editor.equals(token)) {
            stopTracking();
            getSharedPreferences("live_share", MODE_PRIVATE).edit().remove("editor")
                .remove("pending_stop").putString("stopped", token).apply();
            runningToken = null; editor = null; stopSelf();
        } else if (DISMISS.equals(action)) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private static boolean valid(String token) { return token != null && token.matches("[a-f0-9]{64}"); }

    private void startTracking() {
        tracking = true;
        try { locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener, Looper.getMainLooper()); }
        catch (SecurityException | IllegalArgumentException ignored) { }
        try { locations.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, listener, Looper.getMainLooper()); }
        catch (SecurityException | IllegalArgumentException ignored) { }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try { locations.registerGnssStatusCallback(gnss, new android.os.Handler(Looper.getMainLooper())); }
            catch (SecurityException | IllegalArgumentException ignored) { }
        } else gpsText = "Palydovų rodmenims reikia tikslios vietos leidimo";
    }

    private void stopTracking() {
        if (!tracking) return;
        tracking = false;
        try { locations.removeUpdates(listener); } catch (SecurityException ignored) { }
        try { locations.unregisterGnssStatusCallback(gnss); }
        catch (IllegalArgumentException | SecurityException ignored) { }
    }

    private Notification notification(boolean stopping) {
        Intent open = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN);
        PendingIntent content = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, ShareService.class).setAction(STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Kur aš? · gyva vieta")
            .setContentText(stopping ? "Nutraukiamas bendrinimas · laukiama ryšio" :
                "Vieta bendrinama ir užrakintame telefone")
            .setContentIntent(content).setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE);
        if (!stopping) builder.addAction(android.R.drawable.ic_media_pause, "Sustabdyti", stopAction);
        return builder.build();
    }

    private void updateNotification(boolean stopping) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
            NOTIFICATION, notification(stopping));
    }

    private void sendPending() {
        String token = editor;
        if (token == null) return;
        if (cancelling) {
            try {
                int code = post(new JSONObject().put("action", "stop").put("token", token));
                if (code >= 200 && code < 300 || code == 410) {
                    getSharedPreferences("live_share", MODE_PRIVATE).edit()
                        .remove("editor").remove("pending_stop").putString("stopped", token).apply();
                    runningToken = null; editor = null;
                    stopSelf();
                }
            } catch (Exception ignored) { /* Keep the notification and retry when connected. */ }
            return;
        }
        Location location = lastLocation;
        if (!tracking || location == null) return;
        try {
            JSONObject data = new JSONObject().put("action", "update").put("token", token)
                .put("latitude", location.getLatitude()).put("longitude", location.getLongitude())
                .put("accuracy", Math.max(0, location.hasAccuracy() ? location.getAccuracy() : 1000))
                .put("speed", location.hasSpeed() ? Math.max(0, location.getSpeed()) : JSONObject.NULL)
                .put("heading", location.hasBearing() ? location.getBearing() : JSONObject.NULL)
                .put("measuredAt", Math.min(System.currentTimeMillis(), Math.max(0, location.getTime())))
                .put("battery", battery()).put("gps", gpsText).put("network", network());
            if (post(data) == 410 && token.equals(editor)) {
                cancelling = true; stopTracking(); updateNotification(true);
            }
        } catch (Exception ignored) { /* The next interval retries with the latest location. */ }
    }

    private String battery() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return percent >= 0 && percent <= 100 ? percent + "%" : "Neprieinama";
    }

    private String network() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network active = manager.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : manager.getNetworkCapabilities(active);
        String type = caps == null ? "Nėra ryšio" : caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi"
            : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobilieji duomenys" : "Kitas tinklas";
        TelephonyManager telephony = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String signal = "Neprieinamas";
        if (Build.VERSION.SDK_INT >= 28
            && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                SignalStrength strength = telephony.getSignalStrength();
                if (strength != null) {
                    List<CellSignalStrength> cells = strength.getCellSignalStrengths();
                    int dbm = cells.isEmpty() ? Integer.MAX_VALUE : cells.get(0).getDbm();
                    signal = strength.getLevel() + "/4"
                        + (dbm == Integer.MAX_VALUE || dbm == CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN
                            ? "" : " · " + dbm + " dBm");
                }
            } catch (SecurityException | UnsupportedOperationException ignored) { }
        }
        return type + " · Mobilusis " + signal;
    }

    private int post(JSONObject data) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Cache-Control", "no-store");
            byte[] body = data.toString().getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream out = connection.getOutputStream()) { out.write(body); }
            return connection.getResponseCode();
        } finally { connection.disconnect(); }
    }

    @Override public void onDestroy() {
        stopTracking();
        if (worker != null) worker.shutdownNow();
        if (editor != null && editor.equals(runningToken)) runningToken = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}

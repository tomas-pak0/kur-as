package lt.tyliaitpk.kuras;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellSignalStrength;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String SITE = "https://kur-as.t0m45-p4k0.chatgpt.site";
    private static final int PERMISSIONS_REQUEST = 12;
    private static final int NOTIFICATION_REQUEST = 13;
    private static final int FILE_REQUEST = 14;
    private WebView webView;
    private ValueCallback<Uri[]> pendingFiles;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;
    private GeolocationPermissions.Callback pendingLocation;
    private String pendingOrigin;
    private boolean pageReady;
    private boolean gnssTracking;
    private TelephonyCallback signalCallback;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String gpsText = "Laukiama vietos leidimo";
    private String signalText = "Neprieinamas";
    private String networkText = "Tikrinama…";

    private final GnssStatus.Callback gnssCallback = new GnssStatus.Callback() {
        @Override public void onStarted() { gpsText = "Ieškoma palydovų…"; showTelemetry(); }
        @Override public void onStopped() { gpsText = "GPS imtuvas sustabdytas"; showTelemetry(); }
        @Override public void onSatelliteStatusChanged(GnssStatus status) {
            int visible = status.getSatelliteCount(), used = 0;
            float strength = 0;
            for (int i = 0; i < visible; i++) {
                if (status.usedInFix(i)) { used++; strength += status.getCn0DbHz(i); }
            }
            gpsText = "Matomi " + visible + " · naudojami " + used + (used > 0
                ? String.format(Locale.getDefault(), " · vid. %.1f dB-Hz", strength / used) : "");
            showTelemetry();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff111318);
        getWindow().setNavigationBarColor(0xff111318);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        webView = new WebView(this);
        webView.setBackgroundColor(0xff111318);
        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(0xff111318);
        content.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15 draws the app behind system bars. Keep the page inside their safe area.
            content.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return WindowInsets.CONSUMED;
            });
        }
        setContentView(content);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (trusted(request.getUrl())) return false;
                openExternal(request.getUrl());
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = trusted(Uri.parse(url));
                if (pageReady && "/app.html".equals(Uri.parse(url).getPath())) readyBridge();
                showTelemetry();
            }
            @Override public void onPageCommitVisible(WebView view, String url) {
                pageReady = trusted(Uri.parse(url));
                if (pageReady && "/app.html".equals(Uri.parse(url).getPath())) readyBridge();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                      FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try {
                    // Document providers may report XML as text/plain or octet-stream.
                    // Let the app validate the chosen file instead of filtering it here.
                    Intent choose = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    choose.addCategory(Intent.CATEGORY_OPENABLE);
                    choose.setType("*/*");
                    choose.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(choose, FILE_REQUEST);
                } catch (ActivityNotFoundException ex) {
                    pendingFiles.onReceiveValue(null);
                    pendingFiles = null;
                }
                return true;
            }
            @Override public boolean onJsPrompt(WebView view, String url, String message,
                                                String defaultValue, JsPromptResult result) {
                Uri source = Uri.parse(url);
                if (!trusted(source) || !"/app.html".equals(source.getPath()))
                    return super.onJsPrompt(view, url, message, defaultValue, result);
                if ("kuras:emergency:info".equals(message)) {
                    result.confirm(emergencyInfo(defaultValue));
                    return true;
                }
                if ("kuras:dial".equals(message)) {
                    if (defaultValue == null || !defaultValue.matches("[0-9]{0,6}")) { result.confirm("error"); return true; }
                    try {
                        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + defaultValue)));
                        result.confirm("ok");
                    } catch (ActivityNotFoundException ex) { result.confirm("error"); }
                    return true;
                }
                if ("kuras:share:text".equals(message)) {
                    if (defaultValue == null || defaultValue.length() > 6000) { result.confirm("error"); return true; }
                    try {
                        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, defaultValue);
                        startActivity(Intent.createChooser(share, null));
                        result.confirm("ok");
                    } catch (ActivityNotFoundException ex) { result.confirm("error"); }
                    return true;
                }
                if ("kuras:language".equals(message) && defaultValue != null
                    && defaultValue.matches("lt|lv|pl|en|de|es|fr|it|uk|ru")) {
                    getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", defaultValue).apply();
                    result.confirm("ok"); return true;
                }
                if ("kuras:background:status".equals(message) && validEditor(defaultValue)) {
                    result.confirm(ShareService.status(MainActivity.this, defaultValue));
                    return true;
                }
                if ("kuras:background:health".equals(message) && validEditor(defaultValue)) {
                    result.confirm(ShareService.health(MainActivity.this, defaultValue));
                    return true;
                }
                if ("kuras:background:settings".equals(message)) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                        result.confirm("ok");
                    } catch (ActivityNotFoundException ex) { result.confirm("error"); }
                    return true;
                }
                if ("kuras:background:start".equals(message)) {
                    String token = defaultValue;
                    Location initial = null;
                    try {
                        if (defaultValue != null && defaultValue.startsWith("{")) {
                            JSONObject request = new JSONObject(defaultValue);
                            token = request.optString("token");
                            initial = initialLocation(request.optJSONObject("position"));
                        }
                    } catch (Exception ignored) { result.confirm("error"); return true; }
                    if (!validEditor(token)) { result.confirm("error"); return true; }
                    if (!hasLocationPermission()) { result.confirm("permission"); return true; }
                    try {
                        Intent service = new Intent(MainActivity.this, ShareService.class)
                            .setAction(ShareService.START).putExtra(ShareService.TOKEN, token);
                        if (initial != null) service.putExtra(ShareService.INITIAL_LOCATION, initial);
                        startForegroundService(service);
                        result.confirm("ok");
                        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED)
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
                    } catch (SecurityException | IllegalStateException ex) { result.confirm("error"); }
                    return true;
                }
                if ("kuras:background:stop".equals(message) && validEditor(defaultValue)) {
                    startService(new Intent(MainActivity.this, ShareService.class)
                        .setAction(ShareService.DISMISS).putExtra(ShareService.TOKEN, defaultValue));
                    result.confirm("ok");
                    return true;
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!trusted(Uri.parse(origin))) { callback.invoke(origin, false, false); return; }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                    startTelemetry();
                } else {
                    pendingLocation = callback;
                    pendingOrigin = origin;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST);
                }
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                if (!userGesture) return false;
                WebView child = new WebView(MainActivity.this);
                child.setWebViewClient(new WebViewClient() {
                    private boolean opened;
                    private void handle(WebView v, Uri uri) {
                        if (opened || "about".equals(uri.getScheme())) return;
                        opened = true;
                        openExternal(uri);
                        v.stopLoading();
                        v.post(v::destroy);
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        handle(v, request.getUrl());
                        return true;
                    }
                    @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                        handle(v, Uri.parse(url));
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                return true;
            }
        });
        webView.loadUrl(startUrl(getIntent()));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_REQUEST) return;
        if (pendingFiles != null) {
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingFiles = null;
        }
    }

    private String startUrl(Intent intent) {
        Uri uri = intent == null ? null : intent.getData();
        if (uri != null && trusted(uri)) {
            String fragment = uri.getFragment();
            if (fragment != null && fragment.matches("[a-f0-9]{64}")) {
                if ("/watch.html".equals(uri.getPath()))
                    return SITE + "/app.html?open=" + System.currentTimeMillis() + "#watch=" + fragment;
            }
            if ("/app.html".equals(uri.getPath()) && fragment != null
                && fragment.matches("watch=[a-f0-9]{64}"))
                return SITE + "/app.html?open=" + System.currentTimeMillis() + "#" + fragment;
        }
        return SITE + "/app.html";
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (webView != null && intent != null && intent.getData() != null) {
            webView.loadUrl(startUrl(intent));
        }
    }

    private boolean trusted(Uri uri) {
        return "https".equals(uri.getScheme()) && "kur-as.t0m45-p4k0.chatgpt.site".equals(uri.getHost())
            && uri.getPort() == -1;
    }
    private boolean validEditor(String token) { return token != null && token.matches("[a-f0-9]{64}"); }
    private Location initialLocation(JSONObject position) throws Exception {
        if (position == null) return null;
        double latitude = position.getDouble("latitude"), longitude = position.getDouble("longitude");
        double accuracy = position.getDouble("accuracy");
        long measuredAt = position.getLong("timestamp");
        if (!Double.isFinite(latitude) || Math.abs(latitude) > 90
            || !Double.isFinite(longitude) || Math.abs(longitude) > 180
            || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 100000
            || measuredAt <= 0 || measuredAt > System.currentTimeMillis() + 60000) return null;
        Location location = new Location("webview");
        location.setLatitude(latitude); location.setLongitude(longitude);
        location.setAccuracy((float) accuracy); location.setTime(measuredAt);
        double speed = position.optDouble("speed", Double.NaN);
        double heading = position.optDouble("heading", Double.NaN);
        if (Double.isFinite(speed) && speed >= 0 && speed <= 1000) location.setSpeed((float) speed);
        if (Double.isFinite(heading) && heading >= 0 && heading <= 360) location.setBearing((float) heading);
        return location;
    }
    private void readyBridge() {
        webView.evaluateJavascript("window.KurAsNative={version:'0.5.7',start:function(t,p){return prompt('kuras:background:start',JSON.stringify({token:t,position:p?{latitude:p.coords.latitude,longitude:p.coords.longitude,accuracy:p.coords.accuracy,speed:p.coords.speed,heading:p.coords.heading,timestamp:p.timestamp}:null}))},"
            + "stop:function(t){return prompt('kuras:background:stop',t)},"
            + "status:function(t){return prompt('kuras:background:status',t)},"
            + "health:function(t){return prompt('kuras:background:health',t)}};"
            + "window.KurAsNative.settings=function(){return prompt('kuras:background:settings','')};"
            + "window.KurAsNative.emergency=function(c){return prompt('kuras:emergency:info',c||'')};"
            + "window.KurAsNative.dial=function(n){return prompt('kuras:dial',n||'')};"
            + "window.KurAsNative.shareText=function(s){return prompt('kuras:share:text',s)};"
            + "if(!window.KurAsLanguageBridge){window.KurAsLanguageBridge=true;window.addEventListener('kur-as-language-change',function(){prompt('kuras:language',window.KurAsI18n.language)})}"
            + "if(window.KurAsI18n)prompt('kuras:language',window.KurAsI18n.language);"
            + "window.dispatchEvent(new Event('kur-as-native-ready'));", null);
    }
    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException ignored) { }
    }
    private String emergencyInfo(String requestedCountry) {
        JSONObject data = new JSONObject();
        JSONArray numbers = new JSONArray();
        String networkCountry = "";
        try {
            if (telephonyManager != null) networkCountry = telephonyManager.getNetworkCountryIso();
        } catch (SecurityException | UnsupportedOperationException ignored) { }
        String country = requestedCountry != null && requestedCountry.matches("[a-zA-Z]{2}")
            ? requestedCountry.toLowerCase(Locale.ROOT) : networkCountry;
        try {
            data.put("country", networkCountry);
            if (Build.VERSION.SDK_INT >= 29 && telephonyManager != null
                && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                for (List<EmergencyNumber> list : telephonyManager.getEmergencyNumberList().values()) {
                    for (EmergencyNumber number : list) {
                        String iso = number.getCountryIso();
                        if (!iso.isEmpty() && !iso.equalsIgnoreCase(country)) continue;
                        int categories = 0, sources = 0;
                        for (int category : number.getEmergencyServiceCategories()) categories |= category;
                        for (int origin : number.getEmergencyNumberSources()) sources |= origin;
                        numbers.put(new JSONObject().put("number", number.getNumber())
                            .put("country", iso.toLowerCase(Locale.ROOT))
                            .put("categories", categories).put("source", sources));
                    }
                }
            }
        } catch (Exception ignored) { }
        try { data.put("numbers", numbers); } catch (Exception ignored) { }
        return data.toString();
    }
    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != PERMISSIONS_REQUEST) return;
        if (pendingLocation != null) {
            pendingLocation.invoke(pendingOrigin, hasLocationPermission(), false);
            pendingLocation = null;
            pendingOrigin = null;
        }
        startTelemetry();
        if (pageReady) webView.evaluateJavascript(
            "window.dispatchEvent(new Event('kur-as-native-ready'));", null);
    }

    private void startTelemetry() {
        if (hasLocationPermission() && !gnssTracking) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try { gnssTracking = locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper())); }
                catch (SecurityException | IllegalArgumentException ex) { gpsText = "Palydovų duomenys neprieinami"; }
            } else gpsText = "Palydovų rodmenims reikia tikslios vietos leidimo";
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 31 && signalCallback == null) {
                signalCallback = new SignalListener();
                try { telephonyManager.registerTelephonyCallback(getMainExecutor(), signalCallback); }
                catch (SecurityException | UnsupportedOperationException ex) { signalCallback = null; }
            }
            if (Build.VERSION.SDK_INT >= 28) {
                try { SignalStrength signal = telephonyManager.getSignalStrength(); if (signal != null) showSignal(signal); }
                catch (SecurityException | UnsupportedOperationException ignored) { }
            }
        } else signalText = "Reikia telefono būsenos leidimo";
        showTelemetry();
    }
    private final class SignalListener extends TelephonyCallback implements TelephonyCallback.SignalStrengthsListener {
        @Override public void onSignalStrengthsChanged(SignalStrength signal) { showSignal(signal); }
    }
    private void showSignal(SignalStrength signal) {
        List<CellSignalStrength> cells = signal.getCellSignalStrengths();
        int dbm = cells.isEmpty() ? CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN : cells.get(0).getDbm();
        signalText = signal.getLevel() + "/4" + (dbm == CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN
            || dbm == Integer.MAX_VALUE ? "" : " · " + dbm + " dBm");
        showTelemetry();
    }
    private void updateNetwork() {
        try {
            Network active = connectivityManager.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : connectivityManager.getNetworkCapabilities(active);
            networkText = caps == null ? "Nėra ryšio" : caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi"
                : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobilieji duomenys" : "Kitas tinklas";
            showTelemetry();
        } catch (SecurityException ignored) { networkText = "Neprieinamas"; showTelemetry(); }
    }
    private void showTelemetry() {
        if (webView == null || !pageReady) return;
        BatteryManager battery = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int charge = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String batteryText = charge >= 0 && charge <= 100 ? charge + "%" : "Neprieinama";
        String script = "(function(){if(window.KurAsApplyNativeTelemetry)window.KurAsApplyNativeTelemetry({"
            + "gps:" + JSONObject.quote(gpsText) + ",network:"
            + JSONObject.quote(networkText + " · Mobilusis " + signalText)
            + ",battery:" + JSONObject.quote(batteryText) + "});})();";
        runOnUiThread(() -> { if (pageReady) webView.evaluateJavascript(script, null); });
    }
    @Override protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (webView != null) webView.onResume();
        android.content.SharedPreferences sharing = getSharedPreferences("live_share", MODE_PRIVATE);
        if (sharing.getBoolean("pending_stop", false) && hasLocationPermission()) {
            try { startForegroundService(new Intent(this, ShareService.class).setAction(ShareService.STOP)); }
            catch (SecurityException | IllegalStateException ignored) { }
        }
        if (pageReady) webView.evaluateJavascript(
            "window.dispatchEvent(new Event('kur-as-native-ready'));", null);
        if (locationManager != null && hasLocationPermission()) startTelemetry();
        if (connectivityManager != null && networkCallback == null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) { runOnUiThread(() -> updateNetwork()); }
                @Override public void onLost(Network network) { runOnUiThread(() -> updateNetwork()); }
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                    runOnUiThread(() -> updateNetwork());
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
        updateNetwork();
    }
    @Override protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (gnssTracking) { locationManager.unregisterGnssStatusCallback(gnssCallback); gnssTracking = false; }
        if (signalCallback != null && Build.VERSION.SDK_INT >= 31) {
            telephonyManager.unregisterTelephonyCallback(signalCallback);
            signalCallback = null;
        }
        if (networkCallback != null) { connectivityManager.unregisterNetworkCallback(networkCallback); networkCallback = null; }
        if (webView != null) webView.onPause();
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else if (webView.getUrl() != null && webView.getUrl().startsWith(SITE + "/watch.html"))
            webView.loadUrl(SITE + "/app.html");
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (pendingFiles != null) { pendingFiles.onReceiveValue(null); pendingFiles = null; }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}

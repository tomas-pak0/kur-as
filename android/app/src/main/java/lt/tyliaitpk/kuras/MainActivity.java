package lt.tyliaitpk.kuras;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.Address;
import android.location.Geocoder;
import android.location.GnssStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellSignalStrength;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PERMISSIONS_REQUEST = 12;
    private MapView map;
    private Marker marker;
    private Location lastLocation;
    private TextView locationText, accuracyText, networkText, signalText, statusText, gpsText, gpsSignalText, addressText, deviceText, batteryText;
    private long lastAddressTime = 0L;
    private Location lastAddressLocation;
    private Button shareButton;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;
    private TelephonyCallback signalCallback;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean tracking = false;
    private boolean gnssTracking = false;
    private final GnssStatus.Callback gnssCallback = new GnssStatus.Callback() {
        @Override public void onStarted() { gpsText.setText("Palydovų ryšys: ieškoma palydovų…"); }
        @Override public void onStopped() {
            gpsText.setText("Palydovų ryšys: imtuvas sustabdytas");
            gpsSignalText.setText("Palydovų signalas: –");
        }
        @Override public void onSatelliteStatusChanged(GnssStatus status) {
            int visible = status.getSatelliteCount(), used = 0;
            float sum = 0f;
            for (int i = 0; i < visible; i++) {
                if (status.usedInFix(i)) {
                    used++;
                    sum += status.getCn0DbHz(i);
                }
            }
            gpsText.setText("Palydovų ryšys: matomi " + visible + " · vietai naudojami " + used);
            gpsSignalText.setText(used == 0 ? "Palydovų signalas: laukiama vietos fiksavimo"
                : String.format(Locale.getDefault(), "Palydovų signalas: vid. %.1f dB-Hz", sum / used));
        }
    };
    private final LocationListener locationListener = this::showLocation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        locationText = findViewById(R.id.location);
        accuracyText = findViewById(R.id.accuracy);
        addressText = findViewById(R.id.address);
        deviceText = findViewById(R.id.device);
        batteryText = findViewById(R.id.battery);
        deviceText.setText("Įrenginys: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
            + " · Android " + android.os.Build.VERSION.RELEASE);
        BatteryManager battery = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int charge = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        batteryText.setText(charge >= 0 && charge <= 100 ? "Baterija: " + charge + "%" : "Baterija: neprieinama");
        gpsText = findViewById(R.id.gps);
        gpsSignalText = findViewById(R.id.gps_signal);
        networkText = findViewById(R.id.network);
        signalText = findViewById(R.id.signal);
        statusText = findViewById(R.id.status);
        shareButton = findViewById(R.id.share);
        findViewById(R.id.center).setOnClickListener(v -> {
            if (lastLocation != null) map.getController().animateTo(
                new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
            else requestPermissionsIfNeeded();
        });
        shareButton.setOnClickListener(v -> shareLocation());
        findViewById(R.id.live_share).setOnClickListener(v -> startActivity(
            new Intent(Intent.ACTION_VIEW, Uri.parse("https://kur-as.t0m45-p4k0.chatgpt.site/"))));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        updateNetwork();
        requestPermissionsIfNeeded();
    }

    private void requestPermissionsIfNeeded() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST);
        } else if (!tracking) startTracking();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == PERMISSIONS_REQUEST) startTracking();
    }

    private void startTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
                locationManager.requestLocationUpdates(provider, 2500L, 2f, locationListener);
                if (!provider.equals(LocationManager.NETWORK_PROVIDER) &&
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener);
                Location cached = locationManager.getLastKnownLocation(provider);
                if (cached != null) showLocation(cached);
                tracking = true;
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        gnssTracking = locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper()));
                    } catch (SecurityException | IllegalArgumentException ex) {
                        gpsText.setText("Palydovų ryšys: duomenys neprieinami");
                    }
                } else gpsText.setText("Palydovų ryšys: reikia tikslios vietos leidimo");
                statusText.setText("Vieta atnaujinama, kai programėlė atidaryta.");
            } catch (SecurityException | IllegalArgumentException ex) {
                statusText.setText("Nepavyko gauti vietos. Patikrinkite vietos nustatymo nustatymus.");
            }
        } else {
            statusText.setText("Leiskite nustatyti vietą, kad matytumėte save žemėlapyje.");
            gpsText.setText("Palydovų ryšys: reikia vietos leidimo");
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
            startSignal();
        else signalText.setText("Mobilusis signalas: reikia telefono leidimo");
    }

    private void showLocation(Location newLocation) {
        if (lastLocation != null && newLocation.getTime() < lastLocation.getTime() &&
            newLocation.getAccuracy() >= lastLocation.getAccuracy()) return;
        boolean first = lastLocation == null;
        lastLocation = newLocation;
        double lat = newLocation.getLatitude(), lon = newLocation.getLongitude();
        locationText.setText(String.format(Locale.getDefault(), "%.6f, %.6f", lat, lon));
        accuracyText.setText(String.format(Locale.getDefault(), "Tikslumas: apie ±%.0f m · %s",
            newLocation.getAccuracy(), DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(newLocation.getTime()))));
        GeoPoint point = new GeoPoint(lat, lon);
        if (marker == null) {
            marker = new Marker(map);
            marker.setTitle("Jūs esate čia");
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(marker);
        }
        marker.setPosition(point);
        map.invalidate();
        if (first) map.getController().animateTo(point);
        shareButton.setEnabled(true);
        if (lastAddressLocation == null || (System.currentTimeMillis() - lastAddressTime > 60000
            && lastAddressLocation.distanceTo(newLocation) > 100f)) {
            lastAddressLocation = newLocation;
            lastAddressTime = System.currentTimeMillis();
            lookupAddress(lat, lon);
        }
    }

    private void lookupAddress(double lat, double lon) {
        if (!Geocoder.isPresent()) {
            addressText.setText("Adresas: neprieinamas šiame telefone");
            return;
        }
        addressText.setText("Adresas: ieškoma…");
        Geocoder geocoder = new Geocoder(this, new Locale("lt", "LT"));
        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(lat, lon, 1, addresses -> runOnUiThread(() -> showAddress(addresses)));
        } else {
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    runOnUiThread(() -> showAddress(addresses));
                } catch (IOException | IllegalArgumentException ex) {
                    runOnUiThread(() -> addressText.setText("Adresas: nepavyko nustatyti"));
                }
            }).start();
        }
    }

    private void showAddress(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty() || addresses.get(0).getAddressLine(0) == null)
            addressText.setText("Adresas: nerastas (vietą rodo koordinatės)");
        else addressText.setText("Adresas: " + addresses.get(0).getAddressLine(0));
    }

    private void shareLocation() {
        if (lastLocation == null) return;
        String link = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f",
            lastLocation.getLatitude(), lastLocation.getLongitude(),
            lastLocation.getLatitude(), lastLocation.getLongitude());
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, "Mano vieta: " + link);
        startActivity(Intent.createChooser(send, "Bendrinti vietą"));
    }

    private void updateNetwork() {
        Network active = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : connectivityManager.getNetworkCapabilities(active);
        if (caps == null) networkText.setText("Internetas: nėra ryšio");
        else {
            String type = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" :
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "mobilieji duomenys" : "kitas tinklas";
            networkText.setText("Internetas: " + type + (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ? " · veikia" : " · nepatvirtintas"));
        }
    }

    private void startSignal() {
        if (signalCallback != null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS)) {
            if (signalCallback == null) signalText.setText("Mobilusis signalas: nėra modemo duomenų");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            signalCallback = new SignalListener();
            try { telephonyManager.registerTelephonyCallback(getMainExecutor(), signalCallback); }
            catch (SecurityException | UnsupportedOperationException ex) {
                signalCallback = null;
                signalText.setText("Mobilusis signalas: neprieinamas");
            }
        }
        if (Build.VERSION.SDK_INT < 28) {
            signalText.setText("Mobilusis signalas: rodmenys neprieinami šioje Android versijoje");
            return;
        }
        try {
            SignalStrength current = telephonyManager.getSignalStrength();
            if (current != null) showSignal(current);
        } catch (SecurityException | UnsupportedOperationException ignored) {
            signalText.setText("Mobilusis signalas: neprieinamas");
        }
    }

    private final class SignalListener extends TelephonyCallback implements TelephonyCallback.SignalStrengthsListener {
        @Override public void onSignalStrengthsChanged(SignalStrength value) { showSignal(value); }
    }

    private void showSignal(SignalStrength value) {
        List<CellSignalStrength> cells = value.getCellSignalStrengths();
        int dbm = cells.isEmpty() ? CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN : cells.get(0).getDbm();
        String detail = (dbm == CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN || dbm == Integer.MAX_VALUE)
            ? "" : " · " + dbm + " dBm";
        signalText.setText("Mobilusis signalas: " + value.getLevel() + "/4" + detail);
    }

    @Override protected void onStart() {
        super.onStart();
        map.onResume();
        if (locationManager != null && !tracking) startTracking();
        if (connectivityManager != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) { runOnUiThread(() -> updateNetwork()); }
                @Override public void onLost(Network network) { runOnUiThread(() -> updateNetwork()); }
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    runOnUiThread(() -> updateNetwork());
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override protected void onStop() {
        if (tracking) { locationManager.removeUpdates(locationListener); tracking = false; }
        if (gnssTracking) { locationManager.unregisterGnssStatusCallback(gnssCallback); gnssTracking = false; }
        if (signalCallback != null && Build.VERSION.SDK_INT >= 31) {
            telephonyManager.unregisterTelephonyCallback(signalCallback);
            signalCallback = null;
        }
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        map.onPause();
        super.onStop();
    }
}

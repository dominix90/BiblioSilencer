package com.unipi.domi.bibliosilencer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class LibrariesActivity extends AppCompatActivity implements LocationListener {
    /**
     * Parametri per views
     */

    private TextView lat;
    private TextView lon;
    private Button btnSearch;

    /**
     * Parametri per GPS
     */
    private LocationManager locationManager;
    private long uptimeAtResume;
    private List<String> enabledProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libraries);

        //Latitudine e Longitudine
        lat = findViewById(R.id.latitude);
        lon = findViewById(R.id.longitude);


        //Bottone
        btnSearch = (Button) findViewById(R.id.btnSearch);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        setButtonHandlers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuffer stringBuffer = new StringBuffer();
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        enabledProviders = locationManager.getProviders(criteria, true);
        /*if (enabledProviders.isEmpty())
        {
            enabledProvidersValue.setText("");
        }*/
        if (!enabledProviders.isEmpty())
        {
            for (String enabledProvider : enabledProviders)
            {
                stringBuffer.append(enabledProvider).append(" ");
                locationManager.requestSingleUpdate(enabledProvider, this, null);
            }
            //enabledProvidersValue.setText(stringBuffer);
        }
        uptimeAtResume = SystemClock.uptimeMillis();
        /*latitudeValue.setText("");
        longitudeValue.setText("");
        providerValue.setText("");
        accuracyValue.setText("");
        timeToFixValue.setText("");
        findViewById(R.id.timeToFixUnits).setVisibility(View.GONE);
        findViewById(R.id.accuracyUnits).setVisibility(View.GONE);*/
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    return;
        }
    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnSearch)).setOnClickListener(btnClick);
    }


    @Override
    public void onLocationChanged(Location location)
    {
        if (location!= null) {
            lat.setText(String.valueOf(location.getLatitude()));
            lon.setText(String.valueOf(location.getLongitude()));
        } else {
            Log.e("Errore GPS", "Location is null!");
        }
        /**
        providerValue.setText(location.getProvider());
        accuracyValue.setText(String.valueOf(location.getAccuracy()));
        long timeToFix = SystemClock.uptimeMillis() - uptimeAtResume;
        timeToFixValue.setText(String.valueOf(timeToFix / 1000));
        findViewById(R.id.timeToFixUnits).setVisibility(View.VISIBLE);
        findViewById(R.id.accuracyUnits).setVisibility(View.VISIBLE);
         */
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnSearch: {
                    Log.e("Ricerca posizione -->", "....");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ActivityCompat.checkSelfPermission(LibrariesActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LibrariesActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.INTERNET
                            }, 10);
                            return;
                        }
                    }
                    //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, LibrariesActivity.this);
                    //Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    /**
                     * CODICE SLIDE PROF
                     */
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    enabledProviders = locationManager.getProviders(criteria, true);
                    /*if (enabledProviders.isEmpty())
                    {
                        enabledProvidersValue.setText("");
                    }*/
                    if (!enabledProviders.isEmpty())
                    {
                        for (String enabledProvider : enabledProviders)
                        {
                            locationManager.requestSingleUpdate(enabledProvider, LibrariesActivity.this, null);
                        }
                        //enabledProvidersValue.setText(stringBuffer);
                    }

                    /**
                     *
                     */
                }
            }
        }
    };
}

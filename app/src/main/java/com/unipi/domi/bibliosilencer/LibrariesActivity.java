package com.unipi.domi.bibliosilencer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class LibrariesActivity extends AppCompatActivity implements LocationListener, AdapterView.OnItemClickListener {
    /**
     * Parametri per views
     */

    private Button btnSearch;
    private ListView listView;
    private ArrayList<Biblioteca> arrayListBiblioteche;
    private CustomAdapter adapterBiblioteche;
    private ProgressBar progressBar;
    private TextView progressBarText;
    private boolean listaPiena = false;

    /**
     * Parametri per GPS
     */
    private LocationManager locationManager;
    private long uptimeAtResume;
    private List<String> enabledProviders;
    private DrawerLayout mDrawerLayout;

    /**
     * Parametri per request
     */

    private String mainUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";
    private String personalUrl = "&radius=350&keyword=biblioteca&key=AIzaSyDkf5UfwaOpxBnDTnx_MZpuQKw0-D1GnCA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libraries);

        //Menu
        mDrawerLayout = findViewById(R.id.drawer_layout);

        //Caricamento del layout per il menu
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        switch (menuItem.getItemId()) {
                            case R.id.menuSettings:
                                newActivity(menuItem.getItemId());
                                return true;
                            case R.id.menuAbout:
                                newActivity(menuItem.getItemId());
                                return true;
                            case R.id.menuMain:
                                //ritorno alla main activity
                                return true;
                        }

                        return true;
                    }
                });

        //Layout toolbar
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        mainToolbar.setLogo(R.drawable.logo_text);

        //Lista
        listView = (ListView) findViewById(R.id.listaBiblioteche);
        arrayListBiblioteche = new ArrayList<Biblioteca>();

        //Bottone
        btnSearch = (Button) findViewById(R.id.btnSearch);

        //location manager per posizione
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //setto gli handler dei vari elementi cliccabili
        setButtonHandlers();

        //Progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        progressBarText = (TextView) findViewById(R.id.progressBarText);
        progressBarText.setVisibility(View.GONE);
    }

    //Nuova activity
    private void newActivity(int idActivity) {
        //Intent per avviare altra activity
        Intent intent;

        //check del parametro idActivity
        if (idActivity < 0)
            return;

        switch (idActivity) {
            case R.id.menuSettings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return;
            case R.id.menuAbout:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return;
            case R.id.menuLibraries:
                intent = new Intent(this, LibrariesActivity.class);
                startActivity(intent);
                return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        listView.setOnItemClickListener(LibrariesActivity.this);
    }


    @Override
    public void onLocationChanged(Location location)
    {
        Log.i("Location changed -->", location.toString());
        if (location!= null) {
            getBiblioteche(location.getLatitude(),location.getLongitude(), this);
        } else {
            Log.e("Errore GPS", "Location is null!");
        }
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
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
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
                    /**
                     * CODICE SLIDE PROF
                     */
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    enabledProviders = locationManager.getProviders(criteria, true);

                    if (!enabledProviders.isEmpty())
                    {
                        for (String enabledProvider : enabledProviders)
                        {
                            locationManager.requestSingleUpdate(enabledProvider, LibrariesActivity.this, null);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //String uri = String.format(Locale.ENGLISH, "geo:%f,%f", Float.parseFloat(arrayListBiblioteche.get(position).getLatitude()), Float.parseFloat(arrayListBiblioteche.get(position).getLongitude()));
        Uri loc = Uri.parse("geo:0,0?q=" + arrayListBiblioteche.get(position).getName());
        Intent intent = new Intent(Intent.ACTION_VIEW, loc);
        this.startActivity(intent);
    }

    public void getBiblioteche(double lat, double lon, final Context context) {

        // url a cui inviare la richiesta
        String url = mainUrl + String.valueOf(lat) + "," + String.valueOf(lon) + personalUrl;
        Log.i("URL ---> ", url);

        //codice per richiesta
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            String nome = "";
                            double latBiblioteca, lonBiblioteca;
                            for (int i = 0; i < results.length(); i++) {
                                nome = results.getJSONObject(i).getString("name");
                                latBiblioteca = Double.parseDouble(results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat"));
                                lonBiblioteca = Double.parseDouble(results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng"));
                                arrayListBiblioteche.add(new Biblioteca(nome, lonBiblioteca, latBiblioteca));
                            }
                            adapterBiblioteche = new CustomAdapter (context, arrayListBiblioteche);
                            listView.setAdapter(adapterBiblioteche);
                            progressBar.setVisibility(View.GONE);
                            progressBarText.setVisibility(View.GONE);
                            Log.i("Lunghezza inMethod -->", String.valueOf(arrayListBiblioteche.size()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Error in RESPONSE", error.getMessage());
                    }
                });
        queue.add(jsonObjectRequest);
    }
}

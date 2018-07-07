package com.unipi.domi.bibliosilencer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;


public class MainActivity extends AppCompatActivity implements LocationListener {

    /**
     * ATTRIBUTI
     */

    //CONTROLLO PREFERENZE
    private PrefManager prefManager;
    private boolean audioOn = false;

    //LAYOUT
    private ImageButton btnPosition;
    private TextView decibel;
    private TextView decibelMin;
    private TextView decibelMax;
    private TextView biblioGps;
    private DrawerLayout mDrawerLayout;
    private ProgressBar progressBar;
    private TextView progressBarText;

    //SUONI
    private MediaPlayer mp;
    private MediaPlayer mp1;
    private MediaPlayer mp2;

    //PERMESSI
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 10;
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 20;

    //DATI BIBLIOTECA
    private Biblioteca inBiblioteca;
    private ArrayList<Biblioteca> locationBiblioteche;
    private ArrayList<Biblioteca> soundsBiblioteche;
    private Gson gson;

    /**
     * PARAMETRI PER AUDIO RECORD
     */
    private static final int RECORDER_SAMPLERATE = 44100;
    private final static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final static int BYTES_PER_ELEMENT = 2;
    private final static int BLOCK_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
            / BYTES_PER_ELEMENT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private static final int INITIAL_DELAY = 150;
    int initialDelay = 0;
    private double minValueDecibel = 0;
    private double maxValueDecibel = 0;
    double maxAmpReached = 0.0; //massima ampiezza raggiunta nell'intervallo di 15 secondi

    //calcolo media del rumore
    private double averageSound = 0.00;
    private double samplesSum = 0.00;
    private int samplesCount = 0;

    /**
     * Parametri per GPS
     */
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Layout generale
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Controllo se l'audio è attivo
        prefManager = new PrefManager(this);
        if (prefManager.isAudioOn()) {
            audioOn = true;
        }

        //Menu
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        switch (menuItem.getItemId()) {
                            case R.id.menuSettings:
                                newActivity(menuItem.getItemId());
                                return true;
                            case R.id.menuAbout:
                                newActivity(menuItem.getItemId());
                                return true;
                            case R.id.menuLibraries:
                                newActivity(menuItem.getItemId());
                                return true;
                            case R.id.menuStats:
                                newActivity(menuItem.getItemId());
                                return true;
                        }
                        return true;
                    }
                });

        //Layout toolbar
        Toolbar mainToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mainToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        //TextView decibel
        decibel = new TextView(this);
        decibel = findViewById(R.id.decibel);
        decibel.setText(R.string.zero_value);

        //TextView decibelMin
        decibelMin = new TextView(this);
        decibelMin = findViewById(R.id.decibelmin);
        decibelMin.setText(R.string.zero_value);

        //TextView decibelMax
        decibelMax = new TextView(this);
        decibelMax = findViewById(R.id.decibelmax);
        decibelMax.setText(R.string.zero_value);

        //Bottone posizione
        btnPosition = findViewById(R.id.btnPosition);

        //TextView biblioGps
        biblioGps = findViewById(R.id.biblioGps);

        //Progress bar
        progressBar = findViewById(R.id.progressBarMain);
        progressBar.setVisibility(View.GONE);
        progressBarText = findViewById(R.id.progressBarTextMain);
        progressBarText.setVisibility(View.GONE);

        //Attivazione bottoni
        setButtonHandlers();
        enableButtons(false);

        //location manager per posizione
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (inBiblioteca == null)
            registraBiblioteca();

        //GSON object per serializzazione delle shared preferences
        gson = new GsonBuilder().create();
    }

    @Override
    public void onResume(){
        super.onResume();
        //Controllo se l'audio è attivo
        prefManager = new PrefManager(this);
        audioOn = prefManager.isAudioOn();
    }

    @Override
    public void onPause(){
        super.onPause();
        enableButtons(false);
        stopRecording();
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
            case R.id.menuStats:
                intent = new Intent(this, StatsActivity.class);
                startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gestione dei click sul menu
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * BOTTONI
     */

    private void setButtonHandlers() {
        findViewById(R.id.btnStart).setOnClickListener(btnClick);
        findViewById(R.id.btnStop).setOnClickListener(btnClick);
        findViewById(R.id.btnPosition).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        findViewById(id).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    /**
     * NOTIFICHE TESTUALI
     */

    public void generaNotifica(String titolo, String testo, String info) {
        int ID = 1;
        Notification.Builder nb = new Notification.Builder(this);
        nb.setContentTitle(titolo);
        nb.setContentText(testo);
        nb.setContentInfo(info);
        nb.setVibrate(new long[] { 500, 500});
        nb.setSmallIcon(R.drawable.ic_silence);
        nb.setAutoCancel(true);
        Notification n = nb.build();
        NotificationManager nm =
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(ID, n);
        }
    }

    /**
     * REGISTRAZIONE
     */

    private void startRecording() {

        //richiesta permessi per microfono
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.RECORD_AUDIO
                }, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                return;
            }
        }

        //MediaPlayer
        mp = MediaPlayer.create(getApplicationContext(), R.raw.sh);
        mp1 = MediaPlayer.create(getApplicationContext(), R.raw.shh);
        mp2 = MediaPlayer.create(getApplicationContext(), R.raw.cumpa);

        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BLOCK_SIZE * BYTES_PER_ELEMENT);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                // Write the output audio in byte
                short sData[] = new short[BLOCK_SIZE];

                int recordDelay = 0;
                boolean soundCheck = true;
                int soundCheckCount = 0;

                while (isRecording) {
                    /*
                      Aggiunta di un delay
                      I valori iniziali della registrazione sono troppo alti,
                      probabilmente il problema è dovuto all'attivazione del microfono.
                      Pian piano questi valori si normalizzano e possiamo iniziare ad utilizzare i dati.
                     */


                    // gets the voice output from microphone to byte format
                    double sum = 0;
                    int readSize = recorder.read(sData, 0, BLOCK_SIZE);

                    initialDelay++;
                    if (initialDelay < INITIAL_DELAY) {
                        Log.e("Delay -->", (Integer.toString(initialDelay)));
                        continue;
                    }

                    recordDelay++;
                    soundCheckCount++;
                    if (recordDelay < 10)
                        continue;

                    for (int i = 0; i < readSize; i++) {
                        sum += sData[i] * sData[i];
                    }

                    if (readSize > 0) {
                        final double amplitude = sum / readSize;
                        recordDelay = 0;

                        //TextView decibel
                        runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                double currentValueDecibel = Math.round(20 * Math.log10(Math.abs(Math.sqrt(amplitude))));
                                String currentValue = Double.toString(currentValueDecibel) + " dB";
                                TextView sound = findViewById(R.id.decibel);
                                sound.setText(currentValue);
                                if (minValueDecibel == 0 || minValueDecibel > currentValueDecibel) {
                                    minValueDecibel = currentValueDecibel;
                                    sound = findViewById(R.id.decibelmin);
                                    sound.setText(Double.toString(minValueDecibel) + " dB");
                                }
                                if (maxValueDecibel < currentValueDecibel) {
                                    maxValueDecibel = currentValueDecibel;
                                    sound = findViewById(R.id.decibelmax);
                                    sound.setText(Double.toString(maxValueDecibel) + " dB");
                                }

                            }
                        });

                        Log.i("soundCheckCount -->", String.valueOf(soundCheckCount));
                        if (soundCheckCount == 250)
                            soundCheck = true;

                        //TextView decibel
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                progressBarText.setVisibility(View.GONE);
                            }
                        });

                        //Qui va fatto il controllo del livello corrente di ampiezza, se è elevato va inviata la notifica (audio/testuale)
                        if (soundCheck) {
                            soundCheckCount = 0;
                            Double amplitudeDB = 20 * Math.log10(Math.abs(Math.sqrt(amplitude)));
                            //aggiungo per il calcolo della media e incremento il count
                            samplesSum += amplitudeDB;
                            samplesCount++;

                            if (amplitudeDB > maxAmpReached)
                                maxAmpReached = amplitudeDB;
                            if (audioOn) {
                                //Se il livello del suono è oltre un certo livello viene inviata la notifica audio
                                if (maxAmpReached >= 50 && maxAmpReached < 60) {
                                    mp.start();
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                }
                                if (maxAmpReached >= 60 && maxAmpReached < 70) {
                                    mp1.start();
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                }
                                if (maxAmpReached >= 70) {
                                    mp2.start();
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                }
                            } else {
                                //Se il livello del suono è oltre un certo livello viene inviata la notifica testuale
                                if (maxAmpReached >= 50) {
                                    soundCheck = false;
                                    generaNotifica("High noise detected!", String.valueOf(Math.round(maxAmpReached)) + "dB", "");
                                    maxAmpReached = 0;
                                }
                            }
                        }
                    }
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            initialDelay = 0;
            averageSound = samplesSum/samplesCount;
            saveScoreListToSharedpreference();
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart:
                    enableButtons(true);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
                    startRecording();
                    break;
                case R.id.btnStop:
                    enableButtons(false);
                    progressBar.setVisibility(View.GONE);
                    progressBarText.setVisibility(View.GONE);
                    stopRecording();
                    break;
                case R.id.btnPosition:
                    registraBiblioteca();
                    break;
            }
        }
    };

    /**
     * Aggiungo alla lista il nuovo dato
     */
    private void saveScoreListToSharedpreference() {
        soundsBiblioteche = new ArrayList<>();
        if (inBiblioteca != null) {
            //ottengo la lista
            getHighScoreListFromSharedPreference();

            boolean alreadyBeen = false;
            int indexAlreadyBenn = -1;
            double latitude = Double.parseDouble(inBiblioteca.getLatitude());
            double longitude = Double.parseDouble(inBiblioteca.getLongitude());

            for (Biblioteca biblioteca : soundsBiblioteche) {
                if (biblioteca.HaversineInM(latitude, longitude) == 0) {
                    alreadyBeen = true;
                    indexAlreadyBenn = soundsBiblioteche.indexOf(biblioteca);
                }
            }

            if (alreadyBeen)
                soundsBiblioteche.remove(indexAlreadyBenn);

            inBiblioteca.setAverageSound(Math.round(averageSound*100)/100);
            soundsBiblioteche.add(inBiblioteca);

            for (Biblioteca biblioteca: soundsBiblioteche) {
                Log.e("---------->",biblioteca.toString());
            }

            //converto l'ArrayList in String da Gson
            Type type = new TypeToken<List<Biblioteca>>() {
            }.getType();
            String jsonScore = gson.toJson(soundsBiblioteche, type);

            //salvo nelle shared preferences
            prefManager.saveAverageSoundList(jsonScore);
        }
    }

    /**
     * Ottengo la lista
     */
    private void getHighScoreListFromSharedPreference() {
        //ottengo i dati dalle shared preferences
        String jsonScore = prefManager.getAverageSoundList();
        soundsBiblioteche = new ArrayList<>();
        if (!jsonScore.equals("")) {
            Log.i("--->", jsonScore);
            Type type = new TypeToken<List<Biblioteca>>() {
            }.getType();
            soundsBiblioteche = gson.fromJson(jsonScore, type);
        }
    }

    /**
     * Ottenimento registrazione biblioteca (se l'utente si trova entro 20m da una biblioteca)
     */

    public void getBiblioteche(double lat, double lon, final Context context) {

        // url a cui inviare la richiesta
        String personalUrl = "&radius=350&keyword=biblioteca&key=AIzaSyDkf5UfwaOpxBnDTnx_MZpuQKw0-D1GnCA";
        String mainUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=";

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
                            String nome;
                            //Parametri temporanei per biblioteche
                            List<String> biblioNames;
                            double latBiblioteca, lonBiblioteca;
                            if (results.length() > 0) {
                                locationBiblioteche = new ArrayList<>();
                                biblioNames = new ArrayList<>();
                                for (int i = 0; i < results.length(); i++) {
                                    nome = results.getJSONObject(i).getString("name");
                                    latBiblioteca = Double.parseDouble(results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat"));
                                    lonBiblioteca = Double.parseDouble(results.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng"));
                                    locationBiblioteche.add(new Biblioteca(nome, lonBiblioteca, latBiblioteca));
                                    biblioNames.add(nome);
                                }
                                biblioNames.add("I'm not in a library");
                                setBiblioteca(biblioNames);
                            } else {
                                //TextView biblioName
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        biblioGps.setText("You are not in a library\nClick on the top right corner to update");
                                    }
                                });
                            }
                            progressBar.setVisibility(View.GONE);
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

    private void setBiblioteca(List<String> biblioNames) {
        //lista scelte dialog
        final CharSequence[] bibliotecheNeiDintorni = biblioNames.toArray(new CharSequence[locationBiblioteche.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select a library");
        builder.setItems(bibliotecheNeiDintorni, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String selected = bibliotecheNeiDintorni[item].toString().trim();
                if(selected.equals("I'm not in a library")) {
                    //TextView biblioName
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            biblioGps.setText("You are not in a library\nClick on the top right corner to update");
                        }
                    });
                } else {
                    for (Biblioteca b : locationBiblioteche) {
                        if(b.getName().equals(selected)) {
                            inBiblioteca = b;
                            //TextView biblioName
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    biblioGps.setText(inBiblioteca.getName());
                                }
                            });
                        }
                    }
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * GPS
     */

    public void registraBiblioteca() {
        progressBar.setVisibility(View.VISIBLE);
        //Registrazione posizione
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, MY_PERMISSIONS_REQUEST_LOCATION);
                return;
            }
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        List<String> enabledProviders = locationManager.getProviders(criteria, true);

        if (!enabledProviders.isEmpty())
        {
            for (String enabledProvider : enabledProviders)
            {
                locationManager.requestSingleUpdate(enabledProvider, this, null);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.i("Location changed -->", location.toString());
        getBiblioteche(location.getLatitude(),location.getLongitude(), this);
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

    /**
     * Gestione dei permessi
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    registraBiblioteca();
                else {
                    progressBar.setVisibility(View.GONE);
                }
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
                    enableButtons(true);
                    startRecording();
                }
                else {
                    progressBar.setVisibility(View.GONE);
                    progressBarText.setVisibility(View.GONE);
                    enableButtons(false);
                    stopRecording();
                }
        }
    }
}

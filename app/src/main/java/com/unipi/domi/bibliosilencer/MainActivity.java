package com.unipi.domi.bibliosilencer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.media.audiofx.NoiseSuppressor;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //ATTRIBUTI
    //--- ATTRIBUTI ---
    //LAYOUT
    TextView decibel;
    Button sensingButton;
    Button taraturaButton;
    //SUONI
    MediaPlayer mp;
    MediaPlayer mp1;
    MediaPlayer mp2;
    //PERMESSI
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    //TARATURA
    boolean isTarato = false;
    double silenzio = 0.0;
    int samples = 0;
    int samples_sum = 0;
    //REGISTAZIONE
    Timer timer;
    MediaRecorder recorder;
    int contatore = 0;
    double maxAmpReached = 0.0; //massima ampiezza raggiunta nell'intervallo di 15 secondi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Layout generale
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Layout toolbar
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        //Button sensing
        sensingButton = new Button(this);
        sensingButton = (Button) findViewById(R.id.sensing);
        sensingButton.setOnClickListener(this);
        sensingButton.setEnabled(false);

        //Button taratura
        taraturaButton = new Button(this);
        taraturaButton = (Button) findViewById(R.id.taratura);
        taraturaButton.setOnClickListener(this);
        taraturaButton.setEnabled(true);

        //TextView decibel
        decibel = new TextView(this);
        decibel = (TextView) findViewById(R.id.decibel);
        decibel.setText("0.00 dB");
    }

    //Nuova activity
    private void newActivity(int idActivity) {
        //Intent per avviare altra activity
        Intent intent = null;


        //check del parametro idActivity
        if (idActivity < 0)
            return;

        switch (idActivity) {
            case R.id.menuAudio:
                intent = new Intent(this, AudioSettingsActivity.class);
                return;
            case R.id.menuWidget:
                intent = new Intent(this, WidgetSettingsActivity.class);
                return;
            case R.id.menuAbout:
                intent = new Intent(this, AboutActivity.class);
                return;
        }

        if (intent != null)
            startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gestione dei click sul menu
        switch (item.getItemId()) {
            case R.id.menuAudio:
                newActivity(item.getItemId());
                return true;
            case R.id.menuWidget:
                newActivity(item.getItemId());
                return true;
            case R.id.menuAbout:
                newActivity(item.getItemId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
            // Request for RECORD AUDIO permission.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                sensingButton.setEnabled(true);
            } else {
                // Permission request was denied.

            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }

    private void preparaMicrofono() {
        // Check dei permessi del microfono
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            // PERMESSI OK
            sensingButton.setEnabled(true);
        } else {
            // Permission is missing and must be requested.
            requestRecordAudioPermission();
        }
        // END_INCLUDE(startCamera)
    }

    /**
     * Requests the {@link android.Manifest.permission#RECORD_AUDIO} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private void requestRecordAudioPermission() {
        // I permessi non sono garantiti e devono essere richieste.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
        else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.taratura) {
            preparaMicrofono();
            taraSensore();
        }
        if (v.getId() == R.id.sensing) {
            sensingActivation();
            //isTarato = true;
        }
    }

    public void sensingActivation() {
        //Sound Meter
        //MediaRecorder recorder = new MediaRecorder();
        //recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new RecorderTask(recorder), 0, 500);

        //MediaPlayer
        mp = MediaPlayer.create(getApplicationContext(), R.raw.sh);
        mp1 = MediaPlayer.create(getApplicationContext(), R.raw.shh);
        mp2 = MediaPlayer.create(getApplicationContext(), R.raw.cumpa);

        /*recorder.setOutputFile("/dev/null");

        try {
            recorder.prepare();
            recorder.start();
        } catch(Exception e) {
            e.printStackTrace();
        }*/
    }

    public void taraSensore() {
        //Sound Meter
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        timer = new Timer();
        timer.scheduleAtFixedRate(new RecorderTask(recorder), 0, 250);

        recorder.setOutputFile("/dev/null");

        try {
            recorder.prepare();
            recorder.start();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private class RecorderTask extends TimerTask {
        TextView sound = (TextView) findViewById(R.id.decibel);
        private MediaRecorder recorder;

        public RecorderTask(MediaRecorder recorder) {
            this.recorder = recorder;
        }

        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int amplitude = recorder.getMaxAmplitude();
                    double amplitudeDb = 20 * Math.log10((double)Math.abs(amplitude));
                    double checkAmp = amplitudeDb - silenzio;
                    if (isTarato) {
                        //controllo se il massimo livello di pressione acustica è stato superato
                        if (checkAmp > maxAmpReached)
                            maxAmpReached = checkAmp;

                        sound.setText("" + Math.round(amplitudeDb) + " dB - CONTATORE: " + contatore + " - MAX: " + maxAmpReached);

                        if (contatore == 0) {
                            if (maxAmpReached >= 10 && maxAmpReached < 20) {
                                mp.start();
                                maxAmpReached = 0.0;
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                }
                            }
                            if (maxAmpReached >= 20 && maxAmpReached < 30) {
                                mp1.start();
                                maxAmpReached = 0.0;
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                }
                            }
                            if (maxAmpReached >= 30) {
                                mp2.start();
                                maxAmpReached = 0.0;
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                }
                            }
                        }

                        contatore ++;

                        if (contatore == 30)
                            contatore = 0;
                    }
                    else {
                        samples += 1;
                        samples_sum += amplitude;
                        silenzio = 20 * Math.log10((double)Math.abs((samples_sum/samples)));
                        if (samples == 20) {
                            sound.setText("Taratura completata. Silenzio = " + silenzio);
                            isTarato = true;
                            timer.cancel();
                            sensingButton.setEnabled(true);
                        }
                    }
                }
            });
        }
    }
}

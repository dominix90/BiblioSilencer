package com.unipi.domi.bibliosilencer;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.provider.Settings;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.MapView;


public class MainActivity extends AppCompatActivity {

    //ATTRIBUTI
    //--- ATTRIBUTI ---
    //LAYOUT
    TextView decibel;
    TextView lat;
    TextView lon;
    Button btnStart;
    Button btnStop;
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
    /**
    //REGISTAZIONE
    Timer timer;
    MediaRecorder recorder;
     */
    int contatore = 0;
    double maxAmpReached = 0.0; //massima ampiezza raggiunta nell'intervallo di 15 secondi

    /**
     * PARAMETRI PER AUDIO RECORD
     */
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String currentValueDb = "";
    int initialDelay = 0;

    /**
     * PARAMETRI PER GPS
     */
    //Attributi
    LocationManager locationManager;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                lat.setText("Lat: " + Double.toString(location.getLatitude()));
                lon.setText("Long: " + Double.toString(location.getLongitude()));
            } else {
                Log.e("GPS error -->","Location is null");
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Layout generale
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Layout toolbar
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        //TextView decibel
        decibel = new TextView(this);
        decibel = (TextView) findViewById(R.id.decibel);
        decibel.setText("0.00 dB");

        //TextView decibel
        lat = new TextView(this);
        lat = (TextView) findViewById(R.id.latitude);

        //TextView decibel
        lon = new TextView(this);
        lon = (TextView) findViewById(R.id.longitude);

        //Attivazione bottoni
        setButtonHandlers();
        enableButtons(false);

        //Audio Record
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    //Nuova activity
    private void newActivity(int idActivity) {
        //Intent per avviare altra activity
        Intent intent;

        //check del parametro idActivity
        if (idActivity < 0)
            return;

        switch (idActivity) {
            case R.id.menuAudio:
                intent = new Intent(this, AudioSettingsActivity.class);
                startActivity(intent);
                return;
            case R.id.menuWidget:
                intent = new Intent(this, WidgetSettingsActivity.class);
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
            case R.id.menuLibraries:
                newActivity(item.getItemId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        // BEGIN_INCLUDE(onRequestPermissionsResult)
//        if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_AUDIO) {
//            // Request for RECORD AUDIO permission.
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission has been granted.
//                sensingButton.setEnabled(true);
//            } else {
//                // Permission request was denied.
//
//            }
//        }
//        // END_INCLUDE(onRequestPermissionsResult)
//    }
//
//    private void preparaMicrofono() {
//        // Check dei permessi del microfono
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//                == PackageManager.PERMISSION_GRANTED) {
//            // PERMESSI OK
//            sensingButton.setEnabled(true);
//            taraSensore();
//        } else {
//            // Permission is missing and must be requested.
//            requestRecordAudioPermission();
//        }
//    }
//
//    /**
//     * Requests the {@link android.Manifest.permission#RECORD_AUDIO} permission.
//     * If an additional rationale should be displayed, the user has to launch the request from
//     * a SnackBar that includes additional information.
//     */
//    private void requestRecordAudioPermission() {
//        // I permessi non sono garantiti e devono essere richieste.
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
//        }
//        else {
//            // Request the permission. The result will be received in onRequestPermissionResult().
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
//        }
//    }
//
//    public void onClick(View v) {
//        if (v.getId() == R.id.taratura) {
//            Log.e("Errore","-- Calibrating microphone -- ");
//            preparaMicrofono();
//        }
//        if (v.getId() == R.id.sensing) {
//            Log.e("Errore","-- Starting sensing task -- ");
//            sensingActivation();
//            //isTarato = true;
//        }
//    }
//
//    public void sensingActivation() {
//        //Sound Meter
//        //MediaRecorder recorder = new MediaRecorder();
//        //recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        //recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        //recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        Timer timer = new Timer();
//        timer.scheduleAtFixedRate(new RecorderTask(recorder), 0, 500);
//
//        //MediaPlayer
//        mp = MediaPlayer.create(getApplicationContext(), R.raw.sh);
//        mp1 = MediaPlayer.create(getApplicationContext(), R.raw.shh);
//        mp2 = MediaPlayer.create(getApplicationContext(), R.raw.cumpa);
//
//        /*recorder.setOutputFile("/dev/null");
//
//        try {
//            recorder.prepare();
//            recorder.start();
//        } catch(Exception e) {
//            e.printStackTrace();
//        }*/
//    }
//
//    public void taraSensore() {
//        //Sound Meter
//        recorder = new MediaRecorder();
//        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        timer = new Timer();
//        timer.scheduleAtFixedRate(new RecorderTask(recorder), 0, 250);
//
//        recorder.setOutputFile("/dev/null");
//
//        try {
//            recorder.prepare();
//            recorder.start();
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    private class RecorderTask extends TimerTask {
//        TextView sound = (TextView) findViewById(R.id.decibel);
//        private MediaRecorder recorder;
//
//        public RecorderTask(MediaRecorder recorder) {
//            this.recorder = recorder;
//        }
//
//        public void run() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    int amplitude = recorder.getMaxAmplitude();
//                    double amplitudeDb = 20 * Math.log10((double)Math.abs(amplitude));
//                    double checkAmp = amplitudeDb - silenzio;
//                    if (isTarato) {
//                        //controllo se il massimo livello di pressione acustica è stato superato
//                        if (checkAmp > maxAmpReached)
//                            maxAmpReached = checkAmp;
//
//                        sound.setText("" + Math.round(amplitudeDb));
//
//                        /*if (contatore == 0) {
//                            if (maxAmpReached >= 10 && maxAmpReached < 20) {
//                                mp.start();
//                                maxAmpReached = 0.0;
//                                try {
//                                    Thread.sleep(5000);
//                                } catch (InterruptedException ie) {
//                                    ie.printStackTrace();
//                                }
//                            }
//                            if (maxAmpReached >= 20 && maxAmpReached < 30) {
//                                mp1.start();
//                                maxAmpReached = 0.0;
//                                try {
//                                    Thread.sleep(5000);
//                                } catch (InterruptedException ie) {
//                                    ie.printStackTrace();
//                                }
//                            }
//                            if (maxAmpReached >= 30) {
//                                mp2.start();
//                                maxAmpReached = 0.0;
//                                try {
//                                    Thread.sleep(5000);
//                                } catch (InterruptedException ie) {
//                                    ie.printStackTrace();
//                                }
//                            }
//                        }*/
//
//                        contatore ++;
//
//                        if (contatore == 30)
//                            contatore = 0;
//                    }
//                    else {
//                        samples += 1;
//                        samples_sum += amplitude;
//                        silenzio = 20 * Math.log10((double)Math.abs((samples_sum/samples)));
//                        if (samples == 20) {
//                            sound.setText("Silenzio = " + Math.round(silenzio));
//                            isTarato = true;
//                            timer.cancel();
//                            sensingButton.setEnabled(true);
//                        }
//                    }
//                }
//            });
//        }
//    }


    /**
     * Utilizzo Audio Record - TEST
     */

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                // Write the output audio in byte
                short sData[] = new short[BufferElements2Rec];

                int recordDelay = 0;

                while (isRecording) {
                    /**
                     * Aggiunta di un delay
                     * I valori iniziali della registrazione sono troppo alti
                     * come in presenza di un disturbo. Pian piano questi valori si
                     * normalizzano e possiamo iniziare ad utilizzare i dati.
                     */
                    Log.e("Delay -->", (Integer.toString(initialDelay)));
                    initialDelay++;
                    if (initialDelay < 10000)
                        continue;

                    // gets the voice output from microphone to byte format
                    double sum = 0;
                    int readSize = recorder.read(sData, 0, BufferElements2Rec);

                    recordDelay++;
                    if (recordDelay < 20)
                        continue;

                    for (int i = 0; i < readSize; i++) {
                        sum += sData[i] * sData[i];
                    }
                    if (readSize > 0) {
                        final double amplitude = sum / readSize;
                        recordDelay = 0;
                        //TextView decibel

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String currentValue = Double.toString(Math.round(20 * Math.log10(Math.abs(Math.sqrt(amplitude))))) + " dB";
                                TextView sound = (TextView) findViewById(R.id.decibel);
                                sound.setText(currentValue);
                            }
                        });
                    }
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            initialDelay = 0;
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    locationManager.requestLocationUpdates("gps",5000,0, locationListener);
                    startRecording();
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    /**
     * FINE Utilizzo Audio Record - TEST
     */
}

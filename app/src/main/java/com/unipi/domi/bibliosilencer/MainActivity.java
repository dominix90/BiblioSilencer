package com.unipi.domi.bibliosilencer;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    /**
     * ATTRIBUTI
     */

    //CONTROLLO PREFERENZE
    private PrefManager prefManager;
    private boolean audioOn = false;


    //LAYOUT
    private TextView decibel;
    private TextView lat;
    private TextView lon;
    private Button btnStart;
    private Button btnStop;
    private DrawerLayout mDrawerLayout;
    private ProgressBar progressBar;
    private TextView progressBarText;

    //SUONI
    private MediaPlayer mp;
    private MediaPlayer mp1;
    private MediaPlayer mp2;

    //PERMESSI
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;

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
    int initialDelay = 0;

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
                            case R.id.menuLibraries:
                                newActivity(menuItem.getItemId());
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

        //TextView decibel
        decibel = new TextView(this);
        decibel = (TextView) findViewById(R.id.decibel);
        decibel.setText("0.00 dB");

        //Progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBarMain);
        progressBar.setVisibility(View.GONE);
        progressBarText = (TextView) findViewById(R.id.progressBarTextMain);
        progressBarText.setVisibility(View.GONE);

        //Attivazione bottoni
        setButtonHandlers();
        enableButtons(false);

        //Audio Record
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    @Override
    public void onResume(){
        super.onResume();
        //Controllo se l'audio è attivo
        prefManager = new PrefManager(this);
        if (prefManager.isAudioOn()) {
            audioOn = true;
        } else {
            audioOn = false;
        }
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

        //MediaPlayer
        mp = MediaPlayer.create(getApplicationContext(), R.raw.sh);
        mp1 = MediaPlayer.create(getApplicationContext(), R.raw.shh);
        mp2 = MediaPlayer.create(getApplicationContext(), R.raw.cumpa);

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
                boolean soundCheck = true;
                int soundCheckCount = 0;

                while (isRecording) {
                    /**
                     * Aggiunta di un delay
                     * I valori iniziali della registrazione sono troppo alti,
                     * probabilmente il problema è dovuto all'attivazione del microfono.
                     * Pian piano questi valori si normalizzano e possiamo iniziare ad utilizzare i dati.
                     */


                    // gets the voice output from microphone to byte format
                    double sum = 0;
                    int readSize = recorder.read(sData, 0, BufferElements2Rec);

                    initialDelay++;
                    if (initialDelay < 300) {
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
                            @Override
                            public void run() {
                                String currentValue = Double.toString(Math.round(20 * Math.log10(Math.abs(Math.sqrt(amplitude))))) + " dB";
                                TextView sound = (TextView) findViewById(R.id.decibel);
                                sound.setText(currentValue);
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
                                if (maxAmpReached >= 50 && maxAmpReached < 60) {
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                    //mp.start();
                                }
                                if (maxAmpReached >= 60 && maxAmpReached < 70) {
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                    //mp1.start();
                                }
                                if (maxAmpReached >= 70) {
                                    soundCheck = false;
                                    maxAmpReached = 0;
                                    //mp2.start();
                                }
                            }
                        }
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
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
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

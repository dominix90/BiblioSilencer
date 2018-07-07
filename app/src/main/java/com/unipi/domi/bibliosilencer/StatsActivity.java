package com.unipi.domi.bibliosilencer;

import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    //LAYOUT
    private ImageButton btnSearch;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView progressBarText, infoText;
    private DrawerLayout mDrawerLayout;

    //CONTROLLO PREFERENZE
    private PrefManager prefManager;

    //LISTA
    private ArrayList<Biblioteca> arrayListBiblioteche;
    private CustomAdapter adapterBiblioteche;

    //DATI BIBLIOTECA
    private ArrayList<Biblioteca> soundsBiblioteche;
    private HashSet<String> scoreset;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

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
                            case R.id.menuMain:
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

        //Lista
        listView = (ListView) findViewById(R.id.listaStatsBiblioteche);
        arrayListBiblioteche = new ArrayList<Biblioteca>();

        //Bottone
        btnSearch = (ImageButton) findViewById(R.id.btnSearch);


        //setto gli handler dei vari elementi cliccabili
        setButtonHandlers();

        //Testo informativo
        infoText = (TextView) findViewById(R.id.infoTextStats);

        //Progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        progressBarText = (TextView) findViewById(R.id.progressBarText);
        progressBarText.setVisibility(View.GONE);

        //PrefManager per shared preferences
        prefManager = new PrefManager(this);

        //Gson per de-serializzare shared preferences
        gson = new GsonBuilder().create();
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
            case R.id.menuMain:
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return;
            case R.id.menuLibraries:
                intent = new Intent(this, LibrariesActivity.class);
                startActivity(intent);
                return;
        }
    }

    private void setButtonHandlers() {
        btnSearch.setOnClickListener(btnClick);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnSearch: {
                    Log.e("Load biblioteche -->", "....");
                    infoText.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBarText.setVisibility(View.VISIBLE);
                    arrayListBiblioteche = getHighScoreListFromSharedPreference();
                    adapterBiblioteche = new CustomAdapter (StatsActivity.this, arrayListBiblioteche, 0, 0);
                    listView.setAdapter(adapterBiblioteche);
                    progressBar.setVisibility(View.GONE);
                    progressBarText.setVisibility(View.GONE);
                }
            }
        }
    };

    /**
     * Ottengo la lista
     */
    private ArrayList<Biblioteca> getHighScoreListFromSharedPreference() {
        //ottengo i dati dalle shared preferences
        ArrayList<Biblioteca> listaTemp = new ArrayList<>();
        String jsonScore = prefManager.getAverageSoundList();
        Type type = new TypeToken<List<Biblioteca>>(){}.getType();
        listaTemp = gson.fromJson(jsonScore, type);

        if (listaTemp == null) {
            listaTemp = new ArrayList<>();
        }

        return listaTemp;
    }
}

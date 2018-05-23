package com.unipi.domi.bibliosilencer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Layout generale
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Layout toolbar
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);
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
            case R.id.menuWidget:
                intent = new Intent(this, WidgetSettingsActivity.class);
            case R.id.menuAbout:
                intent = new Intent(this, AboutActivity.class);
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
}

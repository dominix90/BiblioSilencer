package com.unipi.domi.bibliosilencer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<Biblioteca> listaBiblioteche;
    private double latitude, longitude;

    public CustomAdapter(Context context, List<Biblioteca> listaBiblioteche, double latitude, double longitude) {
        this.context = context;
        this.listaBiblioteche = listaBiblioteche;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public int getCount() {
        return listaBiblioteche.size();
    }

    @Override
    public Object getItem(int position) {
        return listaBiblioteche.get(position);
    }

    @Override
    public long getItemId(int position) {
        return listaBiblioteche.indexOf(getItem(position));
    }

    /**
     * Classe innestata per la gestione delle Views
     */
    private class ViewHolder {
        TextView nomeBiblioteca;
        TextView distanza;
        TextView averageSound;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();

            holder.nomeBiblioteca = (TextView) convertView.findViewById(R.id.biblioName);
            holder.distanza = (TextView) convertView.findViewById(R.id.distanza);
            holder.averageSound = (TextView) convertView.findViewById(R.id.averageSound);

            Biblioteca biblioteca = listaBiblioteche.get(position);

            holder.nomeBiblioteca.setText(biblioteca.getName());
            holder.distanza.setText("Distance: " + biblioteca.HaversineInM(latitude, longitude) + "m");
            holder.averageSound.setText("Average noise: " + biblioteca.getAverageSound() + "dB");

            if (latitude == 0)
                holder.distanza.setVisibility(View.GONE);
            else
                holder.averageSound.setVisibility(View.GONE);
        }
        return convertView;
    }
}

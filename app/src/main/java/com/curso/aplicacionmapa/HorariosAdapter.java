package com.curso.aplicacionmapa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class HorariosAdapter extends ArrayAdapter<String> {

    private Context context;
    private int resource;
    private List<String> horariosList;

    public HorariosAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.horariosList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        String horario = horariosList.get(position);
        TextView textViewDia = convertView.findViewById(R.id.textViewDia);
        EditText editTextHorario = convertView.findViewById(R.id.editTextHorario);

        textViewDia.setText(horario);

        return convertView;
    }
}

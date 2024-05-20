package com.curso.aplicacionmapa;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.curso.aplicacionmapa.models.Avisos;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminConfiguracion extends AppCompatActivity {

    private EditText editTextAvisos;
    private ListView listViewHorarios;
    private Button buttonActualizar;

    private HorariosAdapter horariosAdapter;
    private List<String> horariosList;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_configuracion);

        db = FirebaseFirestore.getInstance();

        editTextAvisos = findViewById(R.id.editTextAvisos);
        listViewHorarios = findViewById(R.id.listViewHorarios);
        buttonActualizar = findViewById(R.id.buttonActualizar);

        // Inicializar la lista de horarios
        horariosList = new ArrayList<>();
        horariosList.add("Lunes: ");
        horariosList.add("Martes: ");
        horariosList.add("Miércoles: ");
        horariosList.add("Jueves: ");
        horariosList.add("Viernes: ");
        horariosList.add("Sábado: ");
        horariosList.add("Domingo: ");

        // Crear y establecer el adaptador
        horariosAdapter = new HorariosAdapter(this, R.layout.item_horario, horariosList);
        listViewHorarios.setAdapter(horariosAdapter);

        buttonActualizar.setOnClickListener(v -> actualizarInformacion());
    }

    private void actualizarInformacion() {
        String nuevosAvisos = editTextAvisos.getText().toString();

        // Actualizar los avisos y horarios en Firestore
        Map<String, String> horariosMap = new HashMap<>();
        for (int i = 0; i < horariosAdapter.getCount(); i++) {
            View view = listViewHorarios.getChildAt(i);
            EditText editTextHorario = view.findViewById(R.id.editTextHorario);
            String horario = editTextHorario.getText().toString();
            horariosMap.put(String.valueOf(i), horario);
        }

        Avisos avisos = new Avisos(nuevosAvisos, horariosMap);

        DocumentReference avisosRef = db.collection("avisos").document("configuracion_global");
        avisosRef
                .set(avisos)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminConfiguracion.this, "Avisos y horarios actualizados para todos los usuarios", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminConfiguracion.this, "Error al actualizar los avisos y horarios", Toast.LENGTH_SHORT).show();
                });
    }
}

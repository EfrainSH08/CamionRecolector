package com.curso.aplicacionmapa;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView textViewWelcomeMessage;

    private TextView textViewAvisos;
    private TextView textViewHorarios;
    private Button buttonLogout;
    private ImageView btnDashboard, btnMap, btnSettings;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListView listViewHorarios; // Declaración de listViewHorarios

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewWelcomeMessage = findViewById(R.id.textViewWelcomeMessage);
        buttonLogout = findViewById(R.id.buttonLogout);
        btnDashboard = findViewById(R.id.btnDashboard);
        btnMap = findViewById(R.id.btnMap);
        btnSettings = findViewById(R.id.btnSettings);

        textViewAvisos = findViewById(R.id.textViewAvisos);
        textViewHorarios = findViewById(R.id.textViewHorarios);

        // Inicialización de listViewHorarios
        listViewHorarios = findViewById(R.id.listViewHorarios);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("user")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            textViewWelcomeMessage.setText("Bienvenido, " + username);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Manejo de errores
                    });
        }


        buttonLogout.setOnClickListener(v -> showLogoutDialog());
        btnDashboard.setOnClickListener(v -> goToDashboard());
        btnMap.setOnClickListener(v -> goToMap());
        btnSettings.setOnClickListener(v -> goToSettings());

        fetchWelcome();

    }
    // En MainActivity.java
    private void fetchWelcome() {
        // Obtiene los avisos y horarios de recolección de basura desde Firestore
        db.collection("avisos")
                .document("configuracion_global")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String avisos = document.getString("avisos");
                            List<String> horariosList = new ArrayList<>();
                            Map<String, Object> horariosMap = (Map<String, Object>) document.get("horarios");
                            if (horariosMap != null) {
                                String[] diasSemana = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
                                for (int i = 0; i < diasSemana.length; i++) {
                                    String key = String.valueOf(i);
                                    String horario = horariosMap.containsKey(key) ? horariosMap.get(key).toString() : "";
                                    horariosList.add(diasSemana[i] + ": " + horario);
                                }
                            }

                            // Muestra los avisos y horarios en los TextView correspondientes
                            textViewAvisos.setText(avisos);
                            ArrayAdapter<String> horariosAdapter = new ArrayAdapter<>(this,
                                    android.R.layout.simple_list_item_1, horariosList);
                            listViewHorarios.setAdapter(horariosAdapter);
                            // Notifica al adaptador que los datos han cambiado
                            horariosAdapter.notifyDataSetChanged();

                        }
                    } else {
                        Log.d(TAG, "Error getting document: ", task.getException());
                    }
                });
    }



    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro que deseas cerrar sesión?")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void goToDashboard() {
        Intent intent = new Intent(MainActivity.this, AdminConfiguracion.class);
        startActivity(intent);
    }

    private void goToMap() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    private void goToSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }


}

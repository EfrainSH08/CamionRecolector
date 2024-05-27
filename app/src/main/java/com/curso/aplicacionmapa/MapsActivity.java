package com.curso.aplicacionmapa;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.curso.aplicacionmapa.models.TruckLocation;
import com.curso.aplicacionmapa.models.User;
import com.curso.aplicacionmapa.models.UserHomeLocation;
import com.curso.aplicacionmapa.models.UserLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedClient;
    private static final int REQUEST_CODE = 101;
    private FirebaseFirestore db;
    private Marker currentUserMarker;
    private String currentUserId;
    private Marker markerTruck;
    private Marker homeLocationMarker;
    private Map<LatLng, Marker> markerIdentifierMap = new HashMap<>(); // Cambiado a Map<LatLng, Marker>

    private ImageView btnDashboard, btnMap, btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        db = FirebaseFirestore.getInstance();
        currentUserId = getCurrentUserId();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationUpdates();

        markerTruck = null;

        btnDashboard = findViewById(R.id.btnDashboard);
        btnMap = findViewById(R.id.btnMap);
        btnSettings = findViewById(R.id.btnSettings);

        btnDashboard.setOnClickListener(v -> goToDashboard());
        btnMap.setOnClickListener(v -> goToMap());
        btnSettings.setOnClickListener(v -> goToSettings());
    }

    private void goToDashboard() {
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void goToMap() {
        // No es necesario iniciar otra instancia de MapsActivity
    }

    private void goToSettings() {
        Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        } else {
            return "unknown_user";
        }
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                updateMap(location);
                saveLocationToFirestore(location);
            }
        }
    };

    private void updateMap(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentUserMarker == null) {
            currentUserMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Mi ubicación actual"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        } else {
            currentUserMarker.setPosition(latLng);
        }
    }

    private void saveLocationToFirestore(Location location) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            User user = new User(currentUser.getUid(), currentUser.getDisplayName(), currentUser.getEmail(), null);
            UserLocation userLocation = new UserLocation(new GeoPoint(location.getLatitude(), location.getLongitude()), System.currentTimeMillis(), user);
            db.collection("user_locations").document(currentUserId).set(userLocation)
                    .addOnSuccessListener(aVoid -> {
                        // Éxito al guardar la ubicación
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MapsActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);

        // Agregar un icono en el mapa para agregar la ubicación de la casa
        mMap.setOnMapClickListener(latLng -> {
            if (homeLocationMarker != null) {
                homeLocationMarker.remove();
            }
            homeLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación de casa"));
            saveHomeLocationToFirestore(latLng);
        });

        // Cargar la ubicación de la casa del usuario si está disponible en Firestore
        loadHomeLocationFromFirestore();

        fetchTruckLocations();

    }


    private void fetchTruckLocations() {
        db.collection("truck_location").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(MapsActivity.this, "Error al obtener ubicaciones de camiones", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots != null) {
                for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                    snapshot.getReference().collection("camion").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot camionSnapshot : task.getResult().getDocuments()) {
                                TruckLocation truckLocation = camionSnapshot.toObject(TruckLocation.class);
                                if (truckLocation != null && truckLocation.getLocation() != null) {
                                    GeoPoint geoPoint = truckLocation.getLocation();
                                    LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    if (markerIdentifierMap.containsKey(latLng)) {
                                        Marker marker = markerIdentifierMap.get(latLng);
                                        marker.setPosition(latLng);
                                    } else {
                                        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Camión: " + camionSnapshot.getId());
                                        Marker marker = mMap.addMarker(markerOptions);
                                        markerIdentifierMap.put(latLng, marker); // Almacenar el marcador en el mapa
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "Error al obtener ubicaciones de camiones", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationUpdates();
            }
        }
    }

    private void saveHomeLocationToFirestore(LatLng latLng) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String email = currentUser.getEmail();

            // Obtener el nombre de usuario de Firestore
            db.collection("user").document(userId).get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (userSnapshot.exists()) {
                            String username = userSnapshot.getString("username");

                            UserHomeLocation userHomeLocation = new UserHomeLocation(latLng.latitude, latLng.longitude, userId, username, email);

                            db.collection("homes").document(userId).set(userHomeLocation)
                                    .addOnSuccessListener(aVoid -> {
                                        // Éxito al guardar la ubicación de la casa
                                        Log.d("Firestore", "Ubicación de la casa guardada: " + userHomeLocation);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(MapsActivity.this, "Error al guardar la ubicación de la casa", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MapsActivity.this, "Error al obtener el nombre de usuario", Toast.LENGTH_SHORT).show();
                    });
        }
    }





    private void loadHomeLocationFromFirestore() {
        // Cargar la ubicación de la casa del usuario desde Firestore
        db.collection("homes").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double latitude = documentSnapshot.getDouble("latitude");
                        double longitude = documentSnapshot.getDouble("longitude");
                        String username = documentSnapshot.getString("username"); // Obtener el username
                        LatLng latLng = new LatLng(latitude, longitude);
                        homeLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación de casa").snippet("Usuario: " + username));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MapsActivity.this, "Error al cargar la ubicación de la casa", Toast.LENGTH_SHORT).show();
                });
    }

}

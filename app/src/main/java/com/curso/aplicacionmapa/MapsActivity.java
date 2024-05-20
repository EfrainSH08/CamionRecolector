package com.curso.aplicacionmapa;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedClient;
    private static final int REQUEST_CODE = 101;
    private Map<LatLng, Marker> markerIdentifierMap = new HashMap<>(); // Cambiado a Map<LatLng, Marker>
    private FirebaseFirestore db;
    private Marker currentUserMarker;
    private String currentUserId;
    private boolean isZoomSet = false; // Bandera para controlar el zoom

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
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        } else {
            // Manejar el caso cuando no hay un usuario autenticado
            return "unknown_user";
        }
    }

    private void getLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // Intervalo de actualización en milisegundos
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
        currentUserMarker.setPosition(latLng);

        if (!isZoomSet) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            isZoomSet = true;
        }
    }

    private void saveLocationToFirestore(Location location) {
        UserLocation userLocation = new UserLocation(location.getLatitude(), location.getLongitude(), currentUserId);
        db.collection("user_locations").document(currentUserId).set(userLocation)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar la ubicación
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MapsActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchOtherUsersLocations() {
        db.collection("user_locations").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(MapsActivity.this, "Error al obtener ubicaciones de usuarios", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots != null) {
                for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
                    UserLocation userLocation = snapshot.toObject(UserLocation.class);
                    if (userLocation != null) {
                        String userId = userLocation.getUserId();
                        if (userId != null && !userId.equals(currentUserId)) {
                            Double latitude = userLocation.getLatitude();
                            Double longitude = userLocation.getLongitude();
                            if (latitude != null && longitude != null) {
                                LatLng latLng = new LatLng(latitude, longitude);
                                if (markerIdentifierMap.containsKey(latLng)) {
                                    Marker marker = markerIdentifierMap.get(latLng);
                                    marker.setPosition(latLng);
                                } else {
                                    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Usuario: " + userId);
                                    Marker marker = mMap.addMarker(markerOptions);
                                    markerIdentifierMap.put(latLng, marker); // Almacenar el marcador en el mapa
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        currentUserMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Mi ubicación actual"));
        fetchOtherUsersLocations();
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
}

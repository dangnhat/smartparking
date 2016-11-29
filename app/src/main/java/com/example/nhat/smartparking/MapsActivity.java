package com.example.nhat.smartparking;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static boolean is_location_permission_granted = false;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Check permission to get current location
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Log.e("Main", "User denied to permission request of ACCESS_FINE_LOCATION");
                /* TODO: do somethings when User denies request */

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                Log.d("Main", "Requested ACCESS_FINE_LOCATION");
            }
        }
        else {
            is_location_permission_granted = true;
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (is_location_permission_granted != true) {
            Log.d("Main", "is_location_permission_granted = false, return");
            return;
        }

        // Turn on current location
        mMap.setMyLocationEnabled(true);

        // Add a marker at KAIST
        LatLng kaist = new LatLng(36.368203, 127.363764);
        mMap.addMarker(new MarkerOptions().position(kaist).title("KAIST"));

        // Add a marker at some parking lots
        LatLng park1 = new LatLng(36.340645, 127.391122);
        mMap.addMarker(new MarkerOptions().position(park1).title("Parking Lot #1"));

        LatLng park2 = new LatLng(36.34, 127.37);
        mMap.addMarker(new MarkerOptions().position(park2).title("Parking Lot #2"));

        LatLng park3 = new LatLng(36.35, 127.36);
        mMap.addMarker(new MarkerOptions().position(park3).title("Parking Lot #3"));

        /* Get current location */
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted.
                    is_location_permission_granted = true;
                    Log.d("Main", "Permission granted");
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}



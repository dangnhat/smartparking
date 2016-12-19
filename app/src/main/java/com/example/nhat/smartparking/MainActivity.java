package com.example.nhat.smartparking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.xbill.DNS.*;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location current_location;

    // EPCIS and ONS
    EditText etResponse;
    EditText ResultText;

    // Array of airport names
    private String[] airport_names = {
            "Incheon International Airport",
            "Gimpo International Airport",
            "Gimhae International Airport",
            "Jeju International Airport",
            "Daegu International Airport",
            "Ulsan International Airport",
            "Cheongju International Airport",
            "Yangyang International Airport",
            "Muan International Airport",
            "Gwangju International Airport",
            "Yeosu International Airport",
            "Sichuan International Airport",
            "Pohang International Airport",
            "Gunsan International Airport",
            "Wonju International Airport",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Hello world!
        Log.d("Main", "onCreate: Hello world");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        Log.d("Main", "onCreate: Create an instance of GoogleAPIClient");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Check permission to get current location
        Log.d("Main", "onCreate: Check location permission");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Main", "onCreate: We don't have location permission, requesting...");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Log.d("Main", "User denied to permission request of ACCESS_FINE_LOCATION");
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
            Log.d("Main", "OnCreate: we have already have location permission");
        }
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

        // Debug
        Log.d("Main", "onMapReady");

        // Check permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Main", "onMapReady: don't have permission to get location data, return");

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

        Log.d("Main", "onMapReady: get current location");
        /* Get current location */
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        Location current_location = locationManager.getLastKnownLocation(provider);

        if(current_location != null) {
            Log.d("Main", "onMapReady: Current Location:" + String.valueOf(current_location.getLatitude())
                    + ", " + String.valueOf(current_location.getLongitude()));
            LatLng currentLatLng = new LatLng(current_location.getLatitude(),
                    current_location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14));
        }
        else {
            Log.d("Main", "onMapReady: Can't get current location");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted.
                    Log.d("Main", "onRequestPermissionsResult: Permission granted");
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

    // Button1 click
    public void toNearestAirport(View view) throws IOException {
        Log.d("Main", "toNearestAirport: button1 clicked");

        // Find the nearest airport
        double min_distance = -1, distance;
        Geocoder mGeocode = new Geocoder(this);
        Location nearest_airport_location = null;

        for (String airport_name : airport_names) {
            // Find distance to each each airport
            Log.d("Main", "toNearestAirport: Get coordinates of " + airport_name);
            List<Address> addresses = mGeocode.getFromLocationName(airport_name, 1);
            if (addresses.size() > 0) {
                Log.d("Main", "toNearestAirport: coordinates:" + String.valueOf(addresses.get(0).getLatitude())
                        + ", " + String.valueOf(addresses.get(0).getLongitude()));

                Location airport_location = new Location(airport_name);
                airport_location.setLatitude((addresses.get(0).getLatitude()));
                airport_location.setLongitude((addresses.get(0).getLongitude()));

                // Get distance for current location
                distance = current_location.distanceTo(airport_location);
                Log.d("Main", "toNearestAirport: current distance: " + String.valueOf(distance));

                // Compare with min_distance
                if (min_distance < 0) {
                    min_distance = distance;
                    nearest_airport_location = airport_location;

                    Log.d("Main", "toNearestAirport: min_distance updated: " + String.valueOf(distance)
                    + "(m) airport: " + nearest_airport_location.getProvider());
                }
                else{
                    if (distance < min_distance) {
                        min_distance = distance;
                        nearest_airport_location = airport_location;

                        Log.d("Main", "toNearestAirport: min_distance updated: " + String.valueOf(distance)
                                + "(m) airport: " + nearest_airport_location.getProvider());
                    }
                    else {
                        Log.d("Main", "toNearestAirport: distance >= min_distance, do nothing");
                    }
                }
            }
        }

        // Add marker and animate camera
        if (nearest_airport_location != null) {
            LatLng nearest_airport_coor = new LatLng(nearest_airport_location.getLatitude(),
                    nearest_airport_location.getLongitude());

            mMap.addMarker(new MarkerOptions().position(nearest_airport_coor).title(nearest_airport_location.getProvider()));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearest_airport_coor, 14));
        }
        else {
            Log.d("Main", "nearest_airport_location = null");
        }

        // Connect to ONS server

    }

    // Google API Callbacks.
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("Main", "onConnectionFailed");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("Main", "onConnected");

        /* Get current location */
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Main", "onConnected: don't have permission to get location data, return");

            return;
        }

        current_location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (current_location != null) {
            Log.d("Main", "onConnected: Current Location:" + String.valueOf(current_location.getLatitude())
                    + ", " + String.valueOf(current_location.getLongitude()));
        }
        else {
            Log.d("Main", "onConnected: Can't get current location");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Main", "onConnectionSuspended");
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

    // EPCIS and ONS
    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
        //////////////////// ONS!!!
            String RESOLVER_ADDRESS = "110.76.91.123";
            //String DN = "1.1.1.1.1.1.1.1.1.1.1.1.1.gln.gs1.id.onsepc.kr";
            String DN = "1.8.0.0.0.0.1.2.3.4.5.8.8.gtin.gs1.id.onsepc.kr";
            try {
                Lookup.setDefaultResolver(new SimpleResolver(RESOLVER_ADDRESS));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            try {
                System.out.println("[RYUN] request : " + DN);
                Lookup lookup = new Lookup(DN, Type.NAPTR);
                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    for (Record record : records) {
                        NAPTRRecord naptrRecord = (NAPTRRecord) record;

                        String result = naptrRecord.toString();
                        System.out.println("[RYUN] result: " + result);
                    }
                }
            } catch (Exception ignored) {

            }
            ////////////////////
            return GET(urls[0]); ///// for EPCIS
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            etResponse.setText(result);
            ResultText.setText(result);
        }
    }
}



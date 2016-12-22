package com.example.nhat.smartparking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.BubbleIconFactory;
import com.google.maps.android.ui.IconGenerator;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location current_location;

    // GLNs list
    public class GLN_list{
        public String gln;
        public String name_of_parking_lot;
        public String latitude;
        public String longitude;
    }

    ArrayList<GLN_list> GLNsList = null;

    // EPCIS urls list
    ArrayList<String> EPCISUrls = null;

    // Smart search URL
    String SmartSearchURL = "http://143.248.53.173:10023/epcis/Service/Poll/SimpleEventQuery?";

    // ONS
    String ONSAddress = "110.76.91.123";

    // Array of airport names
    private String[] airport_names = {
            "Gimpo International Airport", // 0
            "Gimhae International Airport", // 1
            "Jeju International Airport", // 2
            "Daegu International Airport", // 3
            "Ulsan International Airport", // 4
            "Gwangju International Airport", // 5
            "Yeosu International Airport", // 6
            "Gunsan International Airport", // 7
            "Wonju International Airport", // 8
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

    // Add Bubble icon on the map
    private void addIcon(GoogleMap map, IconGenerator iconFactory, CharSequence text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        map.addMarker(markerOptions);
    }


    // Button1 click
    public void findParkingLots(View view) throws IOException {
        Log.d("Main", "findParkingLots: button1 clicked");
        IconGenerator iconFactory = new IconGenerator(this);

        // Get current view coordinates

        // Get GLNs from current view coordinates
        GetGLNsFromSmartSearch getGLNsTask = new GetGLNsFromSmartSearch();
        getGLNsTask.execute(SmartSearchURL);
        try {
            Log.d("Main", "findParkingLots: waiting for GetGLNsFromSmartSearch to finish");
            GLNsList = getGLNsTask.get();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        /* Show on map */
        for (GLN_list glnTemp : GLNsList) {
            LatLng coor = new LatLng(Double.parseDouble(glnTemp.latitude),
                    Double.parseDouble(glnTemp.longitude));

            iconFactory.setStyle(IconGenerator.STYLE_GREEN);
            addIcon(mMap, iconFactory, glnTemp.name_of_parking_lot + ", ?/?", coor);

            Log.d("Main", "findParkingLots: added marker of " + glnTemp.gln + " at " +
                    glnTemp.latitude + ", " + glnTemp.longitude);
        }

        /* Use GLNs to get EPCIS server urls */
        for (GLN_list glnTemp : GLNsList) {
            GetEPCISURLFromONS getEPCISURLTask = new GetEPCISURLFromONS();
            getEPCISURLTask.execute(ONSAddress, glnTemp.gln);
            try {
                String epcisUrl = getEPCISURLTask.get();
                EPCISUrls.add(epcisUrl);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

//        // Find the nearest airport
//        double min_distance = -1, distance;
//        Geocoder mGeocode = new Geocoder(this);
//        Location nearest_airport_location = null;
//
//        for (String airport_name : airport_names) {
//            // Find distance to each each airport
//            Log.d("Main", "findParkingLots: Get coordinates of " + airport_name);
//            List<Address> addresses = mGeocode.getFromLocationName(airport_name, 1);
//            if (addresses.size() > 0) {
//                Log.d("Main", "findParkingLots: coordinates:" + String.valueOf(addresses.get(0).getLatitude())
//                        + ", " + String.valueOf(addresses.get(0).getLongitude()));
//
//                Location airport_location = new Location(airport_name);
//                airport_location.setLatitude((addresses.get(0).getLatitude()));
//                airport_location.setLongitude((addresses.get(0).getLongitude()));
//
//                // Get distance for current location
//                distance = current_location.distanceTo(airport_location);
//                Log.d("Main", "findParkingLots: current distance: " + String.valueOf(distance));
//
//                // Compare with min_distance
//                if (min_distance < 0) {
//                    min_distance = distance;
//                    nearest_airport_location = airport_location;
//
//                    Log.d("Main", "findParkingLots: min_distance updated: " + String.valueOf(distance)
//                    + "(m) airport: " + nearest_airport_location.getProvider());
//                }
//                else{
//                    if (distance < min_distance) {
//                        min_distance = distance;
//                        nearest_airport_location = airport_location;
//
//                        Log.d("Main", "findParkingLots: min_distance updated: " + String.valueOf(distance)
//                                + "(m) airport: " + nearest_airport_location.getProvider());
//                    }
//                    else {
//                        Log.d("Main", "findParkingLots: distance >= min_distance, do nothing");
//                    }
//                }
//            }
//        }
//
//        // Add marker and animate camera
//        if (nearest_airport_location != null) {
//            LatLng nearest_airport_coor = new LatLng(nearest_airport_location.getLatitude(),
//                    nearest_airport_location.getLongitude());
//
//            mMap.addMarker(new MarkerOptions().position(nearest_airport_coor).title(nearest_airport_location.getProvider()));
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(nearest_airport_coor, 14));
//
//            // Get current boundaries
//            LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
//            Log.d("Main", "findParkingLots: current boundaries NE: " + curScreen.northeast + " SW: "
//            + curScreen.southwest);
//
//        }
//        else {
//            Log.d("Main", "findParkingLots: nearest_airport_location = null");
//        }
//
//        // Assume that we already has GLNs, connect to ONS server to get url to EPICS
//
//        // Test some FQDNs
//        String ONSAddress = "110.76.91.123";
//        String FQDNs;
//
//        FQDNs = "2.1.1.1.1.1.1.1.1.1.1.1.1.gln.gs1.id.onsepc.kr";
//        new GetEPCISURLFromONS().execute(ONSAddress, FQDNs);
//
//        FQDNs = "1.1.1.1.1.1.1.1.1.1.1.1.1.gln.gs1.id.onsepc.kr";
//        new GetEPCISURLFromONS().execute(ONSAddress, FQDNs);
//
//        FQDNs = "1.8.0.0.0.0.1.2.3.4.5.8.8.gtin.gs1.id.onsepc.kr";
//        new GetEPCISURLFromONS().execute(ONSAddress, FQDNs);
    }

    /********************************* Google API Callbacks. **************************************/
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

    /************************** EPCIS and ONS *****************************************************/
    public static String GET(String url){
        InputStream inputStream = null;
        String result = null;
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert input stream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            }
            else {
                Log.d("Main", "GET: inpuStream == null.");
            }

        } catch (Exception e) {
            Log.d("Main", e.getLocalizedMessage());
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

    private static String GLNToFQDN (String gln) {
        // TODO Auto-generated method stub
        String retFQDN = "";
        char[] remainCA;

        remainCA = gln.toCharArray();
        for( int i = remainCA.length-1 ; i >= 0 ; i--)
        {
            if (remainCA[i] == '.') {
                continue;
            }

            retFQDN += remainCA[i]+".";
        }

        retFQDN += "gln.gs1.id.onsepc.kr";

        return retFQDN;
    }

    /* Get list of GLNs from Smart Search server */
    private class GetGLNsFromSmartSearch extends AsyncTask<String, Void, ArrayList<GLN_list>> {
        @Override
        protected ArrayList<GLN_list> doInBackground(String... urls) {
            String result=GET(urls[0]);
            ArrayList<GLN_list> glnLists;
            glnLists=PullParserFromXML(result);

            Log.d("Main", "GetGLNsFromSmartSearch: " + String.valueOf(glnLists.size()));

            return glnLists;
        }

        public ArrayList<GLN_list> PullParserFromXML(String data){
            ArrayList<GLN_list> xml=new ArrayList<GLN_list>();
            GLN_list dummy=new GLN_list();
            boolean is_gln=false;
            boolean is_latitude=false;
            boolean is_longitude=false;
            boolean is_address=false;
            Log.d("Main","PullParserFromXML: start parsing");
            try
            {
                XmlPullParserFactory factory=XmlPullParserFactory.newInstance();
                XmlPullParser parser=factory.newPullParser();
                String stag;

                parser.setInput(new StringReader(data));
                int eventtype=parser.getEventType();

                while(eventtype!=XmlPullParser.END_DOCUMENT)
                {
                    switch(eventtype)
                    {
                        case XmlPullParser.START_DOCUMENT:

                            break;
                        case XmlPullParser.END_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            stag=parser.getName();
                            if(stag.equals("id"))
                                is_gln=true;
                            else if(stag.equals("parkingspace:gps_latitude"))
                                is_latitude=true;
                            else if(stag.equals("parkingspace:gps_longitude"))
                                is_longitude=true;
                            else if(stag.equals("parkingspace:parkingspace_name"))
                                is_address=true;
                            break;
                        case XmlPullParser.TEXT:
                            if(is_gln)
                            {
                                dummy=new GLN_list();
                                dummy.gln=parser.getText();
                                is_gln=false;
                            }
                            if(is_latitude)
                            {
                                dummy.latitude=parser.getText();
                                is_latitude=false;
                            }
                            if(is_longitude)
                            {
                                dummy.longitude=parser.getText();
                                is_longitude=false;
                            }
                            if(is_address) {
                                Log.i("address_test",parser.getText());
                                dummy.name_of_parking_lot = parser.getText();
                                xml.add(dummy);
                                is_address = false;
                                dummy=null;
                            }
                    }
                    eventtype=parser.next();
                    if(xml!=null)
                        Log.i("test",String.valueOf(xml.size()));
                }
            }catch(Exception ex)
            {
                ex.printStackTrace();
            }
            Log.d("Main", "PullParserFromXML: xml size " + String.valueOf(xml.size()));
            return xml;
        }
    }

    /* Get ONS entries */
    private class GetEPCISURLFromONS extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = null;
            String ONSRecord = null;

            //////////////////// ONS!!!
            String ONSAddress = urls[0];
            String GLN = urls[1];
            String QFDN;

            /* Convert GLN to QFDN */
            if (GLN.contains("urn:epc:id:sgln:")) {
                GLN = GLN.substring(GLN.indexOf("urn:epc:id:sgln:") + "urn:epc:id:sgln:".length());
            }

            QFDN = GLNToFQDN(GLN);
            Log.d("Main", "GetEPCISURLFromONS: converted " + GLN + " to " + QFDN);

            try {
                Lookup.setDefaultResolver(new SimpleResolver(ONSAddress));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            try {
                Log.d("Main", "GetEPCISURLFromONS: request " + QFDN + " to " + ONSAddress);
                Lookup lookup = new Lookup(QFDN, Type.NAPTR);
                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    for (Record record : records) {
                        NAPTRRecord naptrRecord = (NAPTRRecord) record;

                        ONSRecord = naptrRecord.toString();
                        Log.d("Main", "GetEPCISURLFromONS: result for " + QFDN + " : " + ONSRecord);
                    }
                }
            } catch (Exception e) {
                Log.d("Main", "GetEPCISURLFromONS: " + e);
            }

            /* Parse NAPTR to get EPICS url */
            if (ONSRecord == null) {
                Log.d("Main", "GetEPCISURLFromONS: ONSRecord is null");
                return null;
            }

            result = ONSRecord.substring(ONSRecord.indexOf("!^.*$!") + "!^.*$!".length(), ONSRecord.lastIndexOf('!'));
            Log.d("Main", "GetEPCISURLFromONS: epcis url: " + result);

            return result;
        }
    }
}



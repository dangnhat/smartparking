package com.example.nhat.smartparking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.SimpleResolver;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.view.View;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location current_location;

    // Parking lot info format
    public class ParkingLotInfo {
        public String gln;
        public String name_of_parking_lot;
        public String latitude;
        public String longitude;
        public String EPCISServer;
        public String address;
        public int maxCapacity;
        public int availableSpaces;
    }

    // Master data format
    public class MasterData {
        public String gln;
        public String parking_name;
        public String address;
        public int maxCapacity;
    }

    // Event data format
    public class EventData {
        public String gln;
        public int availableSpaces;
    }

    ArrayList<ParkingLotInfo> ParkingLotInfoList = null;

    // Updating period
    private int UpdatingPeriod = 2*60*1000; // ms

    // Smart search URL
    String SmartSearchURL = "http://143.248.53.173:10023/epcis/Service/Poll/SimpleEventQuery?&orderBy=recordTime&orderDirection=DESC&";

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

        // Get current view coordinates
        LatLngBounds currentBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.d("Main", "findParkingLots: " + currentBounds);

        String smartSearchUrlWBounds = SmartSearchURL +
                "LT_http://www.tta.or.kr/epcis/schema/parkingspace.xsd%23gps_latitude=" +
                currentBounds.northeast.latitude + "&" +
                "GE_http://www.tta.or.kr/epcis/schema/parkingspace.xsd%23gps_latitude=" +
                currentBounds.southwest.latitude + "&" +
                "LT_http://www.tta.or.kr/epcis/schema/parkingspace.xsd%23gps_longitude=" +
                currentBounds.northeast.longitude + "&" +
                "GE_http://www.tta.or.kr/epcis/schema/parkingspace.xsd%23gps_longitude=" +
                currentBounds.southwest.longitude + "&";
        Log.d("Main", "smartSearchUrlWBounds: " + smartSearchUrlWBounds);

        // Get GLNs from current view coordinates
        GetGLNsFromSmartSearch getGLNsTask = new GetGLNsFromSmartSearch();
        getGLNsTask.execute(smartSearchUrlWBounds);
        try {
            Log.d("Main", "findParkingLots: waiting for GetGLNsFromSmartSearch to finish");
            ParkingLotInfoList = getGLNsTask.get();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        /* Use GLNs to get EPCIS server urls */
        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            GetEPCISURLFromONS getEPCISURLTask = new GetEPCISURLFromONS();
            getEPCISURLTask.execute(ONSAddress, parkingLotInfoTemp.gln);
            try {
                parkingLotInfoTemp.EPCISServer = getEPCISURLTask.get();
                Log.d("Main", "findParkingLots: got EPCIS URL for " + parkingLotInfoTemp.gln + ": "
                        + parkingLotInfoTemp.EPCISServer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        /* Test while ONS is not available */
//        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
//            parkingLotInfoTemp.EPCISServer = "http://143.248.53.173:10022";
//        }

        /* Get Master and Event data */
        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {

            /* Master data */
            String MasterDataUrl = parkingLotInfoTemp.EPCISServer
                    + "/epcis/Service/Poll/SimpleMasterDataQuery?includeAttributes=true&includeChildren=true&vocabularyName=urn:epcglobal:epcis:vtype:ParkingSpace&EQ_name="
                    + parkingLotInfoTemp.gln + "&";

            GetMasterData getMasterDataTask = new GetMasterData();
            getMasterDataTask.execute(MasterDataUrl);

            Log.d("Main", "findParkingLots: getting master data with url: " + MasterDataUrl);

            try {
                MasterData masterData = getMasterDataTask.get();

                parkingLotInfoTemp.address = masterData.address;
                parkingLotInfoTemp.maxCapacity = masterData.maxCapacity;
                Log.d("Main", "findParkingLots: address: " + parkingLotInfoTemp.address);
                Log.d("Main", "findParkingLots: maxCapacity: " + Integer.toString(parkingLotInfoTemp.maxCapacity));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            /* Event data */
            String EventDataUrl = parkingLotInfoTemp.EPCISServer
                    + "/epcis/Service/Poll/SimpleEventQuery?EQ_bizLocation="
                    + parkingLotInfoTemp.gln
                    + "&orderBy=eventTime&orderDirection=DESC&eventCountLimit=1&";
            GetEventData getEventDataTask = new GetEventData();
            getEventDataTask.execute(EventDataUrl);

            Log.d("Main", "findParkingLots: getting event data with url: " + EventDataUrl);

            try {
                EventData eventData = getEventDataTask.get();

                parkingLotInfoTemp.availableSpaces = eventData.availableSpaces;
                Log.d("Main", "findParkingLots: availableSpaces: " + parkingLotInfoTemp.availableSpaces);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

        /* Show on map */
        /* find the nearest parking lot with free spaces */
        String nearestParkingLotGLN = null;
        double min_distance = -1;

        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            if (parkingLotInfoTemp.availableSpaces < 0) {
                continue;
            }

            Location loc = new Location(parkingLotInfoTemp.gln);
            loc.setLatitude(Double.parseDouble(parkingLotInfoTemp.latitude));
            loc.setLongitude(Double.parseDouble(parkingLotInfoTemp.longitude));

            double distance = current_location.distanceTo(loc);
            Log.d ("Main", "findParkingLots: distance to " + loc.getProvider() + " : " + String.valueOf(distance));
            if (min_distance < 0 || distance < min_distance)  {
                min_distance = distance;
                nearestParkingLotGLN = parkingLotInfoTemp.gln;

                Log.d("Main", "findParkingLots: min_distance updated: " + String.valueOf(distance)
                        + "(m) gln: " + nearestParkingLotGLN);
            }
            else {
                Log.d("Main", "findParkingLots: distance >= min_distance, do nothing");
            }
        }

        Log.d("Main", "findParkingLots: nearest gln: " + nearestParkingLotGLN);

        IconGenerator iconFactory = new IconGenerator(this);
        mMap.clear();
        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            LatLng coor = new LatLng(Double.parseDouble(parkingLotInfoTemp.latitude),
                    Double.parseDouble(parkingLotInfoTemp.longitude));

            if (parkingLotInfoTemp.gln == nearestParkingLotGLN) {
                iconFactory.setStyle(IconGenerator.STYLE_BLUE);
            }
            else if (parkingLotInfoTemp.availableSpaces > 0) {
                iconFactory.setStyle(IconGenerator.STYLE_GREEN);
            }
            else {
                iconFactory.setStyle(IconGenerator.STYLE_RED);
            }
            addIcon(mMap, iconFactory, parkingLotInfoTemp.name_of_parking_lot + ", "
                    + Integer.toString(parkingLotInfoTemp.availableSpaces) + "/"
                    + Integer.toString(parkingLotInfoTemp.maxCapacity), coor);

            Log.d("Main", "findParkingLots: added marker of " + parkingLotInfoTemp.gln + " at " +
                    parkingLotInfoTemp.latitude + ", " + parkingLotInfoTemp.longitude);
        }

        /* Enable updating parking lots info */
        Log.d("Main", "findParkingLots: post timerRunnable for " + UpdatingPeriod/1000 + "s");
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, UpdatingPeriod);
    }

    /********************************* Periodically update parking lots info  *********************/
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            Log.d("Main", "timerRunnable: update parking lots info");
            updateParkingLotInfo();

            Log.d("Main", "timerRunnable: post timerRunnable for " + UpdatingPeriod/1000 + "s");
            timerHandler.postDelayed(this, UpdatingPeriod);
        }
    };

    public void updateParkingLotInfo () {
        /* Get Event data */
        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            /* Event data */
            String EventDataUrl = parkingLotInfoTemp.EPCISServer
                    + "/epcis/Service/Poll/SimpleEventQuery?EQ_bizLocation="
                    + parkingLotInfoTemp.gln
                    + "&orderBy=eventTime&orderDirection=DESC&eventCountLimit=1&";
            GetEventData getEventDataTask = new GetEventData();
            getEventDataTask.execute(EventDataUrl);

            Log.d("Main", "findParkingLots: getting event data with url: " + EventDataUrl);

            try {
                EventData eventData = getEventDataTask.get();

                parkingLotInfoTemp.availableSpaces = eventData.availableSpaces;
                Log.d("Main", "findParkingLots: availableSpaces: " + parkingLotInfoTemp.availableSpaces);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

        /* Show on map */
        /* find the nearest parking lot with free spaces */
        String nearestParkingLotGLN = null;
        double min_distance = -1;

        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            if (parkingLotInfoTemp.availableSpaces < 0) {
                continue;
            }

            Location loc = new Location(parkingLotInfoTemp.gln);
            loc.setLatitude(Double.parseDouble(parkingLotInfoTemp.latitude));
            loc.setLongitude(Double.parseDouble(parkingLotInfoTemp.longitude));

            double distance = current_location.distanceTo(loc);
            Log.d ("Main", "findParkingLots: distance to " + loc.getProvider() + " : " + String.valueOf(distance));
            if (min_distance < 0 || distance < min_distance)  {
                min_distance = distance;
                nearestParkingLotGLN = parkingLotInfoTemp.gln;

                Log.d("Main", "findParkingLots: min_distance updated: " + String.valueOf(distance)
                        + "(m) gln: " + nearestParkingLotGLN);
            }
            else {
                Log.d("Main", "findParkingLots: distance >= min_distance, do nothing");
            }
        }

        Log.d("Main", "findParkingLots: nearest gln: " + nearestParkingLotGLN);

        IconGenerator iconFactory = new IconGenerator(this);
        mMap.clear();

        for (ParkingLotInfo parkingLotInfoTemp : ParkingLotInfoList) {
            LatLng coor = new LatLng(Double.parseDouble(parkingLotInfoTemp.latitude),
                    Double.parseDouble(parkingLotInfoTemp.longitude));

            if (parkingLotInfoTemp.gln == nearestParkingLotGLN) {
                iconFactory.setStyle(IconGenerator.STYLE_BLUE);
            }
            else if (parkingLotInfoTemp.availableSpaces > 0) {
                iconFactory.setStyle(IconGenerator.STYLE_GREEN);
            }
            else {
                iconFactory.setStyle(IconGenerator.STYLE_RED);
            }
            addIcon(mMap, iconFactory, parkingLotInfoTemp.name_of_parking_lot + ", "
                    + Integer.toString(parkingLotInfoTemp.availableSpaces), coor);

            Log.d("Main", "findParkingLots: added marker of " + parkingLotInfoTemp.gln + " at " +
                    parkingLotInfoTemp.latitude + ", " + parkingLotInfoTemp.longitude);
        }
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
    private class GetGLNsFromSmartSearch extends AsyncTask<String, Void, ArrayList<ParkingLotInfo>> {
        @Override
        protected ArrayList<ParkingLotInfo> doInBackground(String... urls) {
            String result=GET(urls[0]);
            ArrayList<ParkingLotInfo> glnLists;
            glnLists=ParseGLNsFromXML(result);

            Log.d("Main", "GetGLNsFromSmartSearch: " + String.valueOf(glnLists.size()));

            return glnLists;
        }

        public boolean traverse_array(String gln, ArrayList<String> list)
        {
            int i = 0;
            for (i = 0; i<list.size(); i++)
            {
                if (gln.equals(list.get(i)))
                    return true;
            }
            return false;
        }

        public ArrayList<ParkingLotInfo> ParseGLNsFromXML(String data){
            ArrayList<ParkingLotInfo> xml=new ArrayList<ParkingLotInfo>();
            ArrayList<String> delete_list=new ArrayList<String>();
            ParkingLotInfo dummy=new ParkingLotInfo();
            boolean is_gln=false;
            boolean is_latitude=false;
            boolean is_longitude=false;
            boolean is_address=false;
            boolean is_action=false;
            boolean is_delete_gln=false;
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
                            else if(stag.equals("parkingspace:gps_latitude") && !is_delete_gln)
                                is_latitude=true;
                            else if(stag.equals("parkingspace:gps_longitude") && !is_delete_gln)
                                is_longitude=true;
                            else if(stag.equals("parkingspace:parkingspace_name") && !is_delete_gln)
                                is_address=true;
                            else if(stag.equals("action"))
                                is_action=true;
                            break;
                        case XmlPullParser.TEXT:
                            if(is_delete_gln && is_gln)
                            {
                                //if it is in delete_list and it is gln
                                Log.i("tester",parser.getText());
                                delete_list.add(parser.getText());
                                //add to the delete list
                                is_gln=false;
                            }
                            else if(is_action)
                            {
                                Log.i("event",parser.getText());
                                if(parser.getText().equals("ADD"))
                                    is_delete_gln=false;
                                if(parser.getText().equals("DELETE"))
                                    is_delete_gln=true;
                                is_action=false;
                            }
                            else if(is_gln)
                            {
                                if(traverse_array(parser.getText(),delete_list))
                                    is_delete_gln=true;
                                else {
                                    is_delete_gln=false;
                                    dummy = new ParkingLotInfo();
                                    dummy.gln = parser.getText();
                                }
                                is_gln = false;
                            }
                            else if(is_latitude)
                            {
                                dummy.latitude=parser.getText();
                                is_latitude=false;
                            }
                            else if(is_longitude)
                            {
                                dummy.longitude=parser.getText();
                                is_longitude=false;
                            }
                            else if(is_address) {
                                //Log.d("Main",parser.getText());
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
            Log.d("GetEPCISURLFromONS", "converted " + GLN + " to " + QFDN);

            try {
                Lookup.setDefaultResolver(new SimpleResolver(ONSAddress));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            try {
                Log.d("GetEPCISURLFromONS", "request " + QFDN + " to " + ONSAddress);
                Lookup lookup = new Lookup(QFDN, Type.NAPTR);
                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    for (Record record : records) {
                        NAPTRRecord naptrRecord = (NAPTRRecord) record;

                        ONSRecord = naptrRecord.toString();
                        Log.d("GetEPCISURLFromONS", "result for " + QFDN + " : " + ONSRecord);

                        /* check the service file in NAPTR recore */
                        result = ONSRecord.substring(ONSRecord.indexOf("\"")+1, ONSRecord.indexOf("\"")+2);
                        Log.d("GetEPCISURLFromONS", "RYUN: " + result);
                        if (result.equals("u") || result.equals("U") ) {
                            result = ONSRecord.substring(ONSRecord.indexOf("www"), ONSRecord.indexOf("!^.*$!")-3);
                            Log.d("GetEPCISURLFromONS", "RYUN: " + result);
                            if (result.equals("www.parking-space-finder.org/freespace"))
                                break;
                        }

                        /////////
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Parse NAPTR to get EPICS url */
            if (ONSRecord == null) {
                Log.d("GetEPCISURLFromONS", "ONSRecord is null");
                return null;
            }

            result = ONSRecord.substring(ONSRecord.indexOf("!^.*$!") + "!^.*$!".length(), ONSRecord.lastIndexOf('!'));
            Log.d("GetEPCISURLFromONS", "epcis url: " + result);

            return result;
        }
    }

    // Get Master data
    private class GetMasterData extends AsyncTask<String, Integer, MasterData> {

        @Override
        protected MasterData doInBackground(String... urls) {
            String result = GET(urls[0]);
            MasterData masterData = parseMasterDataFromXML(result);
            return masterData;
        }

        private MasterData parseMasterDataFromXML(String data) {
            ArrayList<MasterData> xml = new ArrayList<MasterData>();
            boolean is_parkingname = false;
            boolean is_addressname = false;
            boolean is_max=false;
            MasterData dummy = new MasterData();
            MasterData result = new MasterData();
            String attribute;

            Log.d("GetMasterData", "ParseMasterData starts");
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                String stag;

                parser.setInput(new StringReader(data));

                int eventtype = parser.getEventType();

                Log.d("GetMasterData", String.valueOf(eventtype));
                while (eventtype != XmlPullParser.END_DOCUMENT) {
                    switch (eventtype) {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            stag = parser.getName();
                            Log.d("GetMasterData", stag);
                            if (stag.equals("VocabularyElement")) {
                                dummy = new MasterData();
                                dummy.gln = parser.getAttributeValue(null, "id");
                                Log.d("GetMasterData", dummy.gln);
                            }
                            if (stag.equals("attribute")) {
                                attribute = parser.getAttributeValue(null, "id");
                                if (attribute.equals("http://epcis.example.com/airport/name"))
                                    is_parkingname = true;
                                else if (attribute.equals("http://epcis.example.com/airport/address"))
                                    is_addressname = true;
                                else if(attribute.equals("http://epcis.example.com/airport/max_capacity"))
                                    is_max=true;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            Log.d("GetMasterData", parser.getText());
                            if (is_parkingname) {
                                dummy.parking_name = parser.getText();
                                Log.d("GetMasterData", parser.getText());
                                is_parkingname = false;
                            }
                            if (is_addressname) {
                                dummy.address = parser.getText();
                                Log.d("GetMasterData", parser.getText());
                                is_addressname = false;
                            }
                            if(is_max) {
                                Log.d("GetMasterData", "RYUN "+parser.getText());
                                dummy.maxCapacity=Integer.parseInt(parser.getText());
                                xml.add(dummy);
                                is_max=false;
                                dummy=null;
                            }
                    }
                    eventtype = parser.next();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Log.d("GetMasterData", String.valueOf(xml.size()));

            if (xml.size() != 0) {
                result = xml.get(0);
            }

            return result;
        }
    }

    // Get Event data
    private class GetEventData extends AsyncTask<String, Void, EventData> {

        @Override
        protected EventData doInBackground(String... urls) {
            String result = GET(urls[0]);
            return parseEventDataFromXML(result);
        }

        private EventData parseEventDataFromXML(String data) {

            EventData eventData = new EventData();
            boolean is_gln = false;
            boolean is_smartparking = false;
            Log.d("GetEventData", "parseEventDataFromXML: start");

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                String stag;

                parser.setInput(new StringReader(data));

                int eventtype = parser.getEventType();

                Log.d("GetEventData", String.valueOf(eventtype));
                while (eventtype != XmlPullParser.END_DOCUMENT) {
                    switch (eventtype) {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.END_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            stag = parser.getName();
                            if (stag.equals("id"))
                                is_gln = true;
                            else if (stag.equals("smartparking:available_space"))
                                is_smartparking = true;
                            break;
                        case XmlPullParser.TEXT:
                            Log.d("GetEventData", String.valueOf(eventtype));
                            if (is_gln) {
                                Log.d("GetEventData", parser.getText());
                                eventData.gln = parser.getText();
                                is_gln = false;
                            }
                            if (is_smartparking) {
                                Log.d("GetEventData", parser.getText());
                                eventData.availableSpaces = Integer.parseInt(parser.getText());
                                is_smartparking = false;
                            }
                    }
                    eventtype = parser.next();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return eventData;
        }
    }
}



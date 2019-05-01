package com.android.ecs193.meterconverter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class DataFragment extends Fragment {
    public TextView countTv;
    public Button countBtn;

    public TextView speed_textView;

    // Radius of the Earth in meters, used in calcHaversineDist
    public static final double EARTH_RADIUS = 6371071.0;
    // Creates a reference to the system Location Manager
    LocationManager locationManager;
    // Creates a listener that responds to location updates
    LocationListener locationListener;
    // To hold previous location
    public static Location oldLocation = null;
    // Constant for fine location, ACCESS_FINE_LOCATION is for GPS_Provider
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    public void DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new speedCalc();

        // This part should "register" the listener with the Location Manager to receive updates after checking permissions
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            msg("entered");
            ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            //ActivityCompat.requestPermissions(getActivity(), new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        //if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // minTime: 0 and minDistance: 0 indicates that the provider should make updates as fast as possible. This seems to be about once per second.
            msg("entered2");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        View view = inflater.inflate(R.layout.frag_data, container, false);
        countTv = (TextView) view.findViewById(R.id.count_tv);
        countTv.setText("0");
        countBtn = (Button) view.findViewById(R.id.count_btn);
        countBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseCount();
            }
        });

        speed_textView = (TextView) view.findViewById(R.id.txtCurrentSpeed);
        //speed_textView.setText(mph + " miles/hour");

        return view;
    }

    public void useNewLocation(Location location) {
        // If on first call, set oldLocation and return
        if (oldLocation == null) {
            oldLocation = location;
            return;
        }
        double newLat = location.getLatitude(); // In degrees
        double newLon = location.getLongitude(); // In degrees
        long currTime = location.getTime(); // Milliseconds since 1/1/70
        double distance = calcHaversineDist(oldLocation.getLatitude(), oldLocation.getLongitude(), newLat, newLon);
        int mph = (int) ((distance/1609.0)/(((double)(currTime - oldLocation.getTime())) / 3600000.0)); // Convert meters to miles and ms to hours
        //TextView textView = findViewById(R.id.txtCurrentSpeed); //FRAGMENTS DOES NOT PROVIDE findViewbById(), must be in oncreate
        speed_textView.setText(mph + " miles/hour");
        oldLocation = location;
    }

    // Haversine Formula for distance b/t pts on sphere given lat. & lon.; see https://en.wikipedia.org/wiki/Haversine_formula
    public static double calcHaversineDist(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2.0)
                * Math.sin(dLon / 2.0);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distInMeters = EARTH_RADIUS * c;
        return distInMeters;
    }

    // Defines a listener that responds to location updates
    class speedCalc implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

            // Called when a new location is found by the GPS location provider.
            if (location != null) {
                msg("entered4");
                useNewLocation(location);
            }
        }
        // Following functions not used but required in class which implements LocationListener
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }

    private void increaseCount() {
        int current = Integer.parseInt((String) countTv.getText());
        countTv.setText(String.valueOf(current + 1));

    }
    private void msg(String s)
    {
        Toast.makeText(getActivity().getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }


}

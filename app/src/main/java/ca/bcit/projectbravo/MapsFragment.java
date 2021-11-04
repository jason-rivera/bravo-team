package ca.bcit.projectbravo;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class MapsFragment extends Fragment {
    private GoogleMap googleMap;
    ArrayList<LatLng> locationArrayList = new ArrayList<>();
    private static final String SERVICE_URL = "https://eonet.sci.gsfc.nasa.gov/api/v2.1/events";
    ReentrantLock lock = new ReentrantLock();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        new GetDataJsonObjects().execute();
        View view = inflater.inflate(R.layout.fragment_maps, null, false);
        MapView mMapView = (MapView) view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                int counter = 0;
                while(lock.isLocked()){
                    counter++;
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        for (int i = 0; i < locationArrayList.size(); i++) {
                            mMap.addMarker(new MarkerOptions().position(locationArrayList.get(i)).title("Marker"));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(100.0f));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(locationArrayList.get(i)));
                        }
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(new LatLng(49.2827, -123.1207) )
                                .zoom(5)
                                .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }
                    if(counter>5){
                        System.out.println("something went wrong");
                    }
                }
            }
        });
        return view;
    }

    private class GetDataJsonObjects extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//
//            _progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleLarge);
//            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
//            params.addRule(RelativeLayout.CENTER_IN_PARENT);
//            RelativeLayout layout = findViewById(R.id.container);
//            layout.addView(_progressBar, params);
//            _progressBar.setVisibility(View.VISIBLE);
//        }

        @Override
        protected Void doInBackground(Void... voids) {
            lock.lock();
            HttpHandler handler = new HttpHandler();
            String jsonStr = handler.makeServiceCall(SERVICE_URL);
            if (jsonStr != null) {
                try {
                    JSONObject fire = new JSONObject(jsonStr);
                    JSONArray events = fire.getJSONArray("events");
                    for (int ndx = 0; ndx < events.length(); ndx++) {
                        JSONObject proObj = events.getJSONObject(ndx);
                        String proTitle = proObj.getString("title");
                        if (proTitle.endsWith("Canada")) {
                            JSONArray categories = proObj.getJSONArray("categories");
                            JSONArray geometries = proObj.getJSONArray("geometries");
                            JSONObject geoProperty = geometries.getJSONObject(0);
                            JSONArray coordinates = geoProperty.getJSONArray("coordinates");
                            double latitude = coordinates.getDouble(1);
                            double longitude = coordinates.getDouble(0);
                            JSONObject categoryProperty = categories.getJSONObject(0);
                            String title = categoryProperty.getString("title");
                            System.out.println("Lat:::: " + latitude);
                            System.out.println("Long:::: " + longitude);
                            Log.d("tag: ", "" + latitude);
                            if (title.equals("Wildfires")) {
                                locationArrayList.add(new LatLng(latitude, longitude));
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }

            } else {
                //Toast.makeText(getApplicationContext(), "Server Error", Toast.LENGTH_LONG).show();
            }
            return null;
        }

//        @Override
//        protected void onPostExecute(Void unused) {
//            super.onPostExecute(unused);
//
//            if (_progressBar.getVisibility() == View.VISIBLE) {
//                _progressBar.setVisibility(View.INVISIBLE);
//            }
//
//            _recyclerAdapter = new RecyclerAdapter(MainActivity.this, _fireList);
//            _recyclerView.setAdapter(_recyclerAdapter);
//        }
//    }
    }
}
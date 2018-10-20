package pistolpropulsion.com.safepath;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceApi;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResult;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // API related objects
    //    - Awareness API -
    private LocationBroadcastReceiver fenceReceiver;
    private PendingIntent mFencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";
    //    - ESRI Routing -
    private MapView mMapView;
    private GraphicsOverlay mGraphicsOverlay;
    private Point mStart;
    private Point mEnd;
    private double lat;
    private double lon;


    // Constants
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 940;

    // Widgets
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set widgets
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        mMapView = findViewById(R.id.mMapView);
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

        // Create context and api client instance
        android.content.Context context = getApplicationContext();
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();

        // Fence location
        fenceReceiver = new LocationBroadcastReceiver();
        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        mFencePendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                10001,
                intent,
                0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }

        //Getting current location
        Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get location.");
                            return;
                        }
                        Location location = locationResult.getLocation();
                        Log.i(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                        setupMap();
                    }
                });

        registerFence();
        registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFence();
        unregisterReceiver(fenceReceiver);
    }

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type basemapType = Basemap.Type.NAVIGATION_VECTOR;
            double latitude = lat;
            double longitude = lon;
            int levelOfDetail = 17;
            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            mMapView.setMap(map);
        }
    }

    protected void registerFence() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
        AwarenessFence locationFence = AwarenessFence.not(LocationFence.in(33.7765673, -84.3960469, 10, 100));
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence("loc", locationFence, mFencePendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Fence was successfully registered.");
                        } else {
                            Log.e(TAG, "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    protected void unregisterFence() {
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence("loc")
                        .build()).setResultCallback(new ResultCallbacks<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                Log.i(TAG, "Fence " + "loc" + " successfully removed.");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.i(TAG, "Fence " + "loc" + " could NOT be removed.");
            }
        });
    }


    class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            Log.d(TAG, "Fence Receiver Received");

            if (TextUtils.equals(fenceState.getFenceKey(), "loc")) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        status.setText("You left.");
                        break;
                    case FenceState.FALSE:
                        status.setText("You didn't leave.");
                        break;
                    case FenceState.UNKNOWN:
                        status.setText("Idk bro.");
                        break;
                }
            }
        }

    }

    protected void queryFence(final String fenceKey) {
        Awareness.FenceApi.queryFences(mGoogleApiClient,
                FenceQueryRequest.forFences(Arrays.asList(fenceKey)))
                .setResultCallback(new ResultCallback<FenceQueryResult>() {
                    @Override
                    public void onResult(@NonNull FenceQueryResult fenceQueryResult) {
                        if (!fenceQueryResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not query fence: " + fenceKey);
                            return;
                        }
                        FenceStateMap map = fenceQueryResult.getFenceStateMap();
                        for (String fenceKey : map.getFenceKeys()) {
                            FenceState fenceState = map.getFenceState(fenceKey);
                            Log.i(TAG, "Fence " + fenceKey + ": "
                                    + fenceState.getCurrentState()
                                    + ", was="
                                    + fenceState.getPreviousState()
                                    + ", lastUpdateTime=");
                        }
                    }
                });
    }
}

package pistolpropulsion.com.safepath;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.ImmutablePartCollection;
import com.esri.arcgisruntime.geometry.Part;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    // API related objects
    //    - Awareness API -
    private LocationBroadcastReceiver fenceReceiver;
    private PendingIntent mFencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";
    AwarenessFence total;
    //    - ESRI Routing -
    private MapView mMapView;
    LocationDisplay display;
    private GraphicsOverlay mGraphicsOverlay;
    private Point mStart;
    private Point mEnd;
    private double lat;
    private double lon;
    private Button logout_button;
    private Polyline currentPath;


    // Constants
    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVE";
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 940;
    private static final int PERMISSION_REQUEST_SEND_SMS = 941;

    // Widgets
    private TextView status;

    //popup stuff
    private Button imok;
    private EditText pincode;
    private FirebaseAuth siAuth;
    private DatabaseReference mdatabase;


    // SMS
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set SMS Shenanigans
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set widgets
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        mMapView = findViewById(R.id.mMapView);
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);
        logout_button = findViewById(R.id.LogoutButton);

        imok = findViewById(R.id.Confirmbutton);
        pincode = findViewById(R.id.Password);
        siAuth = FirebaseAuth.getInstance();
        mdatabase = FirebaseDatabase.getInstance().getReference();

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                android.graphics.Point screenPoint = new android.graphics.Point(
                        Math.round(e.getX()),
                        Math.round(e.getY()));
                Point mapPoint = mMapView.screenToLocation(screenPoint);
                mapClicked(mapPoint);
                return super.onSingleTapConfirmed(e);
            }
        });
        //Toast.makeText(getApplicationContext(), "Unable to find location", Toast.LENGTH_LONG).show();
        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        // Create context and api client instance
        android.content.Context context = getApplicationContext();
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();

        // Fence location
        fenceReceiver = new LocationBroadcastReceiver();
        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mFencePendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                10001,
                intent,
                0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_SEND_SMS);
        }

        //Getting current location
        Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get location.");
                            Toast.makeText(getApplicationContext(), "Unable to find location", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Location location = locationResult.getLocation();
                        //Log.i(TAG, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                        //start = new Point(lat, lon)
                        setupMap();
                        display.startAsync();
                        setupOauth();
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFence();
        try {
            unregisterReceiver(fenceReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMapMarker(Point location, SimpleMarkerSymbol.Style style, int markerColor, int outlineColor) {
        float markerSize = 8.0f;
        float markerOutlineThickness = 2.0f;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(style, markerColor, markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, outlineColor, markerOutlineThickness));
        Graphic pointGraphic = new Graphic(location, pointSymbol);
        mGraphicsOverlay.getGraphics().add(pointGraphic);
    }

    private void setStartMarker(Point location) {
        mGraphicsOverlay.getGraphics().clear();
        setMapMarker(location, SimpleMarkerSymbol.Style.DIAMOND, Color.rgb(226, 119, 40), Color.BLUE);
        mStart = location;
        mEnd = null;
    }

    private void setEndMarker(Point location) {
        setMapMarker(location, SimpleMarkerSymbol.Style.SQUARE, Color.rgb(40, 119, 226), Color.RED);
        mEnd = location;
        findRoute();

    }

    private void mapClicked(Point location) {
        if (mStart == null) {
            // Start is not set, set it to a tapped location
            setStartMarker(location);
        } else if (mEnd == null) {
            // End is not set, set it to the tapped location then find the route
            setEndMarker(location);
        } else {
            // Both locations are set; re-set the start to the tapped location
            setStartMarker(location);
        }
    }

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type basemapType = Basemap.Type.NAVIGATION_VECTOR;
            double latitude = lat;
            double longitude = lon;
            int levelOfDetail = 17;
            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            mMapView.setMap(map);
            display = mMapView.getLocationDisplay();
        }
    }

    protected void registerFence() {
        
        ImmutablePartCollection iterator = currentPath.getParts();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }

        Iterable<Point> iteratePoints = iterator.getPartsAsPoints();
        ArrayList<Point> PointsList = new ArrayList<>();
        int ID = 0;
        ArrayList<AwarenessFence> fences = new ArrayList<>();
        for (Point iteratePoint : iteratePoints) {
            PointsList.add(iteratePoint);
            setMapMarker(iteratePoint, SimpleMarkerSymbol.Style.CIRCLE, Color.rgb(0,0,0), Color.BLACK);
            AwarenessFence locationFence = AwarenessFence.not(LocationFence.in(iteratePoint.getY(), iteratePoint.getX(), 50, 1000));
            fences.add(locationFence);
        }

        total = AwarenessFence.and(fences);
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence("pathFence", total, mFencePendingIntent)
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

        status.setText(String.valueOf(PointsList.size()));
    }

    // Help clear the fences when the app finishes
    protected void unregisterFence() {
        Awareness.FenceApi.updateFences(
                mGoogleApiClient,
                new FenceUpdateRequest.Builder()
                        .removeFence("pathFence")
                        .build()).setResultCallback(new ResultCallbacks<Status>() {
            @Override
            public void onSuccess(@NonNull Status status) {
                Log.i(TAG, "Fence " + "pathFence" + " successfully removed.");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.i(TAG, "Fence " + "pathFence" + " could NOT be removed.");
            }
        });
    }

    // Listener for the geofence
    class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            FenceState fenceState = FenceState.extract(intent);

            Log.d(TAG, "Fence Receiver Received");

            if (TextUtils.equals(fenceState.getFenceKey(), "pathFence")) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        status.setText("You left the area!");
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.SEND_SMS},
                                    PERMISSION_REQUEST_SEND_SMS);
                        }

                        try {
                            setContentView(R.layout.activity_alert);
                            showPopup(siAuth.getCurrentUser());
                            sendMessage();
                            Toast.makeText(getApplicationContext(), "Alert Sent",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Toast.makeText(getApplicationContext(),ex.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            ex.printStackTrace();
                            sendMessage();
                        }
                        break;
                    case FenceState.FALSE:
                        status.setText("Stay Safe!");
                        break;
                    case FenceState.UNKNOWN:
                        status.setText("Service Unable to Locate Geofence.");
                        break;
                }
            }
        }

        public void sendMessage() {
            final SmsManager smsManager = SmsManager.getDefault();
            String Uid = mAuth.getCurrentUser().getUid(); // gets the user ID
            DatabaseReference userRef = mDatabase.child("users").child((mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserFirebase fetchedUser = dataSnapshot.getValue(UserFirebase.class);
                    String contact = "+1" + fetchedUser.getContact();
                    String name = fetchedUser.getName();
//                    status.setText(contact);
                    smsManager.sendTextMessage(contact, null, name + " is missing!", null, null);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }

    }

    // Authentication stuff
    private void setupOauth() {
        String clientId = getResources().getString(R.string.client_id);
        String redirectUri = getResources().getString(R.string.redirect_uri);

        try {
            OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com", clientId, redirectUri);
            DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
            AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
            AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
        } catch (MalformedURLException e) {
            showError(e.getMessage());
        }
    }

    // Error handling
    private void showError(String message) {
        Log.d("FindRoute", message);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public void logout(){
        FirebaseAuth.getInstance().signOut();
        Intent signintent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(signintent);
    }

    // Route locating
    private void findRoute() {
        String routeServiceURI = "https://utility.arcgis.com/usrsvcs/appservices/8GnYPj2wiDmNVyv1/rest/services/World/Route/NAServer/Route_World/";
        final RouteTask solveRouteTask = new RouteTask(getApplicationContext(), routeServiceURI);
        solveRouteTask.loadAsync();
        solveRouteTask.addDoneLoadingListener(new Runnable() {
            @Override public void run() {
                if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {
                    final ListenableFuture<RouteParameters> routeParamsFuture = solveRouteTask.createDefaultParametersAsync();
                    routeParamsFuture.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                RouteParameters routeParameters = routeParamsFuture.get();
                                List<Stop> stops = new ArrayList<>();
                                stops.add(new Stop(mStart));
                                stops.add(new Stop(mEnd));
                                routeParameters.setStops(stops);
                                final ListenableFuture<RouteResult> routeResultFuture = solveRouteTask.solveRouteAsync(routeParameters);
                                routeResultFuture.addDoneListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            RouteResult routeResult = routeResultFuture.get();
                                            Route firstRoute = routeResult.getRoutes().get(0);
                                            Polyline routePolyline = firstRoute.getRouteGeometry();
                                            currentPath = routePolyline;
                                            SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 4.0f);
                                            Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                                            mGraphicsOverlay.getGraphics().add(routeGraphic);
                                            registerFence();
                                            registerReceiver(fenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

                                        } catch (InterruptedException | ExecutionException e) {
                                            showError("Solve RouteTask failed " + e.getMessage());
                                        }
                                    }
                                });

                            } catch (InterruptedException | ExecutionException e) {
                                showError("Cannot create RouteTask parameters " + e.getMessage());
                            }
                        }
                    });
                } else {
                    showError("Unable to load RouteTask " + solveRouteTask.getLoadStatus().toString());
                }
            }
        });
    }

    private PopupWindow pw;
    private void showPopup(FirebaseUser user) {
        try {
            // We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.activity_alert,
                    (ViewGroup) findViewById(R.id.Alertpopup));
            pw = new PopupWindow(layout, 300, 370, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

            //check if pincode entered is the same as the user's password
            imok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String databasepincode = mdatabase.child(siAuth.getCurrentUser().getUid()).child("contact").toString();
                    if(pincode.getText().toString().equals(databasepincode)) {
                        Toast.makeText(getApplicationContext(), "Cheers", Toast.LENGTH_LONG).show();
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("+17066146514", null, "safe", null, null);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

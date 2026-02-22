package com.example.arvr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.ar.core.ArCoreApk;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ArVrPlugin {

    private Context context;
    private ArVrPluginPlugin plugin;
    private FrameLayout arContainer;
    private ArSceneView arSceneView;
    private List<JSONObject> pois = new ArrayList<>();
    private Location userLocation;
    private boolean poisPlaced = false;

    public interface SessionCallback {
        void onSuccess();
        void onError(String message);
    }

    public ArVrPlugin(Context context, ArVrPluginPlugin plugin) {
        this.context = context;
        this.plugin = plugin;
    }

    @SuppressLint("MissingPermission")
    public void startSession(JSArray poiArray, SessionCallback callback) {
        // Parse POI data
        try {
            this.pois = new ArrayList<>();
            List<Object> list = poiArray.toList();
            for (Object obj : list) {
                if (obj instanceof JSONObject) {
                    this.pois.add((JSONObject) obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get user location, then set up AR
        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLocation = location;
                }
                initArSession(callback);
            });
        } else {
            initArSession(callback);
        }
    }

    private void initArSession(SessionCallback callback) {
        new Thread(() -> {
            try {
                ArCoreApk.Availability availability =
                        ArCoreApk.getInstance().checkAvailability(context);
                if (!availability.isSupported()) {
                    callback.onError(
                        "ARCore is not supported on this device. " +
                        "Please use a physical ARCore-compatible device.");
                    return;
                }

                Session session = new Session(context);
                Config config = new Config(session);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                session.configure(config);

                plugin.getBridge().executeOnMainThread(() -> {
                    try {
                        // Create our own FrameLayout container
                        arContainer = new FrameLayout(context);
                        ViewGroup.LayoutParams containerParams = new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT);

                        ViewGroup parent = (ViewGroup) plugin.getBridge().getWebView().getParent();
                        int index = parent.indexOfChild(plugin.getBridge().getWebView());
                        parent.addView(arContainer, index, containerParams);

                        arSceneView = new ArSceneView(context);
                        arSceneView.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));
                        arContainer.addView(arSceneView);

                        arSceneView.setupSession(session);
                        arSceneView.resume();

                        // Hide the default plane indicator circle
                        arSceneView.getPlaneRenderer().setEnabled(false);

                        makeWebViewTransparent();

                        // Defer addPoiAnchors until the first frame is ready
                        poisPlaced = false;
                        arSceneView.getScene().addOnUpdateListener(frameTime -> {
                            if (!poisPlaced && arSceneView.getArFrame() != null
                                    && arSceneView.getArFrame().getCamera() != null) {
                                poisPlaced = true;
                                addPoiAnchors();
                            }
                        });

                        android.util.Log.i("ArVrPlugin", "AR session started successfully");
                        callback.onSuccess();
                    } catch (Exception e) {
                        android.util.Log.e("ArVrPlugin", "Failed to start AR view: " + e.getMessage());
                        callback.onError("Failed to start AR view: " + e.getMessage());
                    }
                });

            } catch (com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException e) {
                callback.onError(
                    "Google Play Services for AR is not installed. " +
                    "Please install it from the Play Store.");
            } catch (com.google.ar.core.exceptions.FatalException e) {
                callback.onError(
                    "ARCore encountered a fatal error. " +
                    "This usually means the device camera is not accessible. " +
                    "If you are using an emulator, please test on a physical device instead.");
            } catch (Exception e) {
                android.util.Log.e("ArVrPlugin", "AR setup failed: " + e.getMessage());
                callback.onError("AR setup failed: " + e.getMessage());
            }
        }).start();
    }

    public void stopSession() {
        plugin.getBridge().executeOnMainThread(() -> {
            if (arSceneView != null) {
                arSceneView.pause();
                arSceneView = null;
            }
            if (arContainer != null) {
                ViewGroup parent = (ViewGroup) arContainer.getParent();
                if (parent != null) parent.removeView(arContainer);
                arContainer = null;
            }
            poisPlaced = false;
        });
    }

    // ─── WebView Transparency ────────────────────────────────────

    private void makeWebViewTransparent() {
        View webView = plugin.getBridge().getWebView();
        webView.setBackgroundColor(Color.TRANSPARENT);
        if (webView instanceof android.webkit.WebView) {
            ((android.webkit.WebView) webView).setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // ─── POI Anchors ─────────────────────────────────────────────

    private void addPoiAnchors() {
        if (arSceneView == null || arSceneView.getArFrame() == null) return;

        for (JSONObject poi : pois) {
            try {
                String id = poi.getString("id");
                double poiLat = poi.getDouble("lat");
                double poiLng = poi.getDouble("lng");
                String label = poi.optString("label", "Property");
                String url = poi.optString("url", "");
                String imageUrl = poi.optString("image", "");

                // Calculate AR position from GPS
                float[] offset = gpsToLocal(poiLat, poiLng);
                float distMeters = (float) Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1]);

                // Create anchor at computed offset
                Anchor anchor = arSceneView.getSession().createAnchor(
                        arSceneView.getArFrame().getCamera().getPose()
                                .compose(com.google.ar.core.Pose.makeTranslation(offset[0], 0, offset[1]))
                                .extractTranslation()
                );

                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arSceneView.getScene());

                buildPoiCard(anchorNode, id, label, distMeters, url, imageUrl);

            } catch (Exception e) {
                android.util.Log.w("ArVrPlugin", "Failed to place POI: " + e.getMessage());
            }
        }
    }

    private void buildPoiCard(AnchorNode anchorNode, String id, String label,
                              float distMeters, String url, String imageUrl) {
        ViewRenderable.builder()
                .setView(context, R.layout.poi_card)
                .build()
                .thenAccept(renderable -> {
                    View cardView = renderable.getView();
                    ((TextView) cardView.findViewById(R.id.poi_label)).setText(label);
                    ((TextView) cardView.findViewById(R.id.poi_distance)).setText(
                            formatDistance(distMeters) + " away");

                    // Load image placeholder (gray box for now)
                    ImageView imageView = cardView.findViewById(R.id.poi_image);
                    imageView.setBackgroundColor(Color.parseColor("#CCCCCC"));

                    // Create a node for the card
                    Node cardNode = new Node();
                    cardNode.setRenderable(renderable);
                    cardNode.setParent(anchorNode);

                    // Elevate the card above the anchor point
                    cardNode.setLocalPosition(new Vector3(0.0f, 1.5f, 0.0f));

                    // Billboard: always face the camera
                    cardNode.setLookDirection(Vector3.forward());

                    // Tap handler → send event to Angular
                    cardNode.setOnTapListener((hitTestResult, motionEvent) -> {
                        JSObject data = new JSObject();
                        data.put("id", id);
                        data.put("url", url);
                        data.put("label", label);
                        plugin.notifyObjectSelected(data);
                    });
                });
    }

    // ─── GPS to local AR coords ──────────────────────────────────

    private float[] gpsToLocal(double poiLat, double poiLng) {
        double userLat, userLng;
        if (userLocation != null) {
            userLat = userLocation.getLatitude();
            userLng = userLocation.getLongitude();
        } else {
            userLat = 0;
            userLng = 0;
        }

        double dLat = Math.toRadians(poiLat - userLat);
        double dLng = Math.toRadians(poiLng - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(poiLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371000 * c;

        double y = Math.sin(dLng) * Math.cos(Math.toRadians(poiLat));
        double x = Math.cos(Math.toRadians(userLat)) * Math.sin(Math.toRadians(poiLat)) -
                Math.sin(Math.toRadians(userLat)) * Math.cos(Math.toRadians(poiLat)) *
                        Math.cos(dLng);
        double bearing = Math.atan2(y, x);

        // Clamp distance for AR visibility (max 50m in AR space)
        float arDist = (float) Math.min(distance, 50.0);

        float arX = (float) (arDist * Math.sin(bearing));
        float arZ = (float) (-arDist * Math.cos(bearing));
        return new float[]{arX, arZ};
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String formatDistance(float meters) {
        if (meters < 1000) {
            return String.format("%.0fm", meters);
        }
        return String.format("%.1fkm", meters / 1000);
    }
}

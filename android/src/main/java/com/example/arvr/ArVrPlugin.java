package com.example.arvr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.google.ar.sceneform.math.Quaternion;
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
    private ArSceneView arSceneView;
    private ArSceneView vrRightView;
    private List<JSONObject> pois = new ArrayList<>();
    private boolean isVrMode = false;
    private Location userLocation;

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
        // Create the session on a background thread to avoid ANR
        new Thread(() -> {
            try {
                // Check ARCore availability
                ArCoreApk.Availability availability =
                        ArCoreApk.getInstance().checkAvailability(context);
                if (!availability.isSupported()) {
                    callback.onError(
                        "ARCore is not supported on this device. " +
                        "Please use a physical ARCore-compatible device.");
                    return;
                }

                // Create session on background thread
                Session session = new Session(context);
                Config config = new Config(session);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                session.configure(config);

                // Switch to main thread for view operations
                plugin.getBridge().executeOnMainThread(() -> {
                    try {
                        arSceneView = new ArSceneView(context);
                        arSceneView.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));

                        ViewGroup parent = (ViewGroup) plugin.getBridge().getWebView().getParent();
                        int index = parent.indexOfChild(plugin.getBridge().getWebView());
                        parent.addView(arSceneView, index);

                        arSceneView.setupSession(session);
                        arSceneView.resume();
                        makeWebViewTransparent();
                        addPoiAnchors();

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
                ((ViewGroup) arSceneView.getParent()).removeView(arSceneView);
                arSceneView = null;
            }
            disableVR();
            restoreWebView();
        });
    }

    public void toggleVRMode(boolean enable) {
        this.isVrMode = enable;
        plugin.getBridge().executeOnMainThread(() -> {
            if (enable) {
                enableVR();
            } else {
                disableVR();
            }
        });
    }


    private void makeWebViewTransparent() {
        View webView = plugin.getBridge().getWebView();
        webView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void restoreWebView() {
        plugin.getBridge().getWebView().setBackgroundColor(Color.WHITE);
    }

    // ─── POI Placement ───────────────────────────────────────────

    private void addPoiAnchors() {
        if (arSceneView == null || arSceneView.getSession() == null) return;

        for (JSONObject poi : pois) {
            try {
                String id = poi.getString("id");
                double poiLat = poi.getDouble("lat");
                double poiLng = poi.getDouble("lng");
                String label = poi.optString("label", "POI");
                String url = poi.optString("url", "");
                String icon = poi.optString("icon", "📍");
                double rating = poi.optDouble("rating", 4.0);
                int votes = poi.optInt("votes", 0);

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

                // Build the info card as a ViewRenderable
                buildPoiCard(anchorNode, id, label, icon, distMeters, rating, votes, url);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void buildPoiCard(AnchorNode anchorNode, String id, String label,
                              String icon, float distMeters, double rating,
                              int votes, String url) {
        ViewRenderable.builder()
                .setView(context, R.layout.poi_card)
                .build()
                .thenAccept(renderable -> {
                    // Populate the card view
                    View cardView = renderable.getView();
                    ((TextView) cardView.findViewById(R.id.poi_label)).setText(label);
                    ((TextView) cardView.findViewById(R.id.poi_icon)).setText(icon);
                    ((TextView) cardView.findViewById(R.id.poi_distance)).setText(
                            formatDistance(distMeters));
                    ((TextView) cardView.findViewById(R.id.poi_rating)).setText(
                            getStarString(rating));
                    ((TextView) cardView.findViewById(R.id.poi_votes)).setText(
                            votes + " votes");

                    // Create a node for the card
                    Node cardNode = new Node();
                    cardNode.setRenderable(renderable);
                    cardNode.setParent(anchorNode);

                    // Elevate the card slightly above the anchor point
                    cardNode.setLocalPosition(new Vector3(0.0f, 1.5f, 0.0f));

                    // Billboard: always face the camera
                    cardNode.setLookDirection(Vector3.forward());

                    // Make it tappable
                    cardNode.setOnTapListener((hitTestResult, motionEvent) -> {
                        onObjectSelected(id);
                    });
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }

    // ─── Event Emission ──────────────────────────────────────────

    public void onObjectSelected(String id) {
        for (JSONObject poi : pois) {
            try {
                if (poi.getString("id").equals(id)) {
                    JSObject ret = new JSObject();
                    ret.put("id", id);
                    ret.put("url", poi.getString("url"));
                    plugin.notifyObjectSelected(ret);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ─── GPS to Local AR Coordinates ─────────────────────────────

    private float[] gpsToLocal(double poiLat, double poiLng) {
        if (userLocation == null) {
            // Fallback: treat raw coords as small offsets (demo mode)
            return new float[]{(float) poiLng * 10f, (float) -poiLat * 10f};
        }

        double userLat = Math.toRadians(userLocation.getLatitude());
        double userLng = Math.toRadians(userLocation.getLongitude());
        double targetLat = Math.toRadians(poiLat);
        double targetLng = Math.toRadians(poiLng);

        double dLat = targetLat - userLat;
        double dLng = targetLng - userLng;

        // Haversine distance
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(userLat) * Math.cos(targetLat) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371000 * c; // meters

        // Bearing
        double y = Math.sin(dLng) * Math.cos(targetLat);
        double x = Math.cos(userLat) * Math.sin(targetLat) -
                Math.sin(userLat) * Math.cos(targetLat) * Math.cos(dLng);
        double bearing = Math.atan2(y, x);

        // Convert to local AR: x = east, z = -north
        float localX = (float) (distance * Math.sin(bearing));
        float localZ = (float) (-distance * Math.cos(bearing));

        return new float[]{localX, localZ};
    }

    // ─── VR Mode ─────────────────────────────────────────────────

    private View vrDivider;

    private void enableVR() {
        if (arSceneView == null) return;

        ViewGroup parent = (ViewGroup) arSceneView.getParent();
        int totalWidth = parent.getWidth();
        int height = parent.getHeight();
        int halfWidth = totalWidth / 2;

        // Resize the main ArSceneView to cover the left half
        FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(halfWidth, height);
        leftParams.leftMargin = 0;
        arSceneView.setLayoutParams(leftParams);

        // Create a mirrored right-eye view by duplicating the ArSceneView
        // ARCore only supports one session, so we create a second ArSceneView
        // that shares the same session's camera imagery
        vrRightView = new ArSceneView(context);
        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(halfWidth, height);
        rightParams.leftMargin = halfWidth;
        vrRightView.setLayoutParams(rightParams);

        int index = parent.indexOfChild(arSceneView);
        parent.addView(vrRightView, index);

        // Share the existing session with the right eye
        try {
            Session session = arSceneView.getSession();
            if (session != null) {
                // The right view renders the same scene without owning the session
                vrRightView.setupSession(session);
                vrRightView.resume();
            }
        } catch (Exception e) {
            android.util.Log.w("ArVrPlugin", "VR right eye setup issue: " + e.getMessage());
            // Fallback: Just duplicate the geometry by keeping both views alive
        }

        // Add center divider line
        vrDivider = new View(context);
        vrDivider.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams dividerParams =
                new FrameLayout.LayoutParams(4, height);
        dividerParams.leftMargin = halfWidth - 2;
        vrDivider.setLayoutParams(dividerParams);
        parent.addView(vrDivider);

        // Force landscape orientation for VR
        plugin.getActivity().setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void disableVR() {
        // Remove right eye
        if (vrRightView != null) {
            vrRightView.pause();
            ViewGroup parent = (ViewGroup) vrRightView.getParent();
            if (parent != null) parent.removeView(vrRightView);
            vrRightView = null;
        }
        // Remove divider
        if (vrDivider != null) {
            ViewGroup parent = (ViewGroup) vrDivider.getParent();
            if (parent != null) parent.removeView(vrDivider);
            vrDivider = null;
        }
        // Restore main ArSceneView to full size
        if (arSceneView != null) {
            FrameLayout.LayoutParams fullParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            arSceneView.setLayoutParams(fullParams);
        }
        // Restore rotation
        plugin.getActivity().setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private String formatDistance(float meters) {
        if (meters < 1000) {
            return String.format("%.0fm", meters);
        }
        return String.format("%.1fkm", meters / 1000);
    }

    private String getStarString(double rating) {
        int full = (int) Math.floor(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < full; i++) sb.append("★");
        for (int i = full; i < 5; i++) sb.append("☆");
        return sb.toString();
    }
}

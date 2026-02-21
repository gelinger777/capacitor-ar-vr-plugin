package com.example.arvr;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ArVrPlugin {
    private Context context;
    private Bridge bridge;
    private FrameLayout arContainer;
    private List<JSObject> pois = new ArrayList<>();
    private boolean isVrMode = false;

    public ArVrPlugin(Context context, Bridge bridge) {
        this.context = context;
        this.bridge = bridge;
    }

    public void startSession(JSArray poiArray) {
        try {
            this.pois = poiArray.toList();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        bridge.executeOnMainThread(() -> {
            makeWebViewTransparent();
            setupAR();
            addAnchors();
        });
    }

    public void stopSession() {
        bridge.executeOnMainThread(() -> {
            if (arContainer != null) {
                ((ViewGroup) bridge.getWebView().getParent()).removeView(arContainer);
                arContainer = null;
            }
            restoreWebView();
        });
    }

    public void toggleVRMode(boolean enable) {
        this.isVrMode = enable;
        bridge.executeOnMainThread(() -> {
            if (enable) {
                // Implement Side-by-Side split for Android
            } else {
                // Restore single view
            }
        });
    }

    private void makeWebViewTransparent() {
        View webView = bridge.getWebView();
        webView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void restoreWebView() {
        bridge.getWebView().setBackgroundColor(Color.WHITE);
    }

    private void setupAR() {
        arContainer = new FrameLayout(context);
        arContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ViewGroup parent = (ViewGroup) bridge.getWebView().getParent();
        int index = parent.indexOfChild(bridge.getWebView());
        parent.addView(arContainer, index);
    }

    private void addAnchors() {
        for (JSObject poi : pois) {
            String id = poi.getString("id");
            Double lat = poi.getDouble("lat");
            Double lng = poi.getDouble("lng");
        }
    }

    public void onObjectSelected(String id) {
        for (JSObject poi : pois) {
            if (poi.getString("id").equals(id)) {
                JSObject ret = new JSObject();
                ret.put("id", id);
                ret.put("url", poi.getString("url"));
                bridge.triggerWindowListener("onObjectSelected", ret.toString());
                break;
            }
        }
    }
}

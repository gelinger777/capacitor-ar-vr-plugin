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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ArVrPlugin {
    private Context context;
    private ArVrPluginPlugin plugin;
    private FrameLayout arContainer;
    private List<JSONObject> pois = new ArrayList<>();
    private boolean isVrMode = false;

    public ArVrPlugin(Context context, ArVrPluginPlugin plugin) {
        this.context = context;
        this.plugin = plugin;
    }

    public void startSession(JSArray poiArray) {
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

        plugin.getBridge().executeOnMainThread(() -> {
            makeWebViewTransparent();
            setupAR();
            addAnchors();
        });
    }

    public void stopSession() {
        plugin.getBridge().executeOnMainThread(() -> {
            if (arContainer != null) {
                ((ViewGroup) plugin.getBridge().getWebView().getParent()).removeView(arContainer);
                arContainer = null;
            }
            restoreWebView();
        });
    }

    public void toggleVRMode(boolean enable) {
        this.isVrMode = enable;
        plugin.getBridge().executeOnMainThread(() -> {
            if (enable) {
                // Implement Side-by-Side split for Android
            } else {
                // Restore single view
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

    private void setupAR() {
        arContainer = new FrameLayout(context);
        arContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ViewGroup parent = (ViewGroup) plugin.getBridge().getWebView().getParent();
        int index = parent.indexOfChild(plugin.getBridge().getWebView());
        parent.addView(arContainer, index);
    }

    private void addAnchors() {
        for (JSONObject poi : pois) {
            try {
                String id = poi.getString("id");
                Double lat = poi.getDouble("lat");
                Double lng = poi.getDouble("lng");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
}

# capacitor-ar-vr-plugin

A high-performance Capacitor 6 plugin for initializing native AR/VR sessions with GPS-based anchoring, low-latency head tracking, and stereoscopic rendering.

## Core Features
- **WebView Transparency**: Seamlessly overlay your Ionic/Angular UI on top of the native AR camera view.
- **World Tracking & GPS Anchors**: Place 3D virtual objects at specific real-world coordinates.
- **Raycasting/Hit-Testing**: Interact with 3D anchors through touch or by "looking" at them (center reticle).
- **Stereoscopic VR Mode**: Low-latency side-by-side rendering for mobile VR headsets.

## Install

```bash
npm install capacitor-ar-vr-plugin
npx cap sync
```

## Native Setup

### iOS (ARKit)
Add the following keys to your `Info.plist`:
- `NSCameraUsageDescription`: "We need camera access for AR."
- `NSLocationWhenInUseUsageDescription`: "We need location access to place AR anchors."

### Android (ARCore)
Ensure your `AndroidManifest.xml` includes permissions for Camera and Location, and that ARCore is enabled:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
```

## API

<docgen-index>

* [`startSession(...)`](#startsession)
* [`stopSession()`](#stopsession)
* [`toggleVRMode(...)`](#togglevrmode)
* [`addListener('onObjectSelected', ...)`](#addlisteneronobjectselected-)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### startSession(...)

```typescript
startSession(options: ArVrSessionOptions) => Promise<void>
```

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#arvrsessionoptions">ArVrSessionOptions</a></code> |

--------------------


### stopSession()

```typescript
stopSession() => Promise<void>
```

--------------------


### toggleVRMode(...)

```typescript
toggleVRMode(options: { enable: boolean; }) => Promise<void>
```

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ enable: boolean; }</code> |

--------------------


### addListener('onObjectSelected', ...)

```typescript
addListener(eventName: 'onObjectSelected', listenerFunc: (data: { id: string; url: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                         |
| ------------------ | ------------------------------------------------------------ |
| **`eventName`**    | <code>'onObjectSelected'</code>                              |
| **`listenerFunc`** | <code>(data: { id: string; url: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### Interfaces


#### ArVrSessionOptions

| Prop       | Type               |
| ---------- | ------------------ |
| **`pois`** | <code>Poi[]</code> |


#### Poi

| Prop        | Type                |
| ----------- | ------------------- |
| **`id`**    | <code>string</code> |
| **`lat`**   | <code>number</code> |
| **`lng`**   | <code>number</code> |
| **`label`** | <code>string</code> |
| **`url`**   | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>

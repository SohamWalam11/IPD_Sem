# AR Tyre Model Setup

## Sketchfab Rally Wheel Model

The AR Tyre Viewer uses the **Rally Wheel** 3D model by **Kryox Shade** from Sketchfab.

### Model Details
- **Name:** Rally Wheel
- **Author:** Kryox Shade
- **URL:** https://sketchfab.com/3d-models/rally-wheel-c580152ebef94978920ad13c730e4eec
- **License:** Check Sketchfab for current license terms

### Download Instructions

1. Visit the model page: https://sketchfab.com/3d-models/rally-wheel-c580152ebef94978920ad13c730e4eec
2. Click **Download 3D Model** (requires Sketchfab account)
3. Select **glTF** format (includes GLB binary)
4. Extract the downloaded ZIP file
5. Find the `.glb` file and rename it to `rally_wheel.glb`
6. Place the file in: `composeApp/src/androidMain/assets/models/rally_wheel.glb`

### Alternative: Use Sample Model

If you don't have a Sketchfab account, the app will fall back to a demo 3D model for testing purposes.

### File Structure
```
composeApp/
└── src/
    └── androidMain/
        └── assets/
            └── models/
                ├── rally_wheel.glb    <- Place the downloaded model here
                └── best_int8.tflite   <- Existing ML model
```

## Using the AR Viewer

To launch the AR Tyre Viewer from your app:

```kotlin
import org.example.project.ar.ArLauncher

// Check if AR is available
if (ArLauncher.isArAvailable(context)) {
    // Launch AR viewer
    ArLauncher.launchArViewer(context)
    
    // Or with a custom model path
    ArLauncher.launchArViewer(context, "/path/to/custom/model.glb")
}
```

## Features

- **Plane Detection:** Automatically detects horizontal and vertical surfaces
- **Model Placement:** Tap on detected surfaces to place the 3D tyre
- **Gesture Controls:** 
  - Pinch to scale the model
  - Drag to rotate
- **Health Data Overlay:** Displays tyre health metrics in floating panels
  - Pressure (PSI)
  - Temperature (°C)
  - Status (Optimal/Warning/Critical)
  - Tread Depth (mm)

## Troubleshooting

### "This device does not support AR"
- Your device needs ARCore support
- Install Google Play Services for AR from Play Store

### Model not loading
- Check if the GLB file exists in the assets folder
- Verify the file is not corrupted
- Check logcat for detailed error messages

### Tracking Lost
- Move to a well-lit area
- Point camera at textured surfaces
- Move the device more slowly

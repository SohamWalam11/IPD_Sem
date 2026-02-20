import 'package:webview_flutter/webview_flutter.dart';
import 'package:logger/logger.dart';

class ModelViewerService {
  static final ModelViewerService _instance = ModelViewerService._internal();
  final Logger _logger = Logger();

  factory ModelViewerService() {
    return _instance;
  }

  ModelViewerService._internal();

  /// Generate HTML content for viewing GLB model
  String generateModelViewerHTML(String modelUrl) {
    return '''
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>3D Tire Model Viewer</title>
      <script type="module" src="https://unpkg.com/@google/model-viewer/dist/model-viewer.min.js"></script>
      <style>
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        
        body {
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 100vh;
          padding: 20px;
        }
        
        .container {
          width: 100%;
          max-width: 800px;
          background: white;
          border-radius: 12px;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
          overflow: hidden;
        }
        
        .header {
          background: linear-gradient(135deg, #2E7D32 0%, #1B5E20 100%);
          color: white;
          padding: 20px;
          text-align: center;
        }
        
        .header h1 {
          font-size: 24px;
          margin-bottom: 5px;
        }
        
        .header p {
          font-size: 14px;
          opacity: 0.9;
        }
        
        model-viewer {
          width: 100%;
          height: 500px;
          background: #f5f5f5;
        }
        
        .controls {
          padding: 20px;
          background: #fafafa;
          border-top: 1px solid #e0e0e0;
        }
        
        .control-group {
          margin-bottom: 15px;
        }
        
        .control-group label {
          display: block;
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
          color: #333;
        }
        
        .button-group {
          display: flex;
          gap: 10px;
          flex-wrap: wrap;
        }
        
        button {
          flex: 1;
          min-width: 100px;
          padding: 10px 15px;
          border: none;
          border-radius: 6px;
          background: #2E7D32;
          color: white;
          font-size: 14px;
          font-weight: 600;
          cursor: pointer;
          transition: background 0.3s;
        }
        
        button:hover {
          background: #1B5E20;
        }
        
        button:active {
          transform: scale(0.98);
        }
        
        .info {
          padding: 15px;
          background: #e8f5e9;
          border-left: 4px solid #2E7D32;
          border-radius: 4px;
          font-size: 13px;
          color: #1B5E20;
        }
        
        .loading {
          text-align: center;
          padding: 40px;
          color: #666;
        }
        
        .spinner {
          border: 4px solid #f3f3f3;
          border-top: 4px solid #2E7D32;
          border-radius: 50%;
          width: 40px;
          height: 40px;
          animation: spin 1s linear infinite;
          margin: 0 auto 15px;
        }
        
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <h1>3D Tire Model</h1>
          <p>Interactive 3D Visualization</p>
        </div>
        
        <model-viewer
          id="viewer"
          src="$modelUrl"
          alt="3D Tire Model"
          auto-rotate
          camera-controls
          touch-action="pan-y"
          style="width: 100%; height: 500px;">
        </model-viewer>
        
        <div class="controls">
          <div class="control-group">
            <label>View Controls</label>
            <div class="button-group">
              <button onclick="resetView()">Reset View</button>
              <button onclick="toggleAutoRotate()">Toggle Rotation</button>
            </div>
          </div>
          
          <div class="info">
            <strong>Controls:</strong> Pinch to zoom • Drag to rotate • Two-finger drag to pan
          </div>
        </div>
      </div>
      
      <script>
        const viewer = document.getElementById('viewer');
        let isAutoRotating = true;
        
        function resetView() {
          viewer.cameraTarget = '0m 0m 0m';
          viewer.cameraOrbit = '0deg 75deg 105%';
        }
        
        function toggleAutoRotate() {
          isAutoRotating = !isAutoRotating;
          viewer.autoRotate = isAutoRotating;
        }
        
        // Handle model load
        viewer.addEventListener('load', function() {
          console.log('Model loaded successfully');
        });
        
        // Handle model error
        viewer.addEventListener('error', function(e) {
          console.error('Model loading error:', e);
        });
      </script>
    </body>
    </html>
    ''';
  }

  /// Generate HTML for loading state
  String generateLoadingHTML() {
    return '''
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Loading</title>
      <style>
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        
        body {
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 100vh;
        }
        
        .loading {
          text-align: center;
          color: white;
        }
        
        .spinner {
          border: 4px solid rgba(255, 255, 255, 0.3);
          border-top: 4px solid white;
          border-radius: 50%;
          width: 50px;
          height: 50px;
          animation: spin 1s linear infinite;
          margin: 0 auto 20px;
        }
        
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        p {
          font-size: 18px;
        }
      </style>
    </head>
    <body>
      <div class="loading">
        <div class="spinner"></div>
        <p>Loading 3D Model...</p>
      </div>
    </body>
    </html>
    ''';
  }

  /// Generate HTML for error state
  String generateErrorHTML(String errorMessage) {
    return '''
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Error</title>
      <style>
        * {
          margin: 0;
          padding: 0;
          box-sizing: border-box;
        }
        
        body {
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 100vh;
          padding: 20px;
        }
        
        .error-container {
          background: white;
          border-radius: 12px;
          padding: 40px;
          text-align: center;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
          max-width: 400px;
        }
        
        .error-icon {
          font-size: 60px;
          margin-bottom: 20px;
        }
        
        h1 {
          color: #d32f2f;
          margin-bottom: 10px;
          font-size: 24px;
        }
        
        p {
          color: #666;
          font-size: 14px;
          line-height: 1.6;
        }
      </style>
    </head>
    <body>
      <div class="error-container">
        <div class="error-icon">⚠️</div>
        <h1>Error Loading Model</h1>
        <p>$errorMessage</p>
      </div>
    </body>
    </html>
    ''';
  }
}

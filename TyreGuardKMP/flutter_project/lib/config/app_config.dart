class AppConfig {
  // API Configuration
  static const String apiBaseUrl = 'https://api.tyreguard.com/api/v1';
  static const String apiTimeout = '30'; // seconds
  
  // Firebase Configuration
  static const String firebaseProjectId = 'tyreguard-project';
  
  // 3D Model Generation
  static const String tripo3dApiKey = 'YOUR_TRIPO3D_API_KEY';
  static const String meshyApiKey = 'YOUR_MESHY_API_KEY';
  static const String modelGenerationProvider = 'tripo3d'; // or 'meshy'
  
  // ML Configuration
  static const String mlProvider = 'google_ml_kit'; // or 'tensorflow_lite'
  
  // App Configuration
  static const String appVersion = '1.0.0';
  static const String appName = 'TyreGuard';
  
  // Health Score Thresholds
  static const int criticalHealthThreshold = 40;
  static const int cautionHealthThreshold = 70;
  
  // Polling Configuration
  static const int modelGenerationPollInterval = 5; // seconds
  static const int modelGenerationMaxRetries = 120; // 10 minutes
  
  // Image Configuration
  static const int maxImageSize = 5242880; // 5MB
  static const int imageCompressionQuality = 85;
  
  // Request Timeout
  static const Duration requestTimeout = Duration(seconds: 30);
  
  // Cache Configuration
  static const Duration cacheExpiration = Duration(hours: 24);
}

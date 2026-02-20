# TyreGuard Application - Setup Guide

## Overview
TyreGuard is a comprehensive tire health monitoring application with:
- **Flutter UI**: Modern mobile interface with camera integration
- **Kotlin Backend**: RESTful API with Spring Boot
- **Cloud Services**: Firebase, Google ML Kit, Tripo3D/Meshy APIs

## Prerequisites

### Flutter Setup
- Flutter SDK 3.0.0 or higher
- Dart SDK 3.0.0 or higher
- Android Studio or Xcode (for iOS)
- Firebase CLI

### Kotlin Backend Setup
- Java 17 or higher
- Gradle 8.0 or higher
- PostgreSQL 14+ (or use Firestore)
- Firebase Admin SDK credentials

## Installation

### 1. Flutter Project Setup

```bash
cd flutter_project
flutter pub get
```

**Key Dependencies:**
- `provider` - State management
- `dio` - HTTP client
- `firebase_core`, `firebase_auth`, `cloud_firestore` - Firebase integration
- `camera`, `image_picker` - Image capture
- `webview_flutter` - 3D model viewing
- `flutter_secure_storage` - Secure token storage
- `geolocator`, `google_maps_flutter` - Location services

### 2. Kotlin Backend Setup

```bash
cd kotlin_backend
./gradlew build
```

**Key Dependencies:**
- Spring Boot 3.1.5
- Spring Security with JWT
- Firebase Admin SDK
- Google Cloud Storage & Logging
- PostgreSQL driver

### 3. Firebase Configuration

#### For Flutter:
1. Create Firebase project at https://console.firebase.google.com
2. Add Android and iOS apps
3. Download `google-services.json` (Android) and `GoogleService-Info.plist` (iOS)
4. Update `firebase_options.dart` with your credentials

#### For Kotlin Backend:
1. Generate Firebase Admin SDK key
2. Save as `firebase-credentials.json`
3. Set `FIREBASE_CREDENTIALS_PATH` environment variable

### 4. Environment Configuration

#### Flutter (`lib/config/app_config.dart`):
```dart
static const String apiBaseUrl = 'https://api.tyreguard.com/api/v1';
static const String tripo3dApiKey = 'YOUR_TRIPO3D_API_KEY';
static const String meshyApiKey = 'YOUR_MESHY_API_KEY';
```

#### Kotlin Backend (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tyreguard
    username: postgres
    password: password

firebase:
  project-id: tyreguard-project
  storage-bucket: tyreguard-project.appspot.com

jwt:
  secret: your-secret-key-change-in-production
```

## Running the Application

### Flutter Development
```bash
cd flutter_project
flutter run
```

### Kotlin Backend
```bash
cd kotlin_backend
./gradlew bootRun
```

The backend will start on `http://localhost:8080/api/v1`

## Project Structure

### Flutter
```
flutter_project/
├── lib/
│   ├── main.dart                 # App entry point
│   ├── config/
│   │   ├── app_config.dart      # Configuration
│   │   └── theme_config.dart    # Theme settings
│   ├── services/
│   │   ├── auth_service.dart    # Authentication
│   │   ├── notification_service.dart
│   │   └── model_viewer_service.dart
│   ├── models/
│   │   └── user_model.dart
│   ├── screens/
│   │   ├── splash_screen.dart
│   │   ├── login_screen.dart
│   │   ├── dashboard_screen.dart
│   │   └── model_viewer_screen.dart
│   └── widgets/
├── pubspec.yaml
└── firebase_options.dart
```

### Kotlin Backend
```
kotlin_backend/
├── src/main/kotlin/com/tyreguard/
│   ├── TyreGuardApplication.kt
│   ├── config/
│   │   └── SecurityConfig.kt
│   ├── security/
│   │   ├── JwtProvider.kt
│   │   └── JwtAuthenticationFilter.kt
│   ├── model/
│   │   ├── User.kt
│   │   ├── Analysis.kt
│   │   └── Notification.kt
│   ├── repository/
│   │   ├── UserRepository.kt
│   │   ├── AnalysisRepository.kt
│   │   └── NotificationRepository.kt
│   ├── service/
│   │   └── AuthService.kt
│   ├── controller/
│   │   └── AuthController.kt
│   └── dto/
│       └── AuthDTO.kt
├── src/main/resources/
│   └── application.yml
└── build.gradle.kts
```

## API Endpoints

### Authentication
- `POST /auth/signup` - User registration
- `POST /auth/login` - User login
- `POST /auth/refresh` - Token refresh
- `GET /auth/validate` - Token validation

### User Profile (Coming Soon)
- `GET /users/profile` - Get user profile
- `PUT /users/profile` - Update profile
- `POST /users/vehicles` - Add vehicle
- `GET /users/vehicles` - List vehicles

### Image Upload (Coming Soon)
- `POST /images/upload` - Upload tire image
- `GET /images/{imageId}` - Get image
- `DELETE /images/{imageId}` - Delete image

### Analysis (Coming Soon)
- `GET /analyses/{analysisId}` - Get analysis results
- `GET /tires/{tireId}/analyses` - Get tire analyses
- `POST /analyses/{analysisId}/3d-model` - Generate 3D model

## Testing

### Flutter Unit Tests
```bash
cd flutter_project
flutter test
```

### Kotlin Backend Tests
```bash
cd kotlin_backend
./gradlew test
```

## Deployment

### Flutter
```bash
# Android
flutter build apk --release

# iOS
flutter build ios --release
```

### Kotlin Backend
```bash
# Build JAR
./gradlew build

# Run with Docker
docker build -t tyreguard-backend .
docker run -p 8080:8080 tyreguard-backend
```

## Troubleshooting

### Flutter Dependency Issues
```bash
flutter clean
flutter pub get
```

### Kotlin Build Issues
```bash
./gradlew clean build
```

### Firebase Connection Issues
- Verify Firebase credentials are correctly set
- Check network connectivity
- Ensure Firebase project is active

## Next Steps

1. **Complete Authentication** - Implement password reset, biometric auth
2. **Image Upload** - Implement camera integration and image processing
3. **ML Analysis** - Integrate Google ML Kit for tire detection
4. **3D Model Generation** - Implement Tripo3D/Meshy API integration
5. **Notifications** - Set up Firebase Cloud Messaging
6. **Dashboard** - Build tire overview and health monitoring
7. **Testing** - Add unit and property-based tests

## Support

For issues or questions:
1. Check the spec documents in `.kiro/specs/tyreguard-rewrite/`
2. Review the design document for architecture details
3. Check the requirements for feature specifications

## License

TyreGuard © 2024. All rights reserved.

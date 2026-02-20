# TyreGuard - Tire Health Monitoring Application

A comprehensive tire health monitoring and analysis application combining modern Flutter UI with intelligent Kotlin backend services, integrated with cloud ML services and 3D model generation APIs.

## 🎯 Project Overview

TyreGuard enables users to:
- 📸 Capture tire images using device camera
- 🤖 Analyze tire condition using ML (Google ML Kit)
- 🎨 Visualize defects in interactive 3D models
- 📊 Monitor tire health with real-time alerts
- 🗺️ Find nearby service centers
- 📱 Manage multiple vehicles and profiles

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         Flutter UI Layer                │
│  • Login/Signup                         │
│  • Dashboard                            │
│  • Camera Integration                   │
│  • 3D Model Viewer                      │
│  • Notifications                        │
└────────────────┬────────────────────────┘
                 │ RESTful API (HTTPS)
                 │ JWT Authentication
┌────────────────▼────────────────────────┐
│      Kotlin Backend Layer               │
│  • Authentication Service               │
│  • Image Processing                     │
│  • ML Analysis Orchestration            │
│  • 3D Model Generation                  │
│  • Notification Service                 │
└────────────────┬────────────────────────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
    ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌──────────┐
│Firebase │ │Google   │ │Tripo3D/  │
│Services │ │ML Kit   │ │Meshy API │
└─────────┘ └─────────┘ └──────────┘
```

## 📋 Requirements

### Flutter
- Flutter SDK 3.0.0+
- Dart SDK 3.0.0+
- Android Studio or Xcode
- Firebase CLI

### Kotlin Backend
- Java 17+
- Gradle 8.0+
- PostgreSQL 14+ (or Firestore)
- Firebase Admin SDK

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone <repository-url>
cd tyreguard
```

### 2. Setup Flutter
```bash
cd flutter_project
flutter pub get
```

### 3. Setup Kotlin Backend
```bash
cd kotlin_backend
./gradlew build
```

### 4. Configure Firebase
- Create Firebase project
- Download credentials
- Update configuration files

### 5. Run Application
```bash
# Terminal 1: Backend
cd kotlin_backend
./gradlew bootRun

# Terminal 2: Flutter
cd flutter_project
flutter run
```

## 📁 Project Structure

```
tyreguard/
├── flutter_project/              # Flutter mobile app
│   ├── lib/
│   │   ├── main.dart
│   │   ├── config/              # Configuration
│   │   ├── services/            # Business logic
│   │   ├── models/              # Data models
│   │   ├── screens/             # UI screens
│   │   └── widgets/             # Reusable widgets
│   ├── pubspec.yaml
│   └── firebase_options.dart
│
├── kotlin_backend/              # Kotlin Spring Boot backend
│   ├── src/main/kotlin/
│   │   └── com/tyreguard/
│   │       ├── config/          # Configuration
│   │       ├── security/        # JWT & Auth
│   │       ├── model/           # Database entities
│   │       ├── repository/      # Data access
│   │       ├── service/         # Business logic
│   │       ├── controller/      # API endpoints
│   │       └── dto/             # Data transfer objects
│   ├── src/main/resources/
│   │   └── application.yml
│   └── build.gradle.kts
│
├── .kiro/specs/tyreguard-rewrite/
│   ├── requirements.md          # Feature requirements
│   ├── design.md                # Architecture & design
│   └── tasks.md                 # Implementation tasks
│
├── SETUP_GUIDE.md               # Detailed setup instructions
├── QUICK_START.md               # Quick start guide
└── README.md                    # This file
```

## 🔑 Key Features

### Authentication
- ✅ User registration with email validation
- ✅ Secure login with JWT tokens
- ✅ Password hashing with BCrypt
- ✅ Token refresh mechanism
- ✅ Session management

### Image Processing
- 📸 Camera integration
- 🖼️ Image compression
- 📤 Cloud storage integration
- 🔒 Secure upload with HTTPS

### ML Analysis
- 🤖 Tire detection using Google ML Kit
- 📊 Wear pattern analysis
- 🔍 Damage detection and classification
- 💯 Health score calculation (0-100)

### 3D Visualization
- 🎨 Interactive 3D model viewer
- 🔄 Gesture controls (zoom, rotate, pan)
- 📍 Defect highlighting
- 🌐 GLB model support

### Notifications
- 🔔 Real-time push notifications
- ⚠️ Health alerts
- 📋 Notification center
- ⚙️ User preferences

### Dashboard
- 🚗 Tire overview with health status
- 📈 Analysis history and trends
- 🎯 Quick action buttons
- 🗺️ Service center locator

## 🔐 Security

- **Authentication**: JWT tokens with 24-hour expiration
- **Password Security**: BCrypt hashing
- **Data Encryption**: HTTPS for all communications
- **Secure Storage**: Flutter secure storage for tokens
- **CORS**: Configured for authorized origins
- **Input Validation**: Server-side validation for all inputs

## 📊 API Endpoints

### Authentication
```
POST   /auth/signup              - User registration
POST   /auth/login               - User login
POST   /auth/refresh             - Token refresh
GET    /auth/validate            - Token validation
```

### User Profile (Coming Soon)
```
GET    /users/profile            - Get user profile
PUT    /users/profile            - Update profile
POST   /users/vehicles           - Add vehicle
GET    /users/vehicles           - List vehicles
```

### Image Upload (Coming Soon)
```
POST   /images/upload            - Upload tire image
GET    /images/{imageId}         - Get image
DELETE /images/{imageId}         - Delete image
```

### Analysis (Coming Soon)
```
GET    /analyses/{analysisId}    - Get analysis results
GET    /tires/{tireId}/analyses  - Get tire analyses
POST   /analyses/{analysisId}/3d-model - Generate 3D model
```

## 🧪 Testing

### Flutter Tests
```bash
cd flutter_project
flutter test
```

### Kotlin Backend Tests
```bash
cd kotlin_backend
./gradlew test
```

### Property-Based Tests
- Authentication token validity
- Image upload persistence
- Analysis result consistency
- 3D model generation round trip
- Notification delivery
- Dashboard data freshness

## 📈 Implementation Progress

### Completed ✅
- [x] Flutter project setup
- [x] Kotlin backend setup
- [x] Authentication service (backend)
- [x] Authentication UI (Flutter)
- [x] JWT token management
- [x] Security configuration
- [x] Database models
- [x] API endpoints (auth)

### In Progress 🔄
- [ ] User profile service
- [ ] Image upload service
- [ ] ML analysis service
- [ ] 3D model generation
- [ ] Notifications
- [ ] Dashboard features

### Planned 📋
- [ ] Biometric authentication
- [ ] Premium features
- [ ] Analytics
- [ ] Performance optimization
- [ ] Integration tests
- [ ] End-to-end tests

## 🛠️ Development

### Code Style
- **Flutter**: Follow Dart style guide
- **Kotlin**: Follow Kotlin conventions
- **Naming**: camelCase for variables, PascalCase for classes

### Commit Messages
```
feat: Add new feature
fix: Fix bug
docs: Update documentation
test: Add tests
refactor: Refactor code
```

### Branch Strategy
- `main` - Production ready
- `develop` - Development branch
- `feature/*` - Feature branches
- `bugfix/*` - Bug fix branches

## 📚 Documentation

- **Requirements**: `.kiro/specs/tyreguard-rewrite/requirements.md`
- **Design Document**: `.kiro/specs/tyreguard-rewrite/design.md`
- **Implementation Tasks**: `.kiro/specs/tyreguard-rewrite/tasks.md`
- **Setup Guide**: `SETUP_GUIDE.md`
- **Quick Start**: `QUICK_START.md`

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit pull request

## 📝 License

TyreGuard © 2024. All rights reserved.

## 📞 Support

For issues or questions:
1. Check the documentation
2. Review the spec documents
3. Check existing issues
4. Create a new issue with details

## 🎉 Acknowledgments

- Flutter team for the amazing framework
- Spring Boot for robust backend framework
- Firebase for cloud services
- Google ML Kit for ML capabilities
- Tripo3D/Meshy for 3D model generation

---

**Status**: Active Development 🚀

**Last Updated**: February 2024

**Version**: 1.0.0-alpha

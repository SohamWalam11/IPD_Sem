# TyreGuard - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Clone & Setup Flutter
```bash
cd flutter_project
flutter pub get
```

### Step 2: Setup Kotlin Backend
```bash
cd kotlin_backend
./gradlew build
```

### Step 3: Configure Firebase
1. Create Firebase project: https://console.firebase.google.com
2. Download credentials and update `firebase_options.dart`
3. Set environment variables for backend

### Step 4: Run the Application

**Terminal 1 - Backend:**
```bash
cd kotlin_backend
./gradlew bootRun
# Backend runs on http://localhost:8080/api/v1
```

**Terminal 2 - Flutter:**
```bash
cd flutter_project
flutter run
```

## 📱 Test the App

### Login Flow
1. Open app → See splash screen
2. Tap "Sign Up" → Create account
3. Enter email, password, name
4. Dashboard loads with tire overview

### Backend API Test
```bash
# Test authentication
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

## 🔧 Configuration

### Flutter Config (`lib/config/app_config.dart`)
```dart
static const String apiBaseUrl = 'https://api.tyreguard.com/api/v1';
static const String tripo3dApiKey = 'YOUR_KEY';
static const String meshyApiKey = 'YOUR_KEY';
```

### Backend Config (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tyreguard
    username: postgres
    password: password
```

## 📦 Key Features Implemented

✅ **Flutter UI**
- Modern login/signup screen with gradient design
- Dashboard with tire overview
- Splash screen with auto-login
- Theme system (light/dark mode)
- Notification service setup

✅ **Kotlin Backend**
- Spring Boot REST API
- JWT authentication
- Password hashing with BCrypt
- User management
- Database models for all entities
- CORS configuration

✅ **Security**
- JWT token generation and validation
- Secure password storage
- Token refresh mechanism
- Authentication middleware

## 🎯 Next Tasks

1. **Task 5**: User profile service
2. **Task 6**: Profile UI
3. **Task 7**: Image upload service
4. **Task 8**: Camera integration
5. **Task 9**: ML analysis service
6. **Task 11**: 3D model generation

## 🐛 Troubleshooting

### Flutter Dependency Error
```bash
flutter clean
flutter pub get
```

### Backend Won't Start
```bash
# Check Java version
java -version  # Should be 17+

# Clear Gradle cache
./gradlew clean
```

### Firebase Connection Issues
- Verify credentials in `firebase_options.dart`
- Check Firebase project is active
- Ensure network connectivity

## 📚 Documentation

- **Requirements**: `.kiro/specs/tyreguard-rewrite/requirements.md`
- **Design**: `.kiro/specs/tyreguard-rewrite/design.md`
- **Tasks**: `.kiro/specs/tyreguard-rewrite/tasks.md`
- **Setup Guide**: `SETUP_GUIDE.md`

## 💡 Tips

- Use `flutter run -v` for verbose output
- Use `./gradlew bootRun --debug` for backend debugging
- Check logs in `logs/tyreguard.log` for backend errors
- Use Postman to test API endpoints

## 🎉 You're Ready!

The foundation is solid. Start with the next tasks to add:
- Image upload & processing
- ML analysis
- 3D model generation
- Notifications
- Dashboard features

Happy coding! 🚀

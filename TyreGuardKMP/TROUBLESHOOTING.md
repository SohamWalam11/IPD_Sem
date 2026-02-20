# TyreGuard - Troubleshooting Guide

## Flutter Issues

### 1. Dependency Resolution Failed

**Error**: `Because tyreguard depends on gltf ^2.0.0 which doesn't match any versions...`

**Solution**:
```bash
# Update pubspec.yaml to use compatible packages
flutter clean
flutter pub get
```

The issue was with the `gltf` package. We've replaced it with `webview_flutter` for GLB rendering.

### 2. Firebase Configuration Issues

**Error**: `FirebaseException: [core/no-app] No Firebase App '[DEFAULT]' has been created`

**Solution**:
1. Verify `firebase_options.dart` has correct credentials
2. Check `google-services.json` (Android) is in correct location
3. Check `GoogleService-Info.plist` (iOS) is in correct location
4. Run: `flutter clean && flutter pub get`

### 3. Build Fails on Android

**Error**: `Gradle build failed`

**Solution**:
```bash
# Clear build cache
flutter clean

# Get dependencies
flutter pub get

# Build again
flutter build apk --debug
```

### 4. iOS Build Issues

**Error**: `Pod install failed` or `CocoaPods error`

**Solution**:
```bash
# Clean iOS build
cd ios
rm -rf Pods
rm Podfile.lock
cd ..

# Rebuild
flutter clean
flutter pub get
flutter run
```

### 5. Hot Reload Not Working

**Solution**:
```bash
# Stop the app
# Run with verbose output
flutter run -v

# Or restart the app
flutter run --no-fast-start
```

### 6. Secure Storage Issues

**Error**: `flutter_secure_storage: Failed to read value`

**Solution**:
- Android: Ensure app has INTERNET permission in AndroidManifest.xml
- iOS: Check Keychain sharing is enabled in Xcode

### 7. Camera Permission Denied

**Error**: `Camera permission denied`

**Solution**:
- Android: Add permissions to AndroidManifest.xml
- iOS: Add permissions to Info.plist
- Request permissions at runtime

### 8. Image Picker Not Working

**Error**: `Image picker returns null`

**Solution**:
```dart
// Ensure permissions are granted
final ImagePicker picker = ImagePicker();
final XFile? image = await picker.pickImage(source: ImageSource.camera);
```

## Kotlin Backend Issues

### 1. Build Fails

**Error**: `Gradle build failed`

**Solution**:
```bash
# Clean and rebuild
./gradlew clean build

# Check Java version
java -version  # Should be 17+

# Update Gradle
./gradlew wrapper --gradle-version 8.0
```

### 2. Port Already in Use

**Error**: `Address already in use: bind`

**Solution**:
```bash
# Change port in application.yml
server:
  port: 8081

# Or kill process using port 8080
# Linux/Mac:
lsof -i :8080
kill -9 <PID>

# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### 3. Database Connection Failed

**Error**: `org.postgresql.util.PSQLException: Connection refused`

**Solution**:
1. Verify PostgreSQL is running
2. Check connection string in `application.yml`
3. Verify database exists
4. Check credentials

```bash
# Test connection
psql -h localhost -U postgres -d tyreguard
```

### 4. Firebase Admin SDK Issues

**Error**: `FirebaseException: Failed to initialize Firebase`

**Solution**:
1. Verify `firebase-credentials.json` exists
2. Check `FIREBASE_CREDENTIALS_PATH` environment variable
3. Verify credentials have correct permissions

```bash
# Set environment variable
export FIREBASE_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

### 5. JWT Token Issues

**Error**: `Invalid JWT token` or `Token expired`

**Solution**:
- Verify JWT secret in `application.yml`
- Check token expiration time
- Ensure token is included in Authorization header

```bash
# Test with curl
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/auth/validate
```

### 6. CORS Issues

**Error**: `Access to XMLHttpRequest blocked by CORS policy`

**Solution**:
- Verify CORS configuration in `SecurityConfig.kt`
- Check allowed origins match Flutter app URL
- Ensure credentials are included in requests

### 7. Dependency Conflicts

**Error**: `Dependency conflict` or `Version mismatch`

**Solution**:
```bash
# Check dependencies
./gradlew dependencies

# Update dependencies
./gradlew dependencyUpdates

# Resolve conflicts in build.gradle.kts
```

### 8. Compilation Errors

**Error**: `Unresolved reference` or `Type mismatch`

**Solution**:
```bash
# Clean and rebuild
./gradlew clean build

# Check Kotlin version compatibility
# Ensure Java 17+ is installed
```

## API Communication Issues

### 1. Connection Timeout

**Error**: `Connection timeout`

**Solution**:
- Increase timeout in `app_config.dart`
- Check network connectivity
- Verify backend is running

```dart
static const Duration requestTimeout = Duration(seconds: 60);
```

### 2. 401 Unauthorized

**Error**: `401 Unauthorized`

**Solution**:
- Verify JWT token is valid
- Check token is included in Authorization header
- Refresh token if expired

### 3. 400 Bad Request

**Error**: `400 Bad Request`

**Solution**:
- Verify request body format
- Check required fields are included
- Validate input data

### 4. 500 Internal Server Error

**Error**: `500 Internal Server Error`

**Solution**:
- Check backend logs
- Verify database connection
- Check for null pointer exceptions

## Testing Issues

### 1. Unit Tests Fail

**Flutter**:
```bash
flutter test
```

**Kotlin**:
```bash
./gradlew test
```

### 2. Integration Tests Fail

**Solution**:
- Ensure backend is running
- Check test configuration
- Verify test data setup

## Performance Issues

### 1. App Slow to Start

**Solution**:
- Profile app with DevTools
- Check for heavy operations in main thread
- Optimize image loading

### 2. Backend Slow Response

**Solution**:
- Check database queries
- Add indexes to frequently queried columns
- Implement caching

### 3. High Memory Usage

**Solution**:
- Profile memory usage
- Dispose resources properly
- Implement image caching limits

## Debugging Tips

### Flutter Debugging

```bash
# Verbose output
flutter run -v

# Debug mode
flutter run --debug

# Profile mode
flutter run --profile

# Release mode
flutter run --release

# Check device logs
flutter logs
```

### Kotlin Debugging

```bash
# Debug mode
./gradlew bootRun --debug

# Check logs
tail -f logs/tyreguard.log

# Enable debug logging
logging:
  level:
    com.tyreguard: DEBUG
```

### Network Debugging

**Flutter**:
```dart
// Enable Dio logging
dio.interceptors.add(LoggingInterceptor());
```

**Kotlin**:
```yaml
logging:
  level:
    org.springframework.web: DEBUG
```

## Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `No Firebase App` | Firebase not initialized | Initialize Firebase in main.dart |
| `Connection refused` | Backend not running | Start backend with `./gradlew bootRun` |
| `Invalid JWT` | Token expired or invalid | Refresh token or login again |
| `CORS error` | CORS not configured | Update CORS settings in backend |
| `Port in use` | Port 8080 already used | Change port or kill process |
| `Database error` | Database not running | Start PostgreSQL |
| `Permission denied` | Missing permissions | Grant permissions in manifest |
| `Timeout` | Request too slow | Increase timeout or check network |

## Getting Help

1. **Check Documentation**
   - README.md
   - SETUP_GUIDE.md
   - QUICK_START.md

2. **Review Logs**
   - Flutter: `flutter logs`
   - Kotlin: `logs/tyreguard.log`

3. **Check Spec Documents**
   - `.kiro/specs/tyreguard-rewrite/requirements.md`
   - `.kiro/specs/tyreguard-rewrite/design.md`

4. **Debug with Tools**
   - Flutter DevTools: `flutter pub global run devtools`
   - Android Studio Debugger
   - Postman for API testing

5. **Search Issues**
   - GitHub Issues
   - Stack Overflow
   - Official Documentation

## Reporting Issues

When reporting issues, include:
1. Error message (full stack trace)
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Environment (OS, versions, etc.)
6. Logs (if applicable)

## Performance Optimization

### Flutter
- Use `const` constructors
- Implement lazy loading
- Cache images
- Use `RepaintBoundary`
- Profile with DevTools

### Kotlin
- Add database indexes
- Implement caching
- Use connection pooling
- Optimize queries
- Monitor with Spring Boot Actuator

---

**Last Updated**: February 2024
**Version**: 1.0.0

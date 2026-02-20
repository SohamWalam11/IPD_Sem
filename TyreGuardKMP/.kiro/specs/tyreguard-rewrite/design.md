# TyreGuard Application Rewrite - Design Document

## Overview

TyreGuard is a comprehensive tire health monitoring application with a modern Flutter UI and Kotlin backend services. The architecture follows a clean separation of concerns with three distinct layers:

1. **Flutter UI Layer**: Mobile application providing user interface, camera integration, and 3D visualization
2. **Kotlin Backend Layer**: RESTful API service handling business logic, ML integration, and cloud orchestration
3. **Cloud Services Layer**: External services for authentication, storage, ML, and 3D model generation

The system enables users to capture tire images, analyze them using machine learning, visualize defects in 3D, and receive real-time health monitoring alerts.

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Flutter UI Layer                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ • Login/Signup Screen                                    │   │
│  │ • Dashboard with Tire Overview                           │   │
│  │ • Camera Integration for Image Capture                   │   │
│  │ • 3D Model Viewer                                        │   │
│  │ • Real-time Notifications                                │   │
│  │ • User Profile & Settings                                │   │
│  │ • Service Center Locator                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬─────────────────────────────────────┘
                             │ RESTful API (HTTPS)
                             │ JWT Authentication
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Kotlin Backend Layer                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ • Authentication Controller                              │   │
│  │ • Image Upload & Processing                              │   │
│  │ • ML Analysis Orchestration                              │   │
│  │ • 3D Model Generation Polling                            │   │
│  │ • Notification Service                                   │   │
│  │ • User Profile Management                                │   │
│  │ • Service Center Locator                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬─────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ Cloud Services   │ │ ML Services      │ │ 3D Generation    │
│                  │ │                  │ │                  │
│ • Firebase Auth  │ │ • Google ML Kit  │ │ • Tripo3D API    │
│ • Firestore      │ │ • TensorFlow     │ │ • Meshy API      │
│ • Cloud Storage  │ │   Lite           │ │                  │
│ • Cloud Logging  │ │                  │ │                  │
│ • FCM            │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

### Communication Flow

```
User Action (Flutter)
        │
        ▼
Flutter UI Layer
        │
        ├─ Validates Input
        ├─ Caches Data Locally
        └─ Sends HTTPS Request with JWT
                │
                ▼
        Kotlin Backend
                │
                ├─ Validates JWT Token
                ├─ Processes Request
                ├─ Calls Cloud Services
                └─ Returns JSON Response
                │
                ▼
        Flutter UI
                │
                ├─ Updates Local Cache
                ├─ Updates UI
                └─ Displays Results to User
```

## Components and Interfaces

### 1. Flutter UI Layer Components

#### 1.1 Authentication Module
- **Login Screen**: Email/password input with modern design
- **Signup Screen**: User registration with validation
- **Password Reset**: Email-based password recovery
- **Session Management**: JWT token storage and refresh
- **Biometric Authentication**: Optional fingerprint/face recognition

#### 1.2 Dashboard Module
- **Tire Overview**: Display all monitored tires with health status
- **Health Indicators**: Color-coded status (green/yellow/red)
- **Quick Actions**: Buttons for capture, view details, find service centers
- **Recent History**: Timeline of recent analyses
- **Trends**: Visual charts showing tire health over time

#### 1.3 Camera Module
- **Image Capture**: Device camera integration
- **Image Preview**: Preview with retake/confirm options
- **Image Compression**: Optimize for upload
- **Gallery Integration**: Select images from device storage
- **Upload Progress**: Real-time upload status indicator

#### 1.4 Analysis Details Module
- **Health Score Display**: Prominent numerical score (0-100)
- **Wear Analysis**: Percentage wear and wear patterns
- **Damage Detection**: Types and severity of detected damage
- **Recommendations**: Actionable suggestions for user
- **3D Model Viewer**: Interactive 3D visualization
- **Defect Highlighting**: Tap to view specific defect details

#### 1.5 3D Viewer Module
- **Model Rendering**: Display GLB format 3D models
- **Gesture Controls**: Pinch zoom, swipe rotate, pan
- **Defect Highlighting**: Visual markers for detected defects
- **Defect Details**: Tap to show defect information
- **Loading State**: Progress indicator during model load
- **Error Handling**: Fallback to 2D view if model fails

#### 1.6 Notifications Module
- **Push Notifications**: Real-time alerts from backend
- **Notification Center**: History of all notifications
- **Alert Types**: Health alerts, service reminders, system notifications
- **Notification Actions**: Tap to navigate to relevant screen
- **Notification Settings**: User-configurable alert preferences

#### 1.7 Profile Module
- **User Information**: Name, email, phone number
- **Vehicle Information**: Make, model, year, tire size
- **Multiple Vehicles**: Switch between different vehicles
- **Edit Profile**: Update user and vehicle information
- **Account Settings**: Password change, email verification

#### 1.8 Settings Module
- **Notification Preferences**: Toggle alert types and thresholds
- **Theme Selection**: Light/dark mode
- **Location Services**: Enable/disable GPS
- **Alert Thresholds**: Customize health score thresholds
- **App Information**: Version, privacy policy, terms of service

#### 1.9 Service Center Locator Module
- **Map View**: Display nearby service centers
- **Location Services**: Request GPS permission and get user location
- **Service Center Details**: Address, phone, hours, ratings
- **Call/Navigate**: Integration with phone dialer and maps app
- **Search**: Filter by service type or distance

### 2. Kotlin Backend Layer Components

#### 2.1 Authentication Service
- **User Registration**: Create new user accounts
- **User Login**: Authenticate users and issue JWT tokens
- **Token Validation**: Verify JWT tokens on each request
- **Token Refresh**: Issue new tokens before expiration
- **Password Reset**: Handle password recovery requests
- **Session Management**: Track active sessions

#### 2.2 Image Processing Service
- **Image Upload Handler**: Receive and validate image uploads
- **Image Compression**: Optimize images for storage and processing
- **Image Storage**: Upload to Cloud Storage with metadata
- **Image Retrieval**: Fetch images for analysis or display
- **Image Cleanup**: Delete images after analysis or user request

#### 2.3 ML Analysis Service
- **Tire Detection**: Use ML to detect tire presence in image
- **Tire Segmentation**: Extract tire region from image
- **Wear Analysis**: Analyze tire wear patterns and percentage
- **Damage Detection**: Identify and classify damage types
- **Health Scoring**: Calculate overall tire health score (0-100)
- **Confidence Scoring**: Return confidence levels for detections
- **Error Handling**: Handle cases where tire is not detected

#### 2.4 3D Model Generation Service
- **Request Submission**: Submit image to 3D_Generator API
- **Job Tracking**: Store job ID and status in database
- **Polling Mechanism**: Periodically check job status
- **Model Download**: Download completed GLB files
- **Model Storage**: Upload GLB files to Cloud Storage
- **Status Updates**: Update database with completion status
- **Retry Logic**: Handle failed generation attempts

#### 2.5 Notification Service
- **Alert Generation**: Create alerts based on tire health
- **Push Notification**: Send FCM messages to Flutter app
- **Notification Persistence**: Store notification history
- **User Preferences**: Respect user notification settings
- **Alert Thresholds**: Trigger alerts based on configured thresholds

#### 2.6 User Profile Service
- **Profile Management**: Create, read, update user profiles
- **Vehicle Management**: Manage user's vehicle information
- **Profile Validation**: Validate user input data
- **Profile Persistence**: Store profiles in Real_Time_Database

#### 2.7 Service Center Service
- **Service Center Database**: Maintain list of service centers
- **Location Search**: Find service centers near user location
- **Service Center Details**: Return center information and ratings
- **Distance Calculation**: Calculate distance from user location

#### 2.8 API Gateway
- **Request Routing**: Route requests to appropriate services
- **Authentication Middleware**: Validate JWT tokens
- **Error Handling**: Standardize error responses
- **Request Logging**: Log all API requests
- **Rate Limiting**: Prevent abuse with rate limiting

### 3. Cloud Services Integration

#### 3.1 Firebase Authentication
- **User Registration**: Create user accounts
- **User Login**: Authenticate users
- **Password Reset**: Send password reset emails
- **Token Management**: Issue and validate tokens
- **User Metadata**: Store user information

#### 3.2 Firestore Database
- **User Profiles**: Store user account information
- **Vehicle Information**: Store vehicle details
- **Tire Analysis Results**: Store analysis history
- **3D Model Metadata**: Store model references and status
- **Notifications**: Store notification history
- **Service Centers**: Store service center information

#### 3.3 Cloud Storage
- **Image Storage**: Store uploaded tire images
- **3D Model Storage**: Store generated GLB files
- **Access Control**: Implement proper access controls
- **Encryption**: Ensure data is encrypted at rest

#### 3.4 Google ML Kit / TensorFlow Lite
- **On-Device ML**: Run models on Android device
- **Cloud ML**: Use cloud-based ML for complex analysis
- **Model Management**: Load and manage ML models
- **Inference**: Run predictions on tire images

#### 3.5 3D Generation APIs
- **Tripo3D API**: Submit images for 3D model generation
- **Meshy API**: Alternative 3D generation service
- **Job Management**: Track generation job status
- **Model Download**: Retrieve completed models

#### 3.6 Firebase Cloud Messaging
- **Push Notifications**: Send notifications to Flutter app
- **Message Delivery**: Ensure reliable message delivery
- **Topic Subscriptions**: Subscribe users to notification topics

#### 3.7 Cloud Logging
- **Error Logging**: Log application errors
- **Request Logging**: Log API requests and responses
- **Performance Monitoring**: Track performance metrics
- **Debugging**: Provide logs for debugging issues

## Data Models

### User Model
```
User {
  id: String (unique identifier)
  email: String (unique)
  passwordHash: String (hashed)
  firstName: String
  lastName: String
  phoneNumber: String
  createdAt: Timestamp
  updatedAt: Timestamp
  profileImageUrl: String (optional)
  vehicles: List<String> (vehicle IDs)
  preferences: UserPreferences
}
```

### UserPreferences Model
```
UserPreferences {
  notificationsEnabled: Boolean
  alertThreshold: Int (0-100)
  theme: String (light/dark)
  locationEnabled: Boolean
  alertTypes: List<String> (health, service, system)
}
```

### Vehicle Model
```
Vehicle {
  id: String (unique identifier)
  userId: String (foreign key)
  make: String
  model: String
  year: Int
  tireSize: String
  createdAt: Timestamp
  updatedAt: Timestamp
  tires: List<String> (tire IDs)
}
```

### Tire Model
```
Tire {
  id: String (unique identifier)
  vehicleId: String (foreign key)
  position: String (front-left, front-right, rear-left, rear-right)
  createdAt: Timestamp
  updatedAt: Timestamp
  analyses: List<String> (analysis IDs)
  currentHealthScore: Int (0-100)
  lastAnalysisDate: Timestamp
}
```

### TireAnalysis Model
```
TireAnalysis {
  id: String (unique identifier)
  tireId: String (foreign key)
  imageUrl: String (Cloud Storage path)
  analysisDate: Timestamp
  healthScore: Int (0-100)
  wearPercentage: Int (0-100)
  wearPatterns: List<String> (descriptions)
  detectedDamages: List<Damage>
  recommendations: List<String>
  modelGenerationJobId: String (optional)
  modelUrl: String (optional, GLB file path)
  confidence: Float (0-1)
  mlServiceUsed: String (ML Kit, TensorFlow Lite, etc.)
}
```

### Damage Model
```
Damage {
  id: String
  type: String (puncture, cut, bulge, sidewall, etc.)
  severity: String (low, medium, high, critical)
  location: String (description of location on tire)
  description: String
  confidence: Float (0-1)
}
```

### Notification Model
```
Notification {
  id: String (unique identifier)
  userId: String (foreign key)
  type: String (health_alert, service_reminder, system)
  title: String
  message: String
  tireId: String (optional, foreign key)
  analysisId: String (optional, foreign key)
  createdAt: Timestamp
  readAt: Timestamp (optional)
  actionUrl: String (optional, deep link)
}
```

### ServiceCenter Model
```
ServiceCenter {
  id: String (unique identifier)
  name: String
  address: String
  latitude: Float
  longitude: Float
  phoneNumber: String
  hours: String
  services: List<String>
  rating: Float (0-5)
  reviewCount: Int
}
```

### 3DModelJob Model
```
3DModelJob {
  id: String (unique identifier)
  analysisId: String (foreign key)
  externalJobId: String (from 3D_Generator API)
  status: String (pending, processing, completed, failed)
  createdAt: Timestamp
  completedAt: Timestamp (optional)
  modelUrl: String (optional, Cloud Storage path)
  errorMessage: String (optional)
  retryCount: Int
}
```

## API Endpoints

### Authentication Endpoints

```
POST /api/v1/auth/signup
Request: { email, password, firstName, lastName }
Response: { userId, token, expiresIn }

POST /api/v1/auth/login
Request: { email, password }
Response: { userId, token, expiresIn }

POST /api/v1/auth/refresh
Request: { refreshToken }
Response: { token, expiresIn }

POST /api/v1/auth/password-reset
Request: { email }
Response: { message }

POST /api/v1/auth/password-reset-confirm
Request: { token, newPassword }
Response: { message }
```

### Image Upload Endpoints

```
POST /api/v1/images/upload
Request: multipart/form-data { image, tireId }
Response: { imageId, imageUrl, analysisJobId }

GET /api/v1/images/{imageId}
Response: { imageId, imageUrl, uploadedAt }

DELETE /api/v1/images/{imageId}
Response: { message }
```

### Analysis Endpoints

```
GET /api/v1/analyses/{analysisId}
Response: { analysisId, healthScore, wearPercentage, damages, recommendations }

GET /api/v1/tires/{tireId}/analyses
Response: { analyses: [TireAnalysis] }

POST /api/v1/analyses/{analysisId}/3d-model
Request: {}
Response: { jobId, status }

GET /api/v1/3d-models/{jobId}
Response: { jobId, status, modelUrl, progress }
```

### User Profile Endpoints

```
GET /api/v1/users/profile
Response: { userId, email, firstName, lastName, phoneNumber, vehicles }

PUT /api/v1/users/profile
Request: { firstName, lastName, phoneNumber }
Response: { message }

POST /api/v1/users/vehicles
Request: { make, model, year, tireSize }
Response: { vehicleId }

GET /api/v1/users/vehicles
Response: { vehicles: [Vehicle] }

PUT /api/v1/users/vehicles/{vehicleId}
Request: { make, model, year, tireSize }
Response: { message }

DELETE /api/v1/users/vehicles/{vehicleId}
Response: { message }
```

### Notification Endpoints

```
GET /api/v1/notifications
Response: { notifications: [Notification] }

PUT /api/v1/notifications/{notificationId}/read
Response: { message }

PUT /api/v1/users/preferences
Request: { notificationsEnabled, alertThreshold, theme, alertTypes }
Response: { message }
```

### Service Center Endpoints

```
GET /api/v1/service-centers/nearby
Query: { latitude, longitude, radius }
Response: { serviceCenters: [ServiceCenter] }

GET /api/v1/service-centers/{centerId}
Response: { ServiceCenter }
```

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Authentication Token Validity
*For any* user login, the returned JWT token should be valid and decodable, and should contain the correct user ID and expiration time.
**Validates: Requirements 1.4, 1.6, 10.2, 10.3**

### Property 2: Image Upload Persistence
*For any* uploaded tire image, querying the image storage should return the same image data that was uploaded.
**Validates: Requirements 2.3, 2.8, 13.1**

### Property 3: Analysis Result Consistency
*For any* tire image, running analysis multiple times on the same image should produce consistent health scores (within acceptable variance).
**Validates: Requirements 3.2, 3.3, 3.4**

### Property 4: 3D Model Generation Round Trip
*For any* completed 3D model generation job, the downloaded GLB file should be valid and renderable in the 3D viewer.
**Validates: Requirements 4.5, 4.6**

### Property 5: Notification Delivery
*For any* alert created in the backend, a corresponding push notification should be delivered to the user's device.
**Validates: Requirements 5.1, 5.2, 5.3**

### Property 6: Dashboard Data Freshness
*For any* tire on the dashboard, the displayed health score should match the most recent analysis result in the database.
**Validates: Requirements 6.2, 6.3, 6.6**

### Property 7: User Profile Isolation
*For any* user, querying their profile should only return data associated with that user, not other users' data.
**Validates: Requirements 8.2, 8.3, 14.7**

### Property 8: API Authentication Enforcement
*For any* protected API endpoint, requests without a valid JWT token should be rejected with a 401 Unauthorized response.
**Validates: Requirements 10.3, 10.4, 10.5**

### Property 9: 3D Model Polling Termination
*For any* 3D model generation job, polling should eventually terminate (either with success or failure) within the maximum retry limit.
**Validates: Requirements 11.8, 11.9**

### Property 10: Error Message Clarity
*For any* failed operation, the error response should include a user-friendly message explaining the issue and suggested actions.
**Validates: Requirements 12.1, 12.2, 12.3**

### Property 11: Offline Data Availability
*For any* previously loaded tire analysis, the data should be accessible from local cache even when the device is offline.
**Validates: Requirements 13.1, 13.2**

### Property 12: Password Security
*For any* user password stored in the database, the stored value should be a cryptographic hash, not plain text.
**Validates: Requirements 14.2**

### Property 13: Token Expiration Enforcement
*For any* expired JWT token, API requests using that token should be rejected with a 401 Unauthorized response.
**Validates: Requirements 14.3, 14.4**

### Property 14: Image Encryption in Transit
*For any* image uploaded to the backend, the transmission should use HTTPS encryption.
**Validates: Requirements 14.5, 14.8**

### Property 15: API Response Time
*For any* API request, the response should be received within the configured timeout (e.g., 30 seconds).
**Validates: Requirements 15.8**

## Error Handling

### Error Categories

1. **Authentication Errors**
   - Invalid credentials: Return 401 with message "Invalid email or password"
   - Token expired: Return 401 with message "Session expired, please login again"
   - Missing token: Return 401 with message "Authorization required"

2. **Validation Errors**
   - Invalid email format: Return 400 with message "Invalid email format"
   - Weak password: Return 400 with message "Password must be at least 8 characters"
   - Missing required fields: Return 400 with message "Missing required field: {fieldName}"

3. **Image Processing Errors**
   - No tire detected: Return 422 with message "No tire detected in image. Please capture a clear tire image"
   - Image too small: Return 422 with message "Image resolution too low. Please use a higher quality image"
   - Upload failed: Return 500 with message "Image upload failed. Please try again"

4. **ML Analysis Errors**
   - Analysis timeout: Return 504 with message "Analysis took too long. Please try again"
   - ML service unavailable: Return 503 with message "Analysis service temporarily unavailable"
   - Invalid model: Return 500 with message "ML model error. Please contact support"

5. **3D Model Generation Errors**
   - Generation failed: Return 500 with message "3D model generation failed. Please try again"
   - Generation timeout: Return 504 with message "3D model generation took too long"
   - Model download failed: Return 500 with message "Failed to download 3D model"

6. **Database Errors**
   - User not found: Return 404 with message "User not found"
   - Vehicle not found: Return 404 with message "Vehicle not found"
   - Analysis not found: Return 404 with message "Analysis not found"

7. **Network Errors**
   - Connection timeout: Return 504 with message "Connection timeout. Please check your internet"
   - Service unavailable: Return 503 with message "Service temporarily unavailable"

### Error Response Format

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly error message",
    "details": "Technical details for debugging",
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "unique-request-id"
  }
}
```

## Testing Strategy

### Unit Testing

Unit tests verify specific examples, edge cases, and error conditions:

1. **Authentication Tests**
   - Valid login with correct credentials
   - Invalid login with wrong password
   - Signup with existing email
   - Password reset flow
   - Token refresh mechanism

2. **Image Processing Tests**
   - Image compression reduces file size
   - Image upload with valid file
   - Image upload with invalid format
   - Image retrieval returns correct data

3. **ML Analysis Tests**
   - Tire detection on valid tire image
   - Tire detection on non-tire image
   - Wear percentage calculation
   - Damage classification
   - Health score calculation

4. **3D Model Generation Tests**
   - Job submission returns valid job ID
   - Job status polling returns correct status
   - Model download on completion
   - Retry logic on failure

5. **Notification Tests**
   - Alert creation triggers notification
   - Notification delivery to correct user
   - Notification preferences respected
   - Notification history stored

6. **User Profile Tests**
   - Profile creation with valid data
   - Profile update with new values
   - Vehicle addition and removal
   - Multiple vehicle management

7. **API Tests**
   - Endpoint returns correct status code
   - Response format matches specification
   - Error responses include error details
   - Rate limiting enforced

### Property-Based Testing

Property-based tests verify universal properties across all inputs:

1. **Authentication Property Tests**
   - Property 1: Authentication Token Validity
   - Property 8: API Authentication Enforcement
   - Property 13: Token Expiration Enforcement

2. **Data Persistence Property Tests**
   - Property 2: Image Upload Persistence
   - Property 11: Offline Data Availability

3. **Analysis Property Tests**
   - Property 3: Analysis Result Consistency
   - Property 4: 3D Model Generation Round Trip

4. **Notification Property Tests**
   - Property 5: Notification Delivery
   - Property 6: Dashboard Data Freshness

5. **Security Property Tests**
   - Property 7: User Profile Isolation
   - Property 12: Password Security
   - Property 14: Image Encryption in Transit

6. **Performance Property Tests**
   - Property 9: 3D Model Polling Termination
   - Property 15: API Response Time

7. **Error Handling Property Tests**
   - Property 10: Error Message Clarity

### Test Configuration

- **Minimum iterations**: 100 per property test
- **Test framework**: 
  - Flutter: `test` package with `property_test` or `quickcheck`
  - Kotlin: `property-based-testing` or `jqwik`
- **Test tags**: Each test tagged with feature name and property number
- **Coverage target**: Minimum 80% code coverage
- **CI/CD integration**: Tests run on every commit

### Testing Approach

- **Unit tests** focus on specific examples and edge cases
- **Property tests** verify universal correctness properties
- **Integration tests** verify component interactions
- **End-to-end tests** verify complete user workflows
- **Performance tests** verify response time requirements
- **Security tests** verify authentication and data protection


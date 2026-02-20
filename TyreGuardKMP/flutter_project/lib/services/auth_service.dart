import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:dio/dio.dart';
import '../config/app_config.dart';
import '../models/user_model.dart';

class AuthService {
  static final AuthService _instance = AuthService._internal();
  
  final FirebaseAuth _firebaseAuth = FirebaseAuth.instance;
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  final Dio _dio = Dio();
  
  String? _jwtToken;
  UserModel? _currentUser;

  factory AuthService() {
    return _instance;
  }

  AuthService._internal();

  // Getters
  String? get jwtToken => _jwtToken;
  UserModel? get currentUser => _currentUser;
  bool get isAuthenticated => _jwtToken != null && _currentUser != null;

  // Initialize auth service
  Future<void> initialize() async {
    _jwtToken = await _secureStorage.read(key: 'jwt_token');
    if (_jwtToken != null) {
      await _validateToken();
    }
  }

  // Sign up with email and password
  Future<UserModel> signUp({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    try {
      final response = await _dio.post(
        '${AppConfig.apiBaseUrl}/auth/signup',
        data: {
          'email': email,
          'password': password,
          'firstName': firstName,
          'lastName': lastName,
        },
        options: Options(
          contentType: Headers.jsonContentType,
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
        ),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        _jwtToken = response.data['token'];
        await _secureStorage.write(key: 'jwt_token', value: _jwtToken!);
        
        _currentUser = UserModel.fromJson(response.data['user']);
        return _currentUser!;
      } else {
        throw Exception('Sign up failed: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw _handleDioError(e);
    }
  }

  // Login with email and password
  Future<UserModel> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await _dio.post(
        '${AppConfig.apiBaseUrl}/auth/login',
        data: {
          'email': email,
          'password': password,
        },
        options: Options(
          contentType: Headers.jsonContentType,
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
        ),
      );

      if (response.statusCode == 200) {
        _jwtToken = response.data['token'];
        await _secureStorage.write(key: 'jwt_token', value: _jwtToken!);
        
        _currentUser = UserModel.fromJson(response.data['user']);
        return _currentUser!;
      } else {
        throw Exception('Login failed: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw _handleDioError(e);
    }
  }

  // Logout
  Future<void> logout() async {
    try {
      await _secureStorage.delete(key: 'jwt_token');
      _jwtToken = null;
      _currentUser = null;
    } catch (e) {
      throw Exception('Logout failed: $e');
    }
  }

  // Refresh token
  Future<void> refreshToken() async {
    try {
      final response = await _dio.post(
        '${AppConfig.apiBaseUrl}/auth/refresh',
        options: Options(
          headers: {'Authorization': 'Bearer $_jwtToken'},
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
        ),
      );

      if (response.statusCode == 200) {
        _jwtToken = response.data['token'];
        await _secureStorage.write(key: 'jwt_token', value: _jwtToken!);
      } else {
        throw Exception('Token refresh failed');
      }
    } on DioException catch (e) {
      throw _handleDioError(e);
    }
  }

  // Password reset
  Future<void> resetPassword(String email) async {
    try {
      final response = await _dio.post(
        '${AppConfig.apiBaseUrl}/auth/password-reset',
        data: {'email': email},
        options: Options(
          contentType: Headers.jsonContentType,
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
        ),
      );

      if (response.statusCode != 200) {
        throw Exception('Password reset failed: ${response.statusMessage}');
      }
    } on DioException catch (e) {
      throw _handleDioError(e);
    }
  }

  // Validate token
  Future<void> _validateToken() async {
    try {
      final response = await _dio.get(
        '${AppConfig.apiBaseUrl}/auth/validate',
        options: Options(
          headers: {'Authorization': 'Bearer $_jwtToken'},
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
        ),
      );

      if (response.statusCode == 200) {
        _currentUser = UserModel.fromJson(response.data['user']);
      } else {
        await logout();
      }
    } on DioException catch (e) {
      await logout();
      throw _handleDioError(e);
    }
  }

  // Handle Dio errors
  Exception _handleDioError(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout) {
      return Exception('Connection timeout. Please check your internet.');
    } else if (e.type == DioExceptionType.receiveTimeout) {
      return Exception('Request timeout. Please try again.');
    } else if (e.response?.statusCode == 401) {
      return Exception('Invalid credentials. Please try again.');
    } else if (e.response?.statusCode == 400) {
      return Exception(e.response?.data['message'] ?? 'Invalid request.');
    } else if (e.response?.statusCode == 500) {
      return Exception('Server error. Please try again later.');
    } else {
      return Exception('An error occurred: ${e.message}');
    }
  }
}

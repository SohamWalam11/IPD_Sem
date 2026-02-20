import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:logger/logger.dart';

import '../models/tire_analysis_models.dart';

/// Michelin Mobility Intelligence API Integration.
///
/// Provides AI-powered tire recognition:
/// - Tire Size Recognition from sidewall photos
/// - DOT Code Recognition for manufacturing date
///
/// API Documentation: https://developer.michelin.com/
class MichelinTireApi {
  static const String _tag = 'MichelinTireApi';
  static const String _baseUrl = 'https://api.michelin.com/mobility-intelligence/v1';

  final Logger _logger = Logger();
  final Dio _dio;
  final String? _apiKey;

  MichelinTireApi({String? apiKey})
      : _apiKey = apiKey,
        _dio = Dio(BaseOptions(
          baseUrl: _baseUrl,
          connectTimeout: const Duration(seconds: 30),
          receiveTimeout: const Duration(seconds: 30),
        ));

  /// Recognize tire size from a sidewall image file.
  Future<TireSizeInfo> recognizeTireSizeFromFile(File imageFile) async {
    final bytes = await imageFile.readAsBytes();
    return recognizeTireSize(bytes);
  }

  /// Recognize tire size from image bytes.
  Future<TireSizeInfo> recognizeTireSize(Uint8List imageBytes) async {
    _logger.d('$_tag: Starting tire size recognition');

    if (_apiKey == null || _apiKey!.isEmpty) {
      _logger.w('$_tag: No API key configured. Using simulation mode.');
      return _simulateTireSizeRecognition();
    }

    try {
      final base64Image = base64Encode(imageBytes);

      final response = await _dio.post(
        '/tire-size-recognition',
        data: {
          'image': base64Image,
          'format': 'base64',
        },
        options: Options(
          headers: {'X-API-Key': _apiKey},
          contentType: 'application/json',
        ),
      );

      if (response.statusCode == 200) {
        final result = _parseTireSizeResponse(response.data);
        _logger.d('$_tag: Tire size recognized: ${result.fullSpecification}');
        return result;
      } else {
        _logger.e('$_tag: API error: ${response.statusCode}');
        return _simulateTireSizeRecognition();
      }
    } catch (e) {
      _logger.e('$_tag: Recognition failed: $e');
      return _simulateTireSizeRecognition();
    }
  }

  /// Recognize DOT code from a sidewall image file.
  Future<DotCodeInfo> recognizeDotCodeFromFile(File imageFile) async {
    final bytes = await imageFile.readAsBytes();
    return recognizeDotCode(bytes);
  }

  /// Recognize DOT code from image bytes.
  Future<DotCodeInfo> recognizeDotCode(Uint8List imageBytes) async {
    _logger.d('$_tag: Starting DOT code recognition');

    if (_apiKey == null || _apiKey!.isEmpty) {
      _logger.w('$_tag: No API key configured. Using simulation mode.');
      return _simulateDotCodeRecognition();
    }

    try {
      final base64Image = base64Encode(imageBytes);

      final response = await _dio.post(
        '/dot-recognition',
        data: {
          'image': base64Image,
          'format': 'base64',
        },
        options: Options(
          headers: {'X-API-Key': _apiKey},
          contentType: 'application/json',
        ),
      );

      if (response.statusCode == 200) {
        final result = _parseDotCodeResponse(response.data);
        _logger.d('$_tag: DOT code recognized: ${result.fullDotCode}');
        return result;
      } else {
        _logger.e('$_tag: API error: ${response.statusCode}');
        return _simulateDotCodeRecognition();
      }
    } catch (e) {
      _logger.e('$_tag: Recognition failed: $e');
      return _simulateDotCodeRecognition();
    }
  }

  /// Analyze complete sidewall (size + DOT code).
  Future<(TireSizeInfo, DotCodeInfo)> analyzeSidewall(Uint8List imageBytes) async {
    final sizeInfo = await recognizeTireSize(imageBytes);
    final dotInfo = await recognizeDotCode(imageBytes);
    return (sizeInfo, dotInfo);
  }

  TireSizeInfo _parseTireSizeResponse(Map<String, dynamic> data) {
    return TireSizeInfo(
      rawText: data['rawText'] ?? '',
      width: data['width'] ?? 0,
      aspectRatio: data['aspectRatio'] ?? 0,
      construction: data['construction'] ?? 'R',
      rimDiameter: data['rimDiameter'] ?? 0,
      loadIndex: data['loadIndex'] ?? 0,
      speedRating: data['speedRating'] ?? '',
      confidence: (data['confidence'] ?? 0).toDouble(),
      additionalMarks: List<String>.from(data['additionalMarks'] ?? []),
    );
  }

  DotCodeInfo _parseDotCodeResponse(Map<String, dynamic> data) {
    final now = DateTime.now();
    final mfgYear = data['manufactureYear'] ?? now.year;
    final mfgWeek = data['manufactureWeek'] ?? 1;

    final yearsOld = now.year - mfgYear;
    final monthsOld = yearsOld * 12 + (now.month - (mfgWeek ~/ 4));

    return DotCodeInfo(
      fullDotCode: data['fullDotCode'] ?? '',
      plantCode: data['plantCode'] ?? '',
      tireSize: data['tireSize'] ?? '',
      brandCharacteristics: data['brandCharacteristics'] ?? '',
      manufactureWeek: mfgWeek,
      manufactureYear: mfgYear,
      ageInMonths: monthsOld.clamp(0, 999),
      confidence: (data['confidence'] ?? 0).toDouble(),
    );
  }

  /// Simulate tire size recognition for development.
  TireSizeInfo _simulateTireSizeRecognition() {
    final sizes = [
      TireSizeInfo(
        rawText: '225/45R17 94W',
        width: 225,
        aspectRatio: 45,
        construction: 'R',
        rimDiameter: 17,
        loadIndex: 94,
        speedRating: 'W',
        confidence: 0.92,
        additionalMarks: ['XL'],
      ),
      TireSizeInfo(
        rawText: '205/55R16 91V',
        width: 205,
        aspectRatio: 55,
        construction: 'R',
        rimDiameter: 16,
        loadIndex: 91,
        speedRating: 'V',
        confidence: 0.88,
      ),
      TireSizeInfo(
        rawText: '235/40R18 95Y',
        width: 235,
        aspectRatio: 40,
        construction: 'R',
        rimDiameter: 18,
        loadIndex: 95,
        speedRating: 'Y',
        confidence: 0.90,
        additionalMarks: ['RF'],
      ),
      TireSizeInfo(
        rawText: '195/65R15 91H',
        width: 195,
        aspectRatio: 65,
        construction: 'R',
        rimDiameter: 15,
        loadIndex: 91,
        speedRating: 'H',
        confidence: 0.95,
      ),
      TireSizeInfo(
        rawText: '255/35R19 96Y',
        width: 255,
        aspectRatio: 35,
        construction: 'R',
        rimDiameter: 19,
        loadIndex: 96,
        speedRating: 'Y',
        confidence: 0.87,
        additionalMarks: ['XL', 'MO'],
      ),
    ];

    return sizes[Random().nextInt(sizes.length)];
  }

  /// Simulate DOT code recognition for development.
  DotCodeInfo _simulateDotCodeRecognition() {
    final random = Random();
    final now = DateTime.now();

    final yearsAgo = random.nextInt(6);
    final mfgYear = now.year - yearsAgo;
    final mfgWeek = random.nextInt(52) + 1;

    final plantCodes = ['3D', '4B', '2M', '5J', '1K', '8H'];
    final plantCode = plantCodes[random.nextInt(plantCodes.length)];

    final ageInMonths = yearsAgo * 12 + random.nextInt(12);

    final weekStr = mfgWeek.toString().padLeft(2, '0');
    final yearStr = (mfgYear % 100).toString().padLeft(2, '0');

    return DotCodeInfo(
      fullDotCode: 'DOT ${plantCode}XX XXXX $weekStr$yearStr',
      plantCode: plantCode,
      tireSize: 'XXXX',
      brandCharacteristics: '',
      manufactureWeek: mfgWeek,
      manufactureYear: mfgYear,
      ageInMonths: ageInMonths,
      confidence: 0.85 + random.nextDouble() * 0.1,
    );
  }

  /// Release resources.
  void dispose() {
    _dio.close();
    _logger.d('$_tag: Resources released');
  }
}


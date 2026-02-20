import 'dart:io';
import 'dart:typed_data';

import 'package:logger/logger.dart';
import 'package:uuid/uuid.dart';

import '../models/tire_analysis_models.dart';
import 'michelin_tire_api.dart';
import 'tire_defect_classifier.dart';
import 'tread_depth_scanner.dart';

/// Comprehensive Tire Analysis Service
///
/// Integrates multiple data sources for complete tire health assessment:
/// 1. TreadDepthScanner - Tread depth measurement (Anyline SDK)
/// 2. MichelinTireApi - Tire size and DOT code recognition
/// 3. TireDefectClassifier - Custom TFLite model for defect detection
///
/// Provides unified analysis with health scoring and recommendations.
class ComprehensiveTireAnalysisService {
  static const String _tag = 'TireAnalysisService';

  // Weight factors for health score calculation
  static const double _treadWeight = 0.35;
  static const double _defectWeight = 0.40;
  static const double _ageWeight = 0.25;

  final Logger _logger = Logger();
  final Uuid _uuid = const Uuid();

  late final TreadDepthScanner _treadScanner;
  late final MichelinTireApi _michelinApi;
  late final TireDefectClassifier _defectClassifier;

  bool _isInitialized = false;

  /// Check if service is ready.
  bool get isReady => _isInitialized;

  /// Initialize all analysis components.
  Future<bool> initialize({
    String? anylineLicenseKey,
    String? michelinApiKey,
  }) async {
    try {
      _logger.d('$_tag: Initializing Comprehensive Tire Analysis Service');

      // Initialize tread depth scanner
      _treadScanner = TreadDepthScanner();
      await _treadScanner.initialize(licenseKey: anylineLicenseKey);

      // Initialize Michelin API
      _michelinApi = MichelinTireApi(apiKey: michelinApiKey);

      // Initialize TFLite classifier
      _defectClassifier = TireDefectClassifier();
      final classifierReady = await _defectClassifier.initialize();

      if (!classifierReady) {
        _logger.w('$_tag: TFLite classifier not ready - defect detection limited');
      }

      _isInitialized = true;
      _logger.d('$_tag: Service initialized successfully');
      return true;
    } catch (e) {
      _logger.e('$_tag: Initialization failed: $e');
      return false;
    }
  }

  /// Perform comprehensive tire analysis.
  ///
  /// [treadImage] - Required image of tire tread
  /// [sidewallImage] - Optional image of sidewall for size/DOT recognition
  Future<ComprehensiveTireAnalysis> analyzeTire({
    required Uint8List treadImage,
    Uint8List? sidewallImage,
  }) async {
    if (!_isInitialized) {
      throw StateError('Service not initialized. Call initialize() first.');
    }

    _logger.d('$_tag: Starting comprehensive tire analysis');
    final analysisId = _uuid.v4();

    // Run analyses in parallel
    final results = await Future.wait([
      _analyzeTread(treadImage),
      _detectDefects(treadImage),
      if (sidewallImage != null) _analyzeSidewall(sidewallImage),
    ]);

    final treadResult = results[0] as TreadDepthResult?;
    final defects = results[1] as List<TireDefect>;

    TireSizeInfo? tireSizeInfo;
    DotCodeInfo? dotCodeInfo;

    if (sidewallImage != null && results.length > 2) {
      final sidewallResult = results[2] as (TireSizeInfo?, DotCodeInfo?);
      tireSizeInfo = sidewallResult.$1;
      dotCodeInfo = sidewallResult.$2;
    }

    // Calculate health score
    final healthScore = _calculateHealthScore(treadResult, defects, dotCodeInfo);

    // Determine overall status
    final overallStatus = _determineOverallStatus(healthScore, treadResult, defects);

    // Generate concerns
    final concerns = _generateConcerns(treadResult, defects, dotCodeInfo);

    // Generate recommendations
    final recommendations = _generateRecommendations(treadResult, defects, dotCodeInfo);

    // Determine action required
    final actionRequired = _determineActionRequired(overallStatus, treadResult, defects);

    // Estimate cost
    final costRange = _estimateCostRange(recommendations);

    final analysis = ComprehensiveTireAnalysis(
      id: analysisId,
      treadDepth: treadResult,
      tireSizeInfo: tireSizeInfo,
      dotCodeInfo: dotCodeInfo,
      detectedDefects: defects,
      overallHealthScore: healthScore,
      overallStatus: overallStatus,
      primaryConcerns: concerns,
      recommendations: recommendations,
      actionRequired: actionRequired,
      estimatedCostRange: costRange,
    );

    _logger.d('$_tag: Analysis complete: score=$healthScore, status=${overallStatus.displayName}');
    return analysis;
  }

  /// Quick defect scan using only TFLite model.
  Future<List<TireDefect>> quickDefectScan(Uint8List imageBytes) async {
    return _detectDefects(imageBytes);
  }

  /// Measure tread depth only.
  Future<TreadDepthResult> measureTreadDepth(Uint8List imageBytes) async {
    if (!_treadScanner.isReady) {
      await _treadScanner.initialize();
    }
    return _treadScanner.scanFromBytes(imageBytes);
  }

  /// Recognize tire size from sidewall image.
  Future<TireSizeInfo> recognizeTireSize(Uint8List imageBytes) async {
    return _michelinApi.recognizeTireSize(imageBytes);
  }

  /// Recognize DOT code from sidewall image.
  Future<DotCodeInfo> recognizeDotCode(Uint8List imageBytes) async {
    return _michelinApi.recognizeDotCode(imageBytes);
  }

  /// Generate heat map from tread scan.
  TreadHeatMap? generateTreadHeatMap(TreadDepthResult? result) {
    if (result == null) return null;
    return _treadScanner.generateHeatMap(result);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Private Analysis Methods
  // ═══════════════════════════════════════════════════════════════════════════

  Future<TreadDepthResult?> _analyzeTread(Uint8List imageBytes) async {
    try {
      return await _treadScanner.scanFromBytes(imageBytes);
    } catch (e) {
      _logger.e('$_tag: Tread analysis failed: $e');
      return null;
    }
  }

  Future<List<TireDefect>> _detectDefects(Uint8List imageBytes) async {
    if (!_defectClassifier.isReady) {
      return [];
    }

    try {
      return await _defectClassifier.detectFromBytes(imageBytes);
    } catch (e) {
      _logger.e('$_tag: Defect detection failed: $e');
      return [];
    }
  }

  Future<(TireSizeInfo?, DotCodeInfo?)> _analyzeSidewall(Uint8List imageBytes) async {
    try {
      final result = await _michelinApi.analyzeSidewall(imageBytes);
      return (result.$1, result.$2);
    } catch (e) {
      _logger.e('$_tag: Sidewall analysis failed: $e');
      return (null, null);
    }
  }

  int _calculateHealthScore(
    TreadDepthResult? treadResult,
    List<TireDefect> defects,
    DotCodeInfo? dotCodeInfo,
  ) {
    double score = 100;

    // Tread depth component (35%)
    if (treadResult != null) {
      double treadScore;
      switch (treadResult.status) {
        case TreadStatus.excellent:
          treadScore = 100;
          break;
        case TreadStatus.good:
          treadScore = 85;
          break;
        case TreadStatus.fair:
          treadScore = 65;
          break;
        case TreadStatus.low:
          treadScore = 35;
          break;
        case TreadStatus.critical:
          treadScore = 10;
          break;
        case TreadStatus.unknown:
          treadScore = 50;
          break;
      }
      score -= (100 - treadScore) * _treadWeight;
    }

    // Defect component (40%)
    if (defects.isNotEmpty) {
      final maxSeverity = defects
          .map((d) => d.severity.priority)
          .reduce((a, b) => a > b ? a : b);

      double defectPenalty;
      switch (maxSeverity) {
        case 4:
          defectPenalty = 100;
          break;
        case 3:
          defectPenalty = 70;
          break;
        case 2:
          defectPenalty = 40;
          break;
        case 1:
          defectPenalty = 20;
          break;
        default:
          defectPenalty = 0;
      }
      score -= defectPenalty * _defectWeight;
    }

    // Age component (25%)
    if (dotCodeInfo != null) {
      double ageScore;
      switch (dotCodeInfo.ageStatus) {
        case TireAgeStatus.newTire:
          ageScore = 100;
          break;
        case TireAgeStatus.good:
          ageScore = 85;
          break;
        case TireAgeStatus.aging:
          ageScore = 60;
          break;
        case TireAgeStatus.old:
          ageScore = 30;
          break;
        case TireAgeStatus.expired:
          ageScore = 0;
          break;
      }
      score -= (100 - ageScore) * _ageWeight;
    }

    return score.round().clamp(0, 100);
  }

  OverallTireStatus _determineOverallStatus(
    int score,
    TreadDepthResult? treadResult,
    List<TireDefect> defects,
  ) {
    // Check for critical conditions
    if (defects.any((d) => d.severity == DefectSeverity.critical)) {
      return OverallTireStatus.critical;
    }

    if (treadResult?.status == TreadStatus.critical) {
      return OverallTireStatus.critical;
    }

    // Base on score
    if (score >= 85) return OverallTireStatus.excellent;
    if (score >= 70) return OverallTireStatus.good;
    if (score >= 50) return OverallTireStatus.fair;
    if (score >= 25) return OverallTireStatus.poor;
    return OverallTireStatus.critical;
  }

  List<String> _generateConcerns(
    TreadDepthResult? treadResult,
    List<TireDefect> defects,
    DotCodeInfo? dotCodeInfo,
  ) {
    final concerns = <String>[];

    // Tread concerns
    if (treadResult != null) {
      switch (treadResult.status) {
        case TreadStatus.critical:
          concerns.add('⚠️ CRITICAL: Tread depth below legal minimum (${treadResult.minimumDepthMm.toStringAsFixed(1)}mm)');
          break;
        case TreadStatus.low:
          concerns.add('⚠️ Low tread depth - replacement needed soon');
          break;
        case TreadStatus.fair:
          concerns.add('Tread wear approaching replacement threshold');
          break;
        default:
          break;
      }

      switch (treadResult.wearPattern) {
        case TreadWearPattern.centerWear:
          concerns.add('Center wear pattern indicates over-inflation');
          break;
        case TreadWearPattern.edgeWear:
          concerns.add('Edge wear pattern indicates under-inflation');
          break;
        case TreadWearPattern.oneSideWear:
          concerns.add('Uneven wear suggests alignment issues');
          break;
        case TreadWearPattern.cupping:
          concerns.add('Cupping wear suggests suspension problems');
          break;
        default:
          break;
      }
    }

    // Defect concerns
    for (final defect in defects.where((d) => d.type != DefectType.good)) {
      concerns.add('${defect.severity.displayName} severity: ${defect.type.displayName}');
    }

    // Age concerns
    if (dotCodeInfo != null) {
      switch (dotCodeInfo.ageStatus) {
        case TireAgeStatus.expired:
          concerns.add('⚠️ Tire exceeds 10-year age limit - replace immediately');
          break;
        case TireAgeStatus.old:
          concerns.add('Tire is ${dotCodeInfo.ageInMonths ~/ 12} years old - inspect carefully');
          break;
        case TireAgeStatus.aging:
          concerns.add('Tire age approaching recommended replacement threshold');
          break;
        default:
          break;
      }
    }

    return concerns;
  }

  List<TireRecommendation> _generateRecommendations(
    TreadDepthResult? treadResult,
    List<TireDefect> defects,
    DotCodeInfo? dotCodeInfo,
  ) {
    final recommendations = <TireRecommendation>[];

    // Critical defects
    if (defects.any((d) => d.severity == DefectSeverity.critical)) {
      recommendations.add(TireRecommendation(
        title: 'Immediate Tire Replacement',
        description: 'Critical defect detected. Do not drive. Replace tire before use.',
        priority: 1,
        estimatedCost: 150,
        serviceType: ServiceType.replacement,
      ));
    }

    // Tread depth recommendations
    if (treadResult != null) {
      switch (treadResult.status) {
        case TreadStatus.critical:
          recommendations.add(TireRecommendation(
            title: 'Replace Tire - Critical Tread',
            description: 'Tread depth ${treadResult.minimumDepthMm.toStringAsFixed(1)}mm is below the 1.6mm legal minimum',
            priority: 1,
            estimatedCost: 150,
            serviceType: ServiceType.replacement,
          ));
          break;
        case TreadStatus.low:
          recommendations.add(TireRecommendation(
            title: 'Plan Tire Replacement',
            description: 'Tread depth approaching minimum. Replace within 5,000 km',
            priority: 2,
            estimatedCost: 150,
            serviceType: ServiceType.replacement,
          ));
          break;
        default:
          break;
      }

      // Wear pattern recommendations
      switch (treadResult.wearPattern) {
        case TreadWearPattern.centerWear:
          recommendations.add(TireRecommendation(
            title: 'Reduce Tire Pressure',
            description: 'Center wear indicates over-inflation. Reduce pressure to recommended PSI',
            priority: 3,
            estimatedCost: 0,
            serviceType: ServiceType.pressureAdjustment,
          ));
          break;
        case TreadWearPattern.edgeWear:
          recommendations.add(TireRecommendation(
            title: 'Increase Tire Pressure',
            description: 'Edge wear indicates under-inflation. Inflate to recommended PSI',
            priority: 3,
            estimatedCost: 0,
            serviceType: ServiceType.pressureAdjustment,
          ));
          break;
        case TreadWearPattern.oneSideWear:
          recommendations.add(TireRecommendation(
            title: 'Wheel Alignment Service',
            description: 'Uneven wear pattern suggests wheel alignment issues',
            priority: 2,
            estimatedCost: 80,
            serviceType: ServiceType.alignment,
          ));
          break;
        case TreadWearPattern.cupping:
          recommendations.add(TireRecommendation(
            title: 'Suspension Inspection',
            description: 'Cupping wear pattern indicates suspension problems',
            priority: 2,
            estimatedCost: 100,
            serviceType: ServiceType.inspection,
          ));
          break;
        default:
          break;
      }
    }

    // Repairable defects
    for (final defect in defects) {
      if (defect.severity == DefectSeverity.medium ||
          defect.severity == DefectSeverity.low) {
        if (defect.type == DefectType.puncture) {
          recommendations.add(TireRecommendation(
            title: 'Puncture Repair',
            description: 'Puncture can likely be repaired if in tread area',
            priority: 2,
            estimatedCost: 25,
            serviceType: ServiceType.repair,
          ));
        }
        if (defect.type == DefectType.foreignObject) {
          recommendations.add(TireRecommendation(
            title: 'Remove Foreign Object',
            description: 'Have foreign object removed and inspect for damage',
            priority: 3,
            estimatedCost: 15,
            serviceType: ServiceType.inspection,
          ));
        }
      }
    }

    // Age recommendations
    if (dotCodeInfo != null) {
      if (dotCodeInfo.ageStatus == TireAgeStatus.expired ||
          dotCodeInfo.ageStatus == TireAgeStatus.old) {
        recommendations.add(TireRecommendation(
          title: 'Age-Related Replacement',
          description: 'Tire is ${dotCodeInfo.ageInMonths ~/ 12} years old. Rubber degrades with age.',
          priority: dotCodeInfo.ageStatus == TireAgeStatus.expired ? 1 : 2,
          estimatedCost: 150,
          serviceType: ServiceType.replacement,
        ));
      }
    }

    // Default recommendation if no issues
    if (recommendations.isEmpty) {
      recommendations.add(TireRecommendation(
        title: 'Continue Regular Maintenance',
        description: 'Tire is in good condition. Continue regular rotation and pressure checks.',
        priority: 5,
        estimatedCost: 0,
        serviceType: ServiceType.rotation,
      ));
    }

    // Sort by priority
    recommendations.sort((a, b) => a.priority.compareTo(b.priority));
    return recommendations;
  }

  ActionRequired _determineActionRequired(
    OverallTireStatus status,
    TreadDepthResult? treadResult,
    List<TireDefect> defects,
  ) {
    // Check critical conditions
    if (defects.any((d) => d.severity == DefectSeverity.critical)) {
      final hasDangerousDefect = defects.any((d) =>
          d.type == DefectType.bulge || d.type == DefectType.sidewallDamage);
      return hasDangerousDefect ? ActionRequired.doNotDrive : ActionRequired.replace;
    }

    if (treadResult?.status == TreadStatus.critical) {
      return ActionRequired.replace;
    }

    switch (status) {
      case OverallTireStatus.critical:
        return ActionRequired.replace;
      case OverallTireStatus.poor:
        return ActionRequired.serviceNow;
      case OverallTireStatus.fair:
        return ActionRequired.serviceSoon;
      case OverallTireStatus.good:
        return ActionRequired.monitor;
      case OverallTireStatus.excellent:
        return ActionRequired.none;
    }
  }

  (int, int)? _estimateCostRange(List<TireRecommendation> recommendations) {
    if (recommendations.isEmpty) return null;

    final costs = recommendations
        .where((r) => r.estimatedCost != null && r.estimatedCost! > 0)
        .map((r) => r.estimatedCost!)
        .toList();

    if (costs.isEmpty) return null;

    final minCost = costs.reduce((a, b) => a < b ? a : b);
    final maxCost = costs.reduce((a, b) => a + b);

    return (minCost, maxCost);
  }

  /// Release all resources.
  void dispose() {
    _treadScanner.dispose();
    _michelinApi.dispose();
    _defectClassifier.dispose();
    _isInitialized = false;
    _logger.d('$_tag: Service resources released');
  }
}


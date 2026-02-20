/// Tire Analysis Models
/// Data models for tire health analysis, tread depth measurement,
/// defect detection, and tire specifications.

import 'dart:ui';

// ═══════════════════════════════════════════════════════════════════════════════
// Tread Depth Measurement Results
// ═══════════════════════════════════════════════════════════════════════════════

/// Comprehensive tread depth measurement result.
class TreadDepthResult {
  final String uuid;
  final DateTime measurementTimestamp;

  // Depth measurements in mm
  final double innerDepthMm;
  final double centerDepthMm;
  final double outerDepthMm;
  final double averageDepthMm;
  final double minimumDepthMm;

  // Confidence and quality
  final double confidence;
  final int qualityScore;

  // Wear analysis
  final double wearPercentage;
  final int estimatedRemainingLife;
  final TreadWearPattern wearPattern;

  // Status
  final TreadStatus status;
  final List<String> recommendations;

  TreadDepthResult({
    required this.uuid,
    DateTime? measurementTimestamp,
    required this.innerDepthMm,
    required this.centerDepthMm,
    required this.outerDepthMm,
    required this.averageDepthMm,
    required this.minimumDepthMm,
    required this.confidence,
    required this.qualityScore,
    required this.wearPercentage,
    required this.estimatedRemainingLife,
    required this.wearPattern,
    required this.status,
    required this.recommendations,
  }) : measurementTimestamp = measurementTimestamp ?? DateTime.now();

  bool get isValid => confidence >= 0.7;

  factory TreadDepthResult.empty() => TreadDepthResult(
    uuid: '',
    innerDepthMm: 0,
    centerDepthMm: 0,
    outerDepthMm: 0,
    averageDepthMm: 0,
    minimumDepthMm: 0,
    confidence: 0,
    qualityScore: 0,
    wearPercentage: 0,
    estimatedRemainingLife: 0,
    wearPattern: TreadWearPattern.unknown,
    status: TreadStatus.unknown,
    recommendations: [],
  );

  Map<String, dynamic> toJson() => {
    'uuid': uuid,
    'measurementTimestamp': measurementTimestamp.toIso8601String(),
    'innerDepthMm': innerDepthMm,
    'centerDepthMm': centerDepthMm,
    'outerDepthMm': outerDepthMm,
    'averageDepthMm': averageDepthMm,
    'minimumDepthMm': minimumDepthMm,
    'confidence': confidence,
    'qualityScore': qualityScore,
    'wearPercentage': wearPercentage,
    'estimatedRemainingLife': estimatedRemainingLife,
    'wearPattern': wearPattern.name,
    'status': status.name,
    'recommendations': recommendations,
  };

  factory TreadDepthResult.fromJson(Map<String, dynamic> json) => TreadDepthResult(
    uuid: json['uuid'] ?? '',
    measurementTimestamp: DateTime.tryParse(json['measurementTimestamp'] ?? ''),
    innerDepthMm: (json['innerDepthMm'] ?? 0).toDouble(),
    centerDepthMm: (json['centerDepthMm'] ?? 0).toDouble(),
    outerDepthMm: (json['outerDepthMm'] ?? 0).toDouble(),
    averageDepthMm: (json['averageDepthMm'] ?? 0).toDouble(),
    minimumDepthMm: (json['minimumDepthMm'] ?? 0).toDouble(),
    confidence: (json['confidence'] ?? 0).toDouble(),
    qualityScore: json['qualityScore'] ?? 0,
    wearPercentage: (json['wearPercentage'] ?? 0).toDouble(),
    estimatedRemainingLife: json['estimatedRemainingLife'] ?? 0,
    wearPattern: TreadWearPattern.values.firstWhere(
      (e) => e.name == json['wearPattern'],
      orElse: () => TreadWearPattern.unknown,
    ),
    status: TreadStatus.values.firstWhere(
      (e) => e.name == json['status'],
      orElse: () => TreadStatus.unknown,
    ),
    recommendations: List<String>.from(json['recommendations'] ?? []),
  );
}

/// Tread depth status classification.
enum TreadStatus {
  excellent('Excellent', Color(0xFF10B981)),
  good('Good', Color(0xFF22C55E)),
  fair('Fair', Color(0xFFF59E0B)),
  low('Low - Replace Soon', Color(0xFFF97316)),
  critical('Critical - Replace Now', Color(0xFFEF4444)),
  unknown('Unknown', Color(0xFF6B7280));

  final String displayName;
  final Color color;

  const TreadStatus(this.displayName, this.color);
}

/// Tread wear pattern classification.
enum TreadWearPattern {
  even('Even Wear', 'Normal driving - tire is wearing correctly'),
  centerWear('Center Wear', 'Over-inflation - reduce pressure'),
  edgeWear('Edge Wear', 'Under-inflation - increase pressure'),
  oneSideWear('One-Side Wear', 'Wheel alignment issue - check alignment'),
  cupping('Cupping/Scalloping', 'Suspension problem - check shocks/struts'),
  feathering('Feathering', 'Toe alignment issue - needs adjustment'),
  diagonalWear('Diagonal Wear', 'Multiple alignment issues'),
  flatSpot('Flat Spots', 'Brake lock-up or extended parking'),
  unknown('Unknown Pattern', 'Unable to determine wear pattern');

  final String displayName;
  final String cause;

  const TreadWearPattern(this.displayName, this.cause);
}

/// Heat map data point for tread visualization.
class TreadHeatMapPoint {
  final double x;
  final double y;
  final double depthMm;
  final double depthNormalized;

  TreadHeatMapPoint({
    required this.x,
    required this.y,
    required this.depthMm,
    required this.depthNormalized,
  });
}

/// Complete heat map for tire tread visualization.
class TreadHeatMap {
  final List<TreadHeatMapPoint> points;
  final int gridWidth;
  final int gridHeight;
  final double minDepthMm;
  final double maxDepthMm;

  TreadHeatMap({
    required this.points,
    required this.gridWidth,
    required this.gridHeight,
    required this.minDepthMm,
    required this.maxDepthMm,
  });
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tire Size Recognition Results
// ═══════════════════════════════════════════════════════════════════════════════

/// Tire size and specification from sidewall image recognition.
class TireSizeInfo {
  final String rawText;
  final int width;
  final int aspectRatio;
  final String construction;
  final int rimDiameter;
  final int loadIndex;
  final String speedRating;
  final double confidence;
  final List<String> additionalMarks;

  TireSizeInfo({
    required this.rawText,
    required this.width,
    required this.aspectRatio,
    required this.construction,
    required this.rimDiameter,
    required this.loadIndex,
    required this.speedRating,
    required this.confidence,
    this.additionalMarks = const [],
  });

  String get formattedSize => '$width/$aspectRatio$construction$rimDiameter';
  String get fullSpecification => '$formattedSize $loadIndex$speedRating';

  int get maxSpeedKmh {
    switch (speedRating.toUpperCase()) {
      case 'L': return 120;
      case 'M': return 130;
      case 'N': return 140;
      case 'P': return 150;
      case 'Q': return 160;
      case 'R': return 170;
      case 'S': return 180;
      case 'T': return 190;
      case 'U': return 200;
      case 'H': return 210;
      case 'V': return 240;
      case 'W': return 270;
      case 'Y': return 300;
      case 'Z': return 300;
      default: return 0;
    }
  }

  static const Map<int, int> _loadIndexTable = {
    70: 335, 71: 345, 72: 355, 73: 365, 74: 375,
    75: 387, 76: 400, 77: 412, 78: 425, 79: 437,
    80: 450, 81: 462, 82: 475, 83: 487, 84: 500,
    85: 515, 86: 530, 87: 545, 88: 560, 89: 580,
    90: 600, 91: 615, 92: 630, 93: 650, 94: 670,
    95: 690, 96: 710, 97: 730, 98: 750, 99: 775,
    100: 800, 101: 825, 102: 850, 103: 875, 104: 900,
  };

  int get maxLoadKg => _loadIndexTable[loadIndex] ?? 0;

  factory TireSizeInfo.empty() => TireSizeInfo(
    rawText: '',
    width: 0,
    aspectRatio: 0,
    construction: 'R',
    rimDiameter: 0,
    loadIndex: 0,
    speedRating: '',
    confidence: 0,
  );

  Map<String, dynamic> toJson() => {
    'rawText': rawText,
    'width': width,
    'aspectRatio': aspectRatio,
    'construction': construction,
    'rimDiameter': rimDiameter,
    'loadIndex': loadIndex,
    'speedRating': speedRating,
    'confidence': confidence,
    'additionalMarks': additionalMarks,
  };

  factory TireSizeInfo.fromJson(Map<String, dynamic> json) => TireSizeInfo(
    rawText: json['rawText'] ?? '',
    width: json['width'] ?? 0,
    aspectRatio: json['aspectRatio'] ?? 0,
    construction: json['construction'] ?? 'R',
    rimDiameter: json['rimDiameter'] ?? 0,
    loadIndex: json['loadIndex'] ?? 0,
    speedRating: json['speedRating'] ?? '',
    confidence: (json['confidence'] ?? 0).toDouble(),
    additionalMarks: List<String>.from(json['additionalMarks'] ?? []),
  );
}

// ═══════════════════════════════════════════════════════════════════════════════
// DOT Code Recognition Results
// ═══════════════════════════════════════════════════════════════════════════════

/// DOT (Department of Transportation) code information.
class DotCodeInfo {
  final String fullDotCode;
  final String plantCode;
  final String tireSize;
  final String brandCharacteristics;
  final int manufactureWeek;
  final int manufactureYear;
  final int ageInMonths;
  final double confidence;

  DotCodeInfo({
    required this.fullDotCode,
    required this.plantCode,
    required this.tireSize,
    required this.brandCharacteristics,
    required this.manufactureWeek,
    required this.manufactureYear,
    required this.ageInMonths,
    required this.confidence,
  });

  String get manufactureDate => 'Week $manufactureWeek, $manufactureYear';

  TireAgeStatus get ageStatus {
    if (ageInMonths < 36) return TireAgeStatus.newTire;
    if (ageInMonths < 60) return TireAgeStatus.good;
    if (ageInMonths < 72) return TireAgeStatus.aging;
    if (ageInMonths < 120) return TireAgeStatus.old;
    return TireAgeStatus.expired;
  }

  bool get isExpired => ageInMonths >= 120;

  factory DotCodeInfo.empty() => DotCodeInfo(
    fullDotCode: '',
    plantCode: '',
    tireSize: '',
    brandCharacteristics: '',
    manufactureWeek: 0,
    manufactureYear: 0,
    ageInMonths: 0,
    confidence: 0,
  );

  Map<String, dynamic> toJson() => {
    'fullDotCode': fullDotCode,
    'plantCode': plantCode,
    'tireSize': tireSize,
    'brandCharacteristics': brandCharacteristics,
    'manufactureWeek': manufactureWeek,
    'manufactureYear': manufactureYear,
    'ageInMonths': ageInMonths,
    'confidence': confidence,
  };

  factory DotCodeInfo.fromJson(Map<String, dynamic> json) => DotCodeInfo(
    fullDotCode: json['fullDotCode'] ?? '',
    plantCode: json['plantCode'] ?? '',
    tireSize: json['tireSize'] ?? '',
    brandCharacteristics: json['brandCharacteristics'] ?? '',
    manufactureWeek: json['manufactureWeek'] ?? 0,
    manufactureYear: json['manufactureYear'] ?? 0,
    ageInMonths: json['ageInMonths'] ?? 0,
    confidence: (json['confidence'] ?? 0).toDouble(),
  );
}

enum TireAgeStatus {
  newTire('New (< 3 years)', Color(0xFF10B981)),
  good('Good (3-5 years)', Color(0xFF22C55E)),
  aging('Aging (5-6 years)', Color(0xFFF59E0B)),
  old('Old (6-10 years)', Color(0xFFF97316)),
  expired('Expired (> 10 years)', Color(0xFFEF4444));

  final String displayName;
  final Color color;

  const TireAgeStatus(this.displayName, this.color);
}

// ═══════════════════════════════════════════════════════════════════════════════
// Defect Detection Results
// ═══════════════════════════════════════════════════════════════════════════════

/// Defect detection result from TFLite model.
class TireDefect {
  final DefectType type;
  final double confidence;
  final Rect? boundingBox;
  final DefectSeverity severity;
  final String description;

  TireDefect({
    required this.type,
    required this.confidence,
    this.boundingBox,
    required this.severity,
    required this.description,
  });

  Map<String, dynamic> toJson() => {
    'type': type.name,
    'confidence': confidence,
    'boundingBox': boundingBox != null ? {
      'left': boundingBox!.left,
      'top': boundingBox!.top,
      'right': boundingBox!.right,
      'bottom': boundingBox!.bottom,
    } : null,
    'severity': severity.name,
    'description': description,
  };
}

enum DefectType {
  crack('Crack'),
  bulge('Bulge/Bubble'),
  cut('Cut/Slash'),
  puncture('Puncture'),
  wornTread('Worn Tread'),
  sidewallDamage('Sidewall Damage'),
  foreignObject('Foreign Object'),
  dryRot('Dry Rot/Age Cracking'),
  beadDamage('Bead Damage'),
  good('No Defect Detected');

  final String displayName;

  const DefectType(this.displayName);
}

enum DefectSeverity {
  none('None', Color(0xFF22C55E), 0),
  low('Low', Color(0xFFF59E0B), 1),
  medium('Medium', Color(0xFFF97316), 2),
  high('High', Color(0xFFEF4444), 3),
  critical('Critical', Color(0xFFDC2626), 4);

  final String displayName;
  final Color color;
  final int priority;

  const DefectSeverity(this.displayName, this.color, this.priority);
}

// ═══════════════════════════════════════════════════════════════════════════════
// Comprehensive Analysis Result
// ═══════════════════════════════════════════════════════════════════════════════

/// Comprehensive tire health analysis combining all data sources.
class ComprehensiveTireAnalysis {
  final String id;
  final DateTime timestamp;

  // Source data
  final TreadDepthResult? treadDepth;
  final TireSizeInfo? tireSizeInfo;
  final DotCodeInfo? dotCodeInfo;
  final List<TireDefect> detectedDefects;

  // Captured images
  final String? treadImagePath;
  final String? sidewallImagePath;

  // Overall assessment
  final int overallHealthScore;
  final OverallTireStatus overallStatus;
  final List<String> primaryConcerns;
  final List<TireRecommendation> recommendations;

  // Action required
  final ActionRequired actionRequired;
  final (int, int)? estimatedCostRange;

  // PDF report
  final String? reportPdfPath;

  ComprehensiveTireAnalysis({
    required this.id,
    DateTime? timestamp,
    this.treadDepth,
    this.tireSizeInfo,
    this.dotCodeInfo,
    required this.detectedDefects,
    this.treadImagePath,
    this.sidewallImagePath,
    required this.overallHealthScore,
    required this.overallStatus,
    required this.primaryConcerns,
    required this.recommendations,
    required this.actionRequired,
    this.estimatedCostRange,
    this.reportPdfPath,
  }) : timestamp = timestamp ?? DateTime.now();

  Map<String, dynamic> toJson() => {
    'id': id,
    'timestamp': timestamp.toIso8601String(),
    'treadDepth': treadDepth?.toJson(),
    'tireSizeInfo': tireSizeInfo?.toJson(),
    'dotCodeInfo': dotCodeInfo?.toJson(),
    'detectedDefects': detectedDefects.map((d) => d.toJson()).toList(),
    'treadImagePath': treadImagePath,
    'sidewallImagePath': sidewallImagePath,
    'overallHealthScore': overallHealthScore,
    'overallStatus': overallStatus.name,
    'primaryConcerns': primaryConcerns,
    'recommendations': recommendations.map((r) => r.toJson()).toList(),
    'actionRequired': actionRequired.name,
    'estimatedCostRange': estimatedCostRange != null
        ? {'min': estimatedCostRange!.$1, 'max': estimatedCostRange!.$2}
        : null,
    'reportPdfPath': reportPdfPath,
  };
}

enum OverallTireStatus {
  excellent('Excellent - No Issues', Color(0xFF10B981)),
  good('Good - Minor Wear', Color(0xFF22C55E)),
  fair('Fair - Monitor Closely', Color(0xFFF59E0B)),
  poor('Poor - Service Soon', Color(0xFFF97316)),
  critical('Critical - Unsafe', Color(0xFFEF4444));

  final String displayName;
  final Color color;

  const OverallTireStatus(this.displayName, this.color);
}

enum ActionRequired {
  none('No Action Needed', 0),
  monitor('Monitor & Re-check', 1),
  serviceSoon('Service Within 30 Days', 2),
  serviceNow('Service Immediately', 3),
  replace('Replace Tire', 4),
  doNotDrive('Do Not Drive - Unsafe', 5);

  final String displayName;
  final int urgency;

  const ActionRequired(this.displayName, this.urgency);
}

class TireRecommendation {
  final String title;
  final String description;
  final int priority;
  final int? estimatedCost;
  final ServiceType serviceType;

  TireRecommendation({
    required this.title,
    required this.description,
    required this.priority,
    this.estimatedCost,
    required this.serviceType,
  });

  Map<String, dynamic> toJson() => {
    'title': title,
    'description': description,
    'priority': priority,
    'estimatedCost': estimatedCost,
    'serviceType': serviceType.name,
  };
}

enum ServiceType {
  rotation,
  balancing,
  alignment,
  repair,
  replacement,
  pressureAdjustment,
  inspection,
}


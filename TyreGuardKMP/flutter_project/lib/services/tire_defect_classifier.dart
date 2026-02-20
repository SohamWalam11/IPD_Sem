import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/services.dart';
import 'package:image/image.dart' as img;
import 'package:logger/logger.dart';
import 'package:tflite_flutter/tflite_flutter.dart';

import '../models/tire_analysis_models.dart';

/// TensorFlow Lite based tire defect classifier.
///
/// Uses the custom trained YOLO model (best_int8.tflite) for:
/// - Defect detection (cracks, bulges, cuts, etc.)
/// - Bounding box localization
/// - Confidence scoring
class TireDefectClassifier {
  static const String _modelPath = 'assets/models/best_int8.tflite';
  static const String _tag = 'TireDefectClassifier';

  // YOLO model parameters
  static const int _inputSize = 640;
  static const int _numChannels = 3;
  static const double _confidenceThreshold = 0.5;
  static const double _iouThreshold = 0.45;

  // Defect labels - adjust based on your model's training
  static const List<String> _labels = [
    'Good',
    'Cracked',
    'Worn',
    'Bulge',
    'Cut',
    'Flat',
    'Puncture',
    'Sidewall Damage',
  ];

  final Logger _logger = Logger();
  Interpreter? _interpreter;
  bool _isModelLoaded = false;
  bool _isQuantized = false;

  List<int>? _inputShape;
  List<int>? _outputShape;

  /// Check if the model is ready for inference.
  bool get isReady => _isModelLoaded && _interpreter != null;

  /// Initialize and load the TFLite model.
  Future<bool> initialize() async {
    try {
      _logger.d('$_tag: Loading TFLite model from $_modelPath');

      // Load model from assets
      final options = InterpreterOptions()..threads = 2;

      // Try to use GPU delegate if available
      // Note: GPU delegate may not be available on all devices
      // Uncomment below if you want to enable GPU acceleration
      // try {
      //   final gpuDelegate = GpuDelegateV2();
      //   options.addDelegate(gpuDelegate);
      //   _logger.d('$_tag: GPU delegate enabled');
      // } catch (e) {
      //   _logger.w('$_tag: GPU delegate not available: $e');
      // }

      _interpreter = await Interpreter.fromAsset(_modelPath, options: options);

      // Get input/output tensor info
      final inputTensor = _interpreter!.getInputTensor(0);
      final outputTensor = _interpreter!.getOutputTensor(0);

      _inputShape = inputTensor.shape;
      _outputShape = outputTensor.shape;

      // Check if model is quantized
      _isQuantized = inputTensor.type == TensorType.uint8 ||
                     inputTensor.type == TensorType.int8;

      _logger.d('$_tag: Model loaded successfully');
      _logger.d('$_tag: Input shape: $_inputShape, Output shape: $_outputShape');
      _logger.d('$_tag: Quantized: $_isQuantized');

      _isModelLoaded = true;
      return true;
    } catch (e, stackTrace) {
      _logger.e('$_tag: Failed to load model: $e', error: e, stackTrace: stackTrace);
      _isModelLoaded = false;
      return false;
    }
  }

  /// Run defect detection on an image file.
  Future<List<TireDefect>> detectFromFile(File imageFile) async {
    try {
      final bytes = await imageFile.readAsBytes();
      return detectFromBytes(bytes);
    } catch (e) {
      _logger.e('$_tag: Failed to read image file: $e');
      return [];
    }
  }

  /// Run defect detection on image bytes.
  Future<List<TireDefect>> detectFromBytes(Uint8List imageBytes) async {
    if (!isReady) {
      _logger.w('$_tag: Model not loaded');
      return [];
    }

    try {
      // Decode image
      final image = img.decodeImage(imageBytes);
      if (image == null) {
        _logger.e('$_tag: Failed to decode image');
        return [];
      }

      return _runInference(image);
    } catch (e, stackTrace) {
      _logger.e('$_tag: Detection failed: $e', error: e, stackTrace: stackTrace);
      return [];
    }
  }

  /// Run inference on a decoded image.
  Future<List<TireDefect>> _runInference(img.Image image) async {
    final originalWidth = image.width;
    final originalHeight = image.height;

    // Resize image to model input size
    final resizedImage = img.copyResize(
      image,
      width: _inputSize,
      height: _inputSize,
      interpolation: img.Interpolation.linear,
    );

    // Prepare input tensor
    final input = _prepareInput(resizedImage);

    // Prepare output tensor
    // Typical YOLO output: [1, num_detections, 5 + num_classes]
    final outputShape = _outputShape ?? [1, 25200, 13];
    final output = List.generate(
      outputShape[0],
      (_) => List.generate(
        outputShape[1],
        (_) => List<double>.filled(outputShape[2], 0),
      ),
    );

    // Run inference
    _interpreter!.run(input, output);

    // Post-process results
    final detections = _postProcess(output[0], originalWidth, originalHeight);

    // Apply NMS
    return _applyNMS(detections);
  }

  /// Prepare input tensor from image.
  dynamic _prepareInput(img.Image image) {
    if (_isQuantized) {
      // For quantized model: return Uint8List
      final input = Uint8List(_inputSize * _inputSize * _numChannels);
      int index = 0;

      for (int y = 0; y < _inputSize; y++) {
        for (int x = 0; x < _inputSize; x++) {
          final pixel = image.getPixel(x, y);
          input[index++] = pixel.r.toInt();
          input[index++] = pixel.g.toInt();
          input[index++] = pixel.b.toInt();
        }
      }

      return input.reshape([1, _inputSize, _inputSize, _numChannels]);
    } else {
      // For float model: normalize to 0-1
      final input = Float32List(_inputSize * _inputSize * _numChannels);
      int index = 0;

      for (int y = 0; y < _inputSize; y++) {
        for (int x = 0; x < _inputSize; x++) {
          final pixel = image.getPixel(x, y);
          input[index++] = pixel.r / 255.0;
          input[index++] = pixel.g / 255.0;
          input[index++] = pixel.b / 255.0;
        }
      }

      return input.reshape([1, _inputSize, _inputSize, _numChannels]);
    }
  }

  /// Post-process YOLO output to get detection results.
  List<TireDefect> _postProcess(
    List<List<double>> output,
    int originalWidth,
    int originalHeight,
  ) {
    final results = <TireDefect>[];

    for (final detection in output) {
      // YOLO format: [x_center, y_center, width, height, confidence, class_scores...]
      final confidence = detection[4];

      if (confidence < _confidenceThreshold) continue;

      // Find best class
      double maxClassScore = 0;
      int maxClassIdx = 0;

      for (int i = 5; i < detection.length; i++) {
        if (detection[i] > maxClassScore) {
          maxClassScore = detection[i];
          maxClassIdx = i - 5;
        }
      }

      final finalConfidence = confidence * maxClassScore;
      if (finalConfidence < _confidenceThreshold) continue;

      // Convert normalized coordinates to pixel coordinates
      final xCenter = detection[0] * originalWidth;
      final yCenter = detection[1] * originalHeight;
      final width = detection[2] * originalWidth;
      final height = detection[3] * originalHeight;

      final left = (xCenter - width / 2).clamp(0, originalWidth.toDouble());
      final top = (yCenter - height / 2).clamp(0, originalHeight.toDouble());
      final right = (xCenter + width / 2).clamp(0, originalWidth.toDouble());
      final bottom = (yCenter + height / 2).clamp(0, originalHeight.toDouble());

      final label = maxClassIdx < _labels.length
          ? _labels[maxClassIdx]
          : 'Defect $maxClassIdx';

      final defectType = _mapLabelToDefectType(label);
      final severity = _calculateSeverity(defectType, finalConfidence);

      results.add(TireDefect(
        type: defectType,
        confidence: finalConfidence,
        boundingBox: ui.Rect.fromLTRB(left, top, right, bottom),
        severity: severity,
        description: _generateDescription(defectType, severity),
      ));
    }

    return results;
  }

  /// Apply Non-Maximum Suppression to remove overlapping detections.
  List<TireDefect> _applyNMS(List<TireDefect> detections) {
    if (detections.isEmpty) return [];

    // Sort by confidence descending
    final sorted = List<TireDefect>.from(detections)
      ..sort((a, b) => b.confidence.compareTo(a.confidence));

    final selected = <TireDefect>[];

    while (sorted.isNotEmpty) {
      final best = sorted.removeAt(0);
      selected.add(best);

      sorted.removeWhere((detection) {
        if (best.boundingBox == null || detection.boundingBox == null) {
          return false;
        }
        return _calculateIoU(best.boundingBox!, detection.boundingBox!) > _iouThreshold;
      });
    }

    return selected;
  }

  /// Calculate Intersection over Union for two bounding boxes.
  double _calculateIoU(ui.Rect box1, ui.Rect box2) {
    final intersectionLeft = box1.left > box2.left ? box1.left : box2.left;
    final intersectionTop = box1.top > box2.top ? box1.top : box2.top;
    final intersectionRight = box1.right < box2.right ? box1.right : box2.right;
    final intersectionBottom = box1.bottom < box2.bottom ? box1.bottom : box2.bottom;

    if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
      return 0;
    }

    final intersectionArea = (intersectionRight - intersectionLeft) *
                             (intersectionBottom - intersectionTop);
    final box1Area = (box1.right - box1.left) * (box1.bottom - box1.top);
    final box2Area = (box2.right - box2.left) * (box2.bottom - box2.top);
    final unionArea = box1Area + box2Area - intersectionArea;

    return unionArea > 0 ? intersectionArea / unionArea : 0;
  }

  DefectType _mapLabelToDefectType(String label) {
    switch (label.toLowerCase()) {
      case 'crack':
      case 'cracked':
        return DefectType.crack;
      case 'bulge':
      case 'bubble':
        return DefectType.bulge;
      case 'cut':
      case 'slash':
        return DefectType.cut;
      case 'puncture':
        return DefectType.puncture;
      case 'worn':
      case 'wear':
      case 'worn_tread':
        return DefectType.wornTread;
      case 'sidewall':
      case 'sidewall_damage':
        return DefectType.sidewallDamage;
      case 'foreign':
      case 'object':
      case 'foreign_object':
        return DefectType.foreignObject;
      case 'dry_rot':
      case 'age':
      case 'aging':
        return DefectType.dryRot;
      case 'bead':
      case 'bead_damage':
        return DefectType.beadDamage;
      case 'good':
      case 'ok':
      case 'normal':
        return DefectType.good;
      default:
        return DefectType.good;
    }
  }

  DefectSeverity _calculateSeverity(DefectType type, double confidence) {
    if (type == DefectType.good) return DefectSeverity.none;

    // Base severity from defect type
    DefectSeverity baseSeverity;
    switch (type) {
      case DefectType.bulge:
      case DefectType.sidewallDamage:
      case DefectType.beadDamage:
        baseSeverity = DefectSeverity.critical;
        break;
      case DefectType.cut:
      case DefectType.puncture:
        baseSeverity = DefectSeverity.high;
        break;
      case DefectType.crack:
      case DefectType.dryRot:
        baseSeverity = DefectSeverity.medium;
        break;
      case DefectType.wornTread:
      case DefectType.foreignObject:
        baseSeverity = DefectSeverity.low;
        break;
      default:
        baseSeverity = DefectSeverity.none;
    }

    // Adjust based on confidence
    if (confidence < 0.5) {
      switch (baseSeverity) {
        case DefectSeverity.critical:
          return DefectSeverity.high;
        case DefectSeverity.high:
          return DefectSeverity.medium;
        case DefectSeverity.medium:
          return DefectSeverity.low;
        default:
          return baseSeverity;
      }
    }

    return baseSeverity;
  }

  String _generateDescription(DefectType type, DefectSeverity severity) {
    switch (type) {
      case DefectType.crack:
        return 'Surface cracks detected - may indicate age or heat damage';
      case DefectType.bulge:
        return 'Bulge/bubble detected - internal structure damage, replace immediately';
      case DefectType.cut:
        return 'Cut or slash damage - check for air leaks, may require replacement';
      case DefectType.puncture:
        return 'Puncture detected - repair or replace depending on location';
      case DefectType.wornTread:
        return 'Excessive tread wear - reduced traction, plan for replacement';
      case DefectType.sidewallDamage:
        return 'Sidewall damage - structural integrity compromised';
      case DefectType.foreignObject:
        return 'Foreign object embedded - remove and inspect for damage';
      case DefectType.dryRot:
        return 'Dry rot/age cracking - tire deterioration from age or UV';
      case DefectType.beadDamage:
        return 'Bead damage - seal integrity may be compromised';
      case DefectType.good:
        return 'No defects detected';
    }
  }

  /// Get the best detection result (highest confidence).
  Future<TireDefect?> detectBest(Uint8List imageBytes) async {
    final detections = await detectFromBytes(imageBytes);
    if (detections.isEmpty) return null;
    return detections.reduce((a, b) => a.confidence > b.confidence ? a : b);
  }

  /// Release resources.
  void dispose() {
    _interpreter?.close();
    _interpreter = null;
    _isModelLoaded = false;
    _logger.d('$_tag: Resources released');
  }
}

extension on Uint8List {
  List<List<List<List<int>>>> reshape(List<int> shape) {
    // Reshape for [1, height, width, channels]
    final result = List.generate(
      shape[0],
      (_) => List.generate(
        shape[1],
        (y) => List.generate(
          shape[2],
          (x) => List.generate(
            shape[3],
            (c) => this[(y * shape[2] + x) * shape[3] + c],
          ),
        ),
      ),
    );
    return result;
  }
}

extension on Float32List {
  List<List<List<List<double>>>> reshape(List<int> shape) {
    final result = List.generate(
      shape[0],
      (_) => List.generate(
        shape[1],
        (y) => List.generate(
          shape[2],
          (x) => List.generate(
            shape[3],
            (c) => this[(y * shape[2] + x) * shape[3] + c],
          ),
        ),
      ),
    );
    return result;
  }
}


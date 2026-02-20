// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserModel _$UserModelFromJson(Map<String, dynamic> json) => UserModel(
      id: json['id'] as String,
      email: json['email'] as String,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      phoneNumber: json['phoneNumber'] as String?,
      profileImageUrl: json['profileImageUrl'] as String?,
      vehicles: List<String>.from(json['vehicles'] as List<dynamic>),
      preferences: UserPreferences.fromJson(
          json['preferences'] as Map<String, dynamic>),
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$UserModelToJson(UserModel instance) => <String, dynamic>{
      'id': instance.id,
      'email': instance.email,
      'firstName': instance.firstName,
      'lastName': instance.lastName,
      'phoneNumber': instance.phoneNumber,
      'profileImageUrl': instance.profileImageUrl,
      'vehicles': instance.vehicles,
      'preferences': instance.preferences,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt.toIso8601String(),
    };

UserPreferences _$UserPreferencesFromJson(Map<String, dynamic> json) =>
    UserPreferences(
      notificationsEnabled: json['notificationsEnabled'] as bool,
      alertThreshold: json['alertThreshold'] as int,
      theme: json['theme'] as String,
      locationEnabled: json['locationEnabled'] as bool,
      alertTypes: List<String>.from(json['alertTypes'] as List<dynamic>),
    );

Map<String, dynamic> _$UserPreferencesToJson(UserPreferences instance) =>
    <String, dynamic>{
      'notificationsEnabled': instance.notificationsEnabled,
      'alertThreshold': instance.alertThreshold,
      'theme': instance.theme,
      'locationEnabled': instance.locationEnabled,
      'alertTypes': instance.alertTypes,
    };

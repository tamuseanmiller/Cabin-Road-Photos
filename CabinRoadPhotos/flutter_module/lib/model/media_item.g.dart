// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'media_item.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MediaItem _$MediaItemFromJson(Map<String, dynamic> json) {
  // print("MediaMetadata: " + json['mediaMetadata'].toString());
  return MediaItem(
    json['id'] as String,
    json['description'] as String,
    json['baseUrl'] as String,
    json['type'] as String
  );
}

Map<String, dynamic> _$MediaItemToJson(MediaItem instance) => <String, dynamic>{
      'id': instance.id,
      'description': instance.description,
      'baseUrl': instance.baseUrl,
      'type': instance.type,
    };

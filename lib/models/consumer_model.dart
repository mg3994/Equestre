import 'info_model.dart';

class ConsumerModel {
  final String? id;
  final InfoModel? info;
  final bool? paused;

  ConsumerModel({
    this.id,
    this.info,
    this.paused,
  });

  factory ConsumerModel.fromJson(Map<String, dynamic> json) {
    return ConsumerModel(
      id: json['id'] as String?,
      info: json['info'] != null ? InfoModel.fromJson(json['info']) : null,
      paused: json['paused'] as bool?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'info': info?.toJson(),
      'paused': paused,
    };
  }
}

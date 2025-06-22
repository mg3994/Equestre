class InfoModel {
  final String? title;
  final bool? live; // 1 = true, 0 = false
  final String? eventTitle;
  final String? eventTime;
  final DateTime? eventDate;
  final String? category;
  final String? country;

  InfoModel({
    this.title,
    this.live,
    this.eventTitle,
    this.eventTime,
    this.eventDate,
    this.category,
    this.country,
  });

  factory InfoModel.fromJson(Map<String, dynamic> json) {
    return InfoModel(
      title: json['title'] as String?,
      live: json['live'] == 1,
      eventTitle: json['eventTitle'] as String?,
      eventTime: json['eventTime'] as String?,
      eventDate: json['eventDate'] != null ? DateTime.tryParse(json['eventDate']) : null,
      category: json['category'] as String?,
      country: json['country'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'live': live == true ? 1 : 0,
      'eventTitle': eventTitle,
      'eventTime': eventTime,
      'eventDate': eventDate?.toIso8601String(),
      'category': category,
      'country': country,
    };
  }
}


class OverlayItem {
  final String? text;
  final int? textSizePx;
  final String? bgColor;
  final String? fgColor;
  final double? x;
  final double? y;

  OverlayItem({
    this.text,
    this.textSizePx,
    this.bgColor,
    this.fgColor,
    this.x,
    this.y,
  });

  OverlayItem copyWith({
    String? text,
    int? textSizePx,
    String? bgColor,
    String? fgColor,
    double? x,
    double? y,
  }) {
    return OverlayItem(
      text: text ?? this.text,
      textSizePx: textSizePx ?? this.textSizePx,
      bgColor: bgColor ?? this.bgColor,
      fgColor: fgColor ?? this.fgColor,
      x: x ?? this.x,
      y: y ?? this.y,
    );
  }

  Map<String, dynamic> toMap(String prefix) {
    final map = <String, dynamic>{};
    if (text != null) map[prefix] = text;
    if (textSizePx != null) map['${prefix}TextSizePx'] = textSizePx;
    if (bgColor != null) map['${prefix}BgColor'] = bgColor;
    if (fgColor != null) map['${prefix}FgColor'] = fgColor;
    if (x != null) map['${prefix}X'] = x;
    if (y != null) map['${prefix}Y'] = y;
    return map;
  }
}

class OverlayConfig {
  final OverlayItem? horseName;
  final OverlayItem? horseNumber;
  final OverlayItem? rider;
  final OverlayItem? penalty;
  final OverlayItem? timeTaken;
  final OverlayItem? rank;
  final OverlayItem? gapToBest;
  final OverlayItem? liveMsg;

  OverlayConfig({
    this.horseName,
    this.horseNumber,
    this.rider,
    this.penalty,
    this.timeTaken,
    this.rank,
    this.gapToBest,
    this.liveMsg,
  });

  OverlayConfig copyWith({
    OverlayItem? horseName,
    OverlayItem? horseNumber,
    OverlayItem? rider,
    OverlayItem? penalty,
    OverlayItem? timeTaken,
    OverlayItem? rank,
    OverlayItem? gapToBest,
    OverlayItem? liveMsg,
  }) {
    return OverlayConfig(
      horseName: horseName ?? this.horseName,
      horseNumber: horseNumber ?? this.horseNumber,
      rider: rider ?? this.rider,
      penalty: penalty ?? this.penalty,
      timeTaken: timeTaken ?? this.timeTaken,
      rank: rank ?? this.rank,
      gapToBest: gapToBest ?? this.gapToBest,
      liveMsg: liveMsg ?? this.liveMsg,
    );
  }

  /// Flatten all OverlayItems to a single map with keys like 'horseNameTextSizePx'
  Map<String, dynamic> toMap() {
    final map = <String, dynamic>{};
    if (horseName != null) map.addAll(horseName!.toMap('horseName'));
    if (horseNumber != null) map.addAll(horseNumber!.toMap('horseNumber'));
    if (rider != null) map.addAll(rider!.toMap('rider'));
    if (penalty != null) map.addAll(penalty!.toMap('penalty'));
    if (timeTaken != null) map.addAll(timeTaken!.toMap('timeTaken'));
    if (rank != null) map.addAll(rank!.toMap('rank'));
    if (gapToBest != null) map.addAll(gapToBest!.toMap('gapToBest'));
    if (liveMsg != null) map.addAll(liveMsg!.toMap('liveMsg'));
    return map;
  }
}

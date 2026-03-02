import 'package:latlong2/latlong.dart';

abstract final class DistanceUtil {
  static const _distance = Distance();

  static double calculate(LatLng from, LatLng to) =>
      _distance.as(LengthUnit.Meter, from, to);

  static String format(double metres) {
    if (metres < 1000) {
      return '${metres.round()} m away';
    }
    return '${(metres / 1000).toStringAsFixed(1)} km away';
  }
}

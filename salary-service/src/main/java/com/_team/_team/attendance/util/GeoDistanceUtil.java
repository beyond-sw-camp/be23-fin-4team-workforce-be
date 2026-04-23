package com._team._team.attendance.util;

/**
 * GPS 좌표 간 거리 계산, Haversine 공식 (구면 최단거리)
 */
public final class GeoDistanceUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoDistanceUtil() {}

    /**
     * 두 지점 간 거리를 미터 단위로 반환
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
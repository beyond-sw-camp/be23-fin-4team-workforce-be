package com._team._team.attendance.service;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.WorkTripDetail;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.WorkTripDetailRepository;
import com._team._team.attendance.util.GeoDistanceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 출퇴근 위치 검증, IP 또는 GPS 기반
 * - 회사 허용 IP 에서 요청: 통과
 * - IP 실패 → 오늘 승인된 출장 범위 내 GPS: 통과
 * - 둘 다 실패: 차단
 */
@Service
@Transactional(readOnly = true)
public class AttendanceLocationValidator {

    private static final int DEFAULT_TRIP_RADIUS_METERS = 200;

    private final CompanyIpWhitelistService companyIpWhitelistService;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkTripDetailRepository workTripDetailRepository;

    @Autowired
    public AttendanceLocationValidator(CompanyIpWhitelistService companyIpWhitelistService,
                                       DailyAttendanceRepository dailyAttendanceRepository,
                                       WorkTripDetailRepository workTripDetailRepository) {
        this.companyIpWhitelistService = companyIpWhitelistService;
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workTripDetailRepository = workTripDetailRepository;
    }

    public LocationResult validate(UUID companyId, UUID memberId, LocalDate date,
                                   String clientIp, Double lat, Double lng) {
        // 1. 회사 IP 허용
        if (companyIpWhitelistService.isIpAllowed(companyId, clientIp)) {
            return LocationResult.allowedByIp();
        }

        // 2. IP 실패, 출장 GPS 체크
        if (lat == null || lng == null) {
            return LocationResult.deniedNoGps();
        }

        Optional<DailyAttendance> dailyOpt = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(memberId, date);
        if (dailyOpt.isEmpty()) {
            return LocationResult.deniedNoTrip();
        }
        UUID dailyId = dailyOpt.get().getDailyAttendanceId();

        List<WorkTripDetail> trips = workTripDetailRepository
                .findByMemberIdAndCompanyIdAndDelYnAndDailyAttendanceDailyAttendanceId(
                        memberId, companyId, "N", dailyId);
        if (trips.isEmpty()) {
            return LocationResult.deniedNoTrip();
        }

        // 가장 가까운 출장지 찾기 + 반경 체크
        for (WorkTripDetail trip : trips) {
            if (trip.getDestinationLat() == null || trip.getDestinationLng() == null) continue;
            int radius = trip.getDestinationRadiusMeters() != null
                    ? trip.getDestinationRadiusMeters()
                    : DEFAULT_TRIP_RADIUS_METERS;
            double distance = GeoDistanceUtil.haversineMeters(
                    lat, lng, trip.getDestinationLat(), trip.getDestinationLng());
            if (distance <= radius) {
                return LocationResult.allowedByGps(trip.getDestination(), (int) distance);
            }
        }

        return LocationResult.deniedOutsideTrip();
    }

    public record LocationResult(boolean allowed, String reason, String detail) {
        public static LocationResult allowedByIp() {
            return new LocationResult(true, "IP_MATCH", null);
        }
        public static LocationResult allowedByGps(String destination, int distanceMeters) {
            return new LocationResult(true, "GPS_MATCH",
                    destination + " 반경 내 (" + distanceMeters + "m)");
        }
        public static LocationResult deniedNoGps() {
            return new LocationResult(false, "NO_GPS",
                    "사무실 IP 가 아니며 GPS 좌표도 전달되지 않았습니다.");
        }
        public static LocationResult deniedNoTrip() {
            return new LocationResult(false, "NO_TRIP",
                    "오늘 등록된 출장이 없어 외부에서 출근할 수 없습니다.");
        }
        public static LocationResult deniedOutsideTrip() {
            return new LocationResult(false, "OUTSIDE_TRIP",
                    "등록된 출장지 반경 밖에서 출근을 시도했습니다.");
        }
    }
}
package com._team._team.attendance.util;

/**
 * IPv4 CIDR 매칭 유틸, prefix 없으면 단일 IP 로 취급
 */
public final class CidrMatcher {

    private CidrMatcher() {}

    public static boolean matches(String clientIp, String cidr) {
        if (clientIp == null || cidr == null || cidr.isBlank()) {
            return false;
        }
        try {
            int slashIdx = cidr.indexOf('/');
            String network;
            int prefix;
            if (slashIdx < 0) {
                network = cidr;
                prefix = 32;
            } else {
                network = cidr.substring(0, slashIdx);
                prefix = Integer.parseInt(cidr.substring(slashIdx + 1));
            }
            if (prefix < 0 || prefix > 32) {
                return false;
            }

            long networkLong = ipv4ToLong(network);
            long clientLong = ipv4ToLong(clientIp);
            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;

            return (networkLong & mask) == (clientLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipv4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("IPv4 형식 아님: " + ip);
        }
        long result = 0;
        for (String octet : octets) {
            int v = Integer.parseInt(octet);
            if (v < 0 || v > 255) {
                throw new IllegalArgumentException("옥텟 범위 벗어남: " + v);
            }
            result = (result << 8) | v;
        }
        return result;
    }
}
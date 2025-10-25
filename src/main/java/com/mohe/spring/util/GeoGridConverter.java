package com.mohe.spring.util;

/**
 * Utility class to convert WGS84 coordinates (latitude, longitude) to KMA grid coordinates (nx, ny)
 * Based on Korean Meteorological Administration's Lambert Conformal Conic projection
 */
public class GeoGridConverter {

    // Lambert Conformal Conic projection parameters for KMA grid
    private static final double RE = 6371.00877; // Earth radius (km)
    private static final double GRID = 5.0; // Grid spacing (km)
    private static final double SLAT1 = 30.0; // Standard parallel 1
    private static final double SLAT2 = 60.0; // Standard parallel 2
    private static final double OLON = 126.0; // Origin longitude
    private static final double OLAT = 38.0; // Origin latitude
    private static final double XO = 43; // Grid X origin
    private static final double YO = 136; // Grid Y origin

    /**
     * Grid coordinates result
     */
    public static class GridCoordinate {
        private final int nx;
        private final int ny;

        public GridCoordinate(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }

        public int getNx() { return nx; }
        public int getNy() { return ny; }

        @Override
        public String toString() {
            return "GridCoordinate{nx=" + nx + ", ny=" + ny + "}";
        }
    }

    /**
     * Convert WGS84 coordinates to KMA grid coordinates
     *
     * @param lat Latitude in decimal degrees
     * @param lon Longitude in decimal degrees
     * @return Grid coordinates (nx, ny)
     */
    public static GridCoordinate toGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new GridCoordinate(nx, ny);
    }

    /**
     * Validate if coordinates are within Korean territory
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return true if within approximate Korean boundaries
     */
    public static boolean isValidKoreanCoordinate(double lat, double lon) {
        // Approximate boundaries of South Korea
        return lat >= 33.0 && lat <= 38.9 && lon >= 124.5 && lon <= 132.0;
    }
}

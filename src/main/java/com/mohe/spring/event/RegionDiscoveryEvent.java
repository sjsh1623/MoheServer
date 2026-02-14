package com.mohe.spring.event;

import org.springframework.context.ApplicationEvent;

public class RegionDiscoveryEvent extends ApplicationEvent {

    private final double latitude;
    private final double longitude;
    private final double radiusKm;

    public RegionDiscoveryEvent(Object source, double latitude, double longitude, double radiusKm) {
        super(source);
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getRadiusKm() {
        return radiusKm;
    }
}

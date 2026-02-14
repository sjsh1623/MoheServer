package com.mohe.spring.event;

import com.mohe.spring.service.RegionDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RegionDiscoveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(RegionDiscoveryEventListener.class);

    private final RegionDiscoveryService regionDiscoveryService;

    public RegionDiscoveryEventListener(RegionDiscoveryService regionDiscoveryService) {
        this.regionDiscoveryService = regionDiscoveryService;
    }

    @Async("taskExecutor")
    @EventListener
    public void handleRegionDiscovery(RegionDiscoveryEvent event) {
        log.info("[RegionDiscoveryListener] Processing discovery for lat={}, lon={}",
                event.getLatitude(), event.getLongitude());

        try {
            regionDiscoveryService.discoverPlaces(
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getRadiusKm()
            );
        } catch (Exception e) {
            log.error("[RegionDiscoveryListener] Discovery failed: {}", e.getMessage());
        }
    }
}

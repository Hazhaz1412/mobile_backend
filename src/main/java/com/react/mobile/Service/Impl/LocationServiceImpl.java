package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.LocationUpdateRequest;
import com.react.mobile.DTO.response.LocationDiscoveryResponse;
import com.react.mobile.DTO.response.LocationRouteResponse;
import com.react.mobile.DTO.response.LocationSnapshotResponse;
import com.react.mobile.DTO.response.NearbyPlaceResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.InterestType;
import com.react.mobile.Entity.LocationHistory;
import com.react.mobile.Entity.UserLocation;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Repository.LocationHistoryRepository;
import com.react.mobile.Repository.UserLocationRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_RADIUS_KM = 8.0;

    private final UserLocationRepository userLocationRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final UserProfileRepository userProfileRepository;

    private static final List<CatalogPlace> CATALOG = List.of(
            new CatalogPlace("poi-dragon-bridge", "Dragon Bridge", "POI", "CULTURE",
                    "Nguyen Van Linh, Da Nang", 16.0618, 108.2238),
            new CatalogPlace("poi-han-market", "Han Market", "POI", "SHOPPING",
                    "46 Bach Dang, Da Nang", 16.0678, 108.2247),
            new CatalogPlace("poi-my-khe-beach", "My Khe Beach", "POI", "NATURE",
                    "Vo Nguyen Giap, Da Nang", 16.0593, 108.2451),
            new CatalogPlace("poi-son-tra-trail", "Son Tra Peninsula Trail", "POI", "ADVENTURE",
                    "Son Tra, Da Nang", 16.1124, 108.2772),
            new CatalogPlace("poi-hoi-an-ancient-town", "Hoi An Ancient Town", "POI", "CULTURE",
                    "Hoi An, Quang Nam", 15.8801, 108.3380),
            new CatalogPlace("poi-night-food-lane", "Night Food Lane", "POI", "FOOD",
                    "Tran Phu, Da Nang", 16.0672, 108.2219),
            new CatalogPlace("event-riverside-music", "Riverside Acoustic Night", "EVENT", "CULTURE",
                    "Han Riverside Stage, Da Nang", 16.0715, 108.2245),
            new CatalogPlace("event-local-food-tour", "Local Street Food Tour", "EVENT", "FOOD",
                    "Le Duan, Da Nang", 16.0731, 108.2156),
            new CatalogPlace("event-weekend-market", "Weekend Craft Market", "EVENT", "SHOPPING",
                    "Tran Hung Dao, Da Nang", 16.0654, 108.2305),
            new CatalogPlace("event-sunrise-hike", "Sunrise Hill Hike Meetup", "EVENT", "ADVENTURE",
                    "Son Tra Entry Gate", 16.1040, 108.2678)
    );

    @Override
    @Transactional
    public LocationSnapshotResponse updateRealtimeLocation(AuthUser authUser, LocationUpdateRequest request) {
        return saveLocation(authUser, request, false);
    }

    @Override
    @Transactional
    public LocationSnapshotResponse updateManualLocation(AuthUser authUser, LocationUpdateRequest request) {
        return saveLocation(authUser, request, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationSnapshotResponse getCurrentLocation(AuthUser authUser) {
        UserLocation location = userLocationRepository.findByUserId(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("No current location found for this account"));
        return mapLocation(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationSnapshotResponse> getLocationHistory(AuthUser authUser, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 200);
        return locationHistoryRepository.findByUserIdOrderByTimestampDesc(authUser.getId())
                .stream()
                .limit(safeLimit)
                .map(item -> LocationSnapshotResponse.builder()
                        .latitude(item.getLatitude())
                        .longitude(item.getLongitude())
                        .locationName(item.getLocationName())
                        .manualOverride(false)
                        .updatedAt(item.getTimestamp())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDiscoveryResponse discoverNearby(AuthUser authUser, Double latitude, Double longitude, Double radiusKm) {
        LocationReference reference = resolveReference(authUser, latitude, longitude);
        double safeRadius = clampRadius(radiusKm);

        Set<InterestType> preferredInterests = userProfileRepository.findByAuthUser(authUser)
                .map(UserProfile::getInterests)
                .filter(Objects::nonNull)
                .orElse(Set.of());
        String travelStyle = userProfileRepository.findByAuthUser(authUser)
                .map(UserProfile::getTravelStyle)
                .orElse("");

        List<ScoredPlace> allDistances = CATALOG.stream()
                .map(item -> {
                    double distance = haversineKm(reference.latitude, reference.longitude, item.latitude, item.longitude);
                    int score = buildRecommendationScore(item, distance, preferredInterests, travelStyle);
                    return new ScoredPlace(item, round(distance), score);
                })
                .sorted(Comparator.comparingDouble(value -> value.distanceKm))
                .toList();

        List<ScoredPlace> nearby = allDistances.stream()
                .filter(item -> item.distanceKm <= safeRadius)
                .toList();

        List<ScoredPlace> pool = nearby.isEmpty() ? allDistances.stream().limit(8).toList() : nearby;

        List<NearbyPlaceResponse> pois = pool.stream()
                .filter(item -> "POI".equals(item.place.type))
                .sorted(Comparator.comparingDouble(value -> value.distanceKm))
                .map(item -> toNearbyResponse(reference, item))
                .toList();

        List<NearbyPlaceResponse> events = pool.stream()
                .filter(item -> "EVENT".equals(item.place.type))
                .sorted(Comparator.comparingDouble(value -> value.distanceKm))
                .map(item -> toNearbyResponse(reference, item))
                .toList();

        List<NearbyPlaceResponse> recommendations = pool.stream()
                .sorted((a, b) -> {
                    int scoreCompare = Integer.compare(b.score, a.score);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return Double.compare(a.distanceKm, b.distanceKm);
                })
                .limit(6)
                .map(item -> toNearbyResponse(reference, item))
                .toList();

        return LocationDiscoveryResponse.builder()
                .referenceLocation(LocationSnapshotResponse.builder()
                        .latitude(reference.latitude)
                        .longitude(reference.longitude)
                        .locationName(reference.locationName)
                        .manualOverride(reference.manualOverride)
                        .updatedAt(reference.updatedAt)
                        .build())
                .radiusKm(safeRadius)
                .pointsOfInterest(pois)
                .events(events)
                .recommendations(recommendations)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationRouteResponse getRoute(
            AuthUser authUser,
            Double fromLatitude,
            Double fromLongitude,
            Double toLatitude,
            Double toLongitude,
            String mode) {
        if (toLatitude == null || toLongitude == null) {
            throw new IllegalArgumentException("toLatitude and toLongitude are required");
        }
        validateCoordinates(toLatitude, toLongitude);

        LocationReference reference = resolveReference(authUser, fromLatitude, fromLongitude);
        String normalizedMode = normalizeMode(mode);

        double distanceKm = round(haversineKm(reference.latitude, reference.longitude, toLatitude, toLongitude));
        int estimatedMinutes = estimateMinutes(distanceKm, normalizedMode);

        String googleMapsUrl = String.format(
                Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%.6f,%.6f&destination=%.6f,%.6f&travelmode=%s",
                reference.latitude,
                reference.longitude,
                toLatitude,
                toLongitude,
                googleMode(normalizedMode)
        );

        String mapboxDirectionsUrl = String.format(
                Locale.US,
                "https://www.mapbox.com/directions/?origin=%.6f,%.6f&destination=%.6f,%.6f&profile=mapbox/%s",
                reference.longitude,
                reference.latitude,
                toLongitude,
                toLatitude,
                mapboxMode(normalizedMode)
        );

        return LocationRouteResponse.builder()
                .fromLatitude(reference.latitude)
                .fromLongitude(reference.longitude)
                .toLatitude(toLatitude)
                .toLongitude(toLongitude)
                .distanceKm(distanceKm)
                .estimatedMinutes(estimatedMinutes)
                .travelMode(normalizedMode)
                .googleMapsUrl(googleMapsUrl)
                .mapboxDirectionsUrl(mapboxDirectionsUrl)
                .build();
    }

    private LocationSnapshotResponse saveLocation(AuthUser authUser, LocationUpdateRequest request, boolean manual) {
        validateCoordinates(request.getLatitude(), request.getLongitude());

        UserLocation location = userLocationRepository.findByUserId(authUser.getId())
                .orElseGet(() -> UserLocation.builder().user(authUser).build());

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setLocationName(trimToNull(request.getLocationName()));
        location.setIsManual(manual);

        UserLocation saved = userLocationRepository.save(location);

        LocationHistory history = LocationHistory.builder()
                .user(authUser)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(trimToNull(request.getLocationName()))
                .build();
        locationHistoryRepository.save(history);

        return mapLocation(saved);
    }

    private LocationSnapshotResponse mapLocation(UserLocation location) {
        return LocationSnapshotResponse.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .locationName(location.getLocationName())
                .manualOverride(Boolean.TRUE.equals(location.getIsManual()))
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    private LocationReference resolveReference(AuthUser authUser, Double latitude, Double longitude) {
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("latitude and longitude must be provided together");
            }
            validateCoordinates(latitude, longitude);
            return new LocationReference(latitude, longitude, "Manual query point", true, LocalDateTime.now());
        }

        UserLocation saved = userLocationRepository.findByUserId(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("No current location found for this account"));
        return new LocationReference(
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getLocationName(),
                Boolean.TRUE.equals(saved.getIsManual()),
                saved.getUpdatedAt()
        );
    }

    private NearbyPlaceResponse toNearbyResponse(LocationReference reference, ScoredPlace scored) {
        String navigationUrl = String.format(
                Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%.6f,%.6f&destination=%.6f,%.6f&travelmode=walking",
                reference.latitude,
                reference.longitude,
                scored.place.latitude,
                scored.place.longitude
        );

        return NearbyPlaceResponse.builder()
                .id(scored.place.id)
                .name(scored.place.name)
                .type(scored.place.type)
                .category(scored.place.category)
                .address(scored.place.address)
                .latitude(scored.place.latitude)
                .longitude(scored.place.longitude)
                .distanceKm(scored.distanceKm)
                .recommendationScore(scored.score)
                .navigationUrl(navigationUrl)
                .build();
    }

    private double clampRadius(Double radiusKm) {
        if (radiusKm == null) {
            return DEFAULT_RADIUS_KM;
        }
        return Math.max(1.0, Math.min(radiusKm, 50.0));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private int buildRecommendationScore(CatalogPlace place, double distanceKm, Set<InterestType> interests, String travelStyle) {
        int score = 45;
        score += Math.max(0, 28 - (int) Math.round(distanceKm * 5));

        InterestType tag = toInterest(place.category);
        if (tag != null && interests.contains(tag)) {
            score += 22;
        }

        String style = travelStyle == null ? "" : travelStyle.trim().toUpperCase(Locale.ROOT);
        if ("GROUP".equals(style) && "EVENT".equals(place.type)) {
            score += 10;
        } else if ("SOLO".equals(style) && "POI".equals(place.type)) {
            score += 6;
        } else if ("FAMILY".equals(style) && ("POI".equals(place.type) || "EVENT".equals(place.type))) {
            score += 8;
        }

        return Math.max(1, Math.min(score, 99));
    }

    private InterestType toInterest(String category) {
        try {
            return InterestType.valueOf(category.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "walking";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "walking", "driving", "bicycling" -> normalized;
            default -> throw new IllegalArgumentException("Mode must be one of: walking, driving, bicycling");
        };
    }

    private int estimateMinutes(double distanceKm, String mode) {
        double speedKmPerHour = switch (mode) {
            case "driving" -> 35.0;
            case "bicycling" -> 16.0;
            default -> 4.8;
        };
        return Math.max(1, (int) Math.ceil((distanceKm / speedKmPerHour) * 60.0));
    }

    private String googleMode(String mode) {
        return switch (mode) {
            case "driving" -> "driving";
            case "bicycling" -> "bicycling";
            default -> "walking";
        };
    }

    private String mapboxMode(String mode) {
        return switch (mode) {
            case "driving" -> "driving";
            case "bicycling" -> "cycling";
            default -> "walking";
        };
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class CatalogPlace {
        private final String id;
        private final String name;
        private final String type;
        private final String category;
        private final String address;
        private final double latitude;
        private final double longitude;

        private CatalogPlace(String id, String name, String type, String category, String address, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.category = category;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final class ScoredPlace {
        private final CatalogPlace place;
        private final double distanceKm;
        private final int score;

        private ScoredPlace(CatalogPlace place, double distanceKm, int score) {
            this.place = place;
            this.distanceKm = distanceKm;
            this.score = score;
        }
    }

    private static final class LocationReference {
        private final double latitude;
        private final double longitude;
        private final String locationName;
        private final boolean manualOverride;
        private final LocalDateTime updatedAt;

        private LocationReference(double latitude, double longitude, String locationName, boolean manualOverride, LocalDateTime updatedAt) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.locationName = locationName;
            this.manualOverride = manualOverride;
            this.updatedAt = updatedAt;
        }
    }
}

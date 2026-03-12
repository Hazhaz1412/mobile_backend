package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.CreateTravelPlanItemRequest;
import com.react.mobile.DTO.request.CreateTravelPlanRequest;
import com.react.mobile.DTO.request.OptimizeRouteRequest;
import com.react.mobile.DTO.request.UpdateTravelPlanItemRequest;
import com.react.mobile.DTO.request.UpdateTravelPlanRequest;
import com.react.mobile.DTO.response.TravelPlanDetailResponse;
import com.react.mobile.DTO.response.TravelPlanItemResponse;
import com.react.mobile.DTO.response.TravelPlanShareResponse;
import com.react.mobile.DTO.response.TravelPlanSummaryResponse;
import com.react.mobile.DTO.response.TravelRouteOptimizationResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.Enums.TravelPlanItemType;
import com.react.mobile.Entity.TravelPlan;
import com.react.mobile.Entity.TravelPlanItem;
import com.react.mobile.Repository.TravelPlanItemRepository;
import com.react.mobile.Repository.TravelPlanRepository;
import com.react.mobile.Service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TravelPlanServiceImpl implements TravelPlanService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanItemRepository travelPlanItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TravelPlanSummaryResponse> listPlans(AuthUser user) {
        return travelPlanRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional
    public TravelPlanDetailResponse createPlan(AuthUser user, CreateTravelPlanRequest request) {
        String title = requireText(request.getTitle(), "Plan title is required");
        LocalDate startDate = parseDate(request.getStartDate());
        LocalDate endDate = parseDate(request.getEndDate());
        validateDateRange(startDate, endDate);

        TravelPlan plan = TravelPlan.builder()
                .user(user)
                .title(title)
                .description(trimToNull(request.getDescription()))
                .startDate(startDate)
                .endDate(endDate)
                .isPublic(false)
                .build();

        plan = travelPlanRepository.save(plan);
        return toDetailResponse(plan, true);
    }

    @Override
    @Transactional(readOnly = true)
    public TravelPlanDetailResponse getPlan(AuthUser user, Long planId) {
        TravelPlan plan = getOwnedPlan(user, planId);
        return toDetailResponse(plan, true);
    }

    @Override
    @Transactional
    public TravelPlanDetailResponse updatePlan(AuthUser user, Long planId, UpdateTravelPlanRequest request) {
        TravelPlan plan = getOwnedPlan(user, planId);

        if (request.getTitle() != null) {
            plan.setTitle(requireText(request.getTitle(), "Plan title is required"));
        }
        if (request.getDescription() != null) {
            plan.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(parseDateNullable(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(parseDateNullable(request.getEndDate()));
        }
        if (request.getIsPublic() != null) {
            plan.setIsPublic(request.getIsPublic());
        }

        validateDateRange(plan.getStartDate(), plan.getEndDate());
        plan = travelPlanRepository.save(plan);
        return toDetailResponse(plan, true);
    }

    @Override
    @Transactional
    public void deletePlan(AuthUser user, Long planId) {
        TravelPlan plan = getOwnedPlan(user, planId);
        travelPlanRepository.delete(plan);
    }

    @Override
    @Transactional
    public TravelPlanItemResponse addItem(AuthUser user, Long planId, CreateTravelPlanItemRequest request) {
        TravelPlan plan = getOwnedPlan(user, planId);

        Integer dayNumber = normalizeDayNumber(request.getDayNumber());
        TravelPlanItemType itemType = parseItemType(request.getItemType());
        String title = normalizeItemTitle(request.getTitle(), itemType);

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        validateCoordinates(latitude, longitude);

        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        validateTimeRange(startTime, endTime);

        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            Integer maxSort = travelPlanItemRepository.findMaxSortOrderByPlanAndDay(plan.getId(), dayNumber);
            sortOrder = (maxSort == null ? 0 : maxSort) + 1;
        }

        TravelPlanItem item = TravelPlanItem.builder()
                .plan(plan)
                .dayNumber(dayNumber)
                .sortOrder(Math.max(sortOrder, 0))
                .itemType(itemType)
                .referenceId(trimToNull(request.getReferenceId()))
                .title(title)
                .locationName(trimToNull(request.getLocationName()))
                .latitude(latitude)
                .longitude(longitude)
                .startTime(startTime)
                .endTime(endTime)
                .note(trimToNull(request.getNote()))
                .reminderAt(parseDateTime(request.getReminderAt()))
                .build();

        item = travelPlanItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public TravelPlanItemResponse updateItem(AuthUser user, Long planId, Long itemId, UpdateTravelPlanItemRequest request) {
        TravelPlan plan = getOwnedPlan(user, planId);
        TravelPlanItem item = travelPlanItemRepository.findByIdAndPlanId(itemId, plan.getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan item not found"));

        if (request.getDayNumber() != null) {
            item.setDayNumber(normalizeDayNumber(request.getDayNumber()));
        }
        if (request.getItemType() != null) {
            item.setItemType(parseItemType(request.getItemType()));
        }
        if (request.getReferenceId() != null) {
            item.setReferenceId(trimToNull(request.getReferenceId()));
        }
        if (request.getTitle() != null) {
            item.setTitle(normalizeItemTitle(request.getTitle(), item.getItemType()));
        }
        if (request.getLocationName() != null) {
            item.setLocationName(trimToNull(request.getLocationName()));
        }

        Double latitude = item.getLatitude();
        Double longitude = item.getLongitude();
        if (request.getLatitude() != null) {
            latitude = request.getLatitude();
        }
        if (request.getLongitude() != null) {
            longitude = request.getLongitude();
        }
        validateCoordinates(latitude, longitude);
        item.setLatitude(latitude);
        item.setLongitude(longitude);

        if (request.getStartTime() != null) {
            item.setStartTime(parseDateTimeNullable(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            item.setEndTime(parseDateTimeNullable(request.getEndTime()));
        }
        validateTimeRange(item.getStartTime(), item.getEndTime());

        if (request.getNote() != null) {
            item.setNote(trimToNull(request.getNote()));
        }
        if (request.getReminderAt() != null) {
            item.setReminderAt(parseDateTimeNullable(request.getReminderAt()));
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(Math.max(request.getSortOrder(), 0));
        }

        item = travelPlanItemRepository.save(item);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public void deleteItem(AuthUser user, Long planId, Long itemId) {
        TravelPlan plan = getOwnedPlan(user, planId);
        TravelPlanItem item = travelPlanItemRepository.findByIdAndPlanId(itemId, plan.getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan item not found"));
        travelPlanItemRepository.delete(item);
    }

    @Override
    @Transactional
    public TravelPlanShareResponse sharePlan(AuthUser user, Long planId) {
        TravelPlan plan = getOwnedPlan(user, planId);

        if (plan.getShareToken() == null || plan.getShareToken().isBlank()) {
            plan.setShareToken(generateUniqueShareToken());
        }
        plan.setIsPublic(true);
        plan = travelPlanRepository.save(plan);

        return TravelPlanShareResponse.builder()
                .planId(plan.getId())
                .shareToken(plan.getShareToken())
                .sharePath(buildSharePath(plan.getShareToken()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TravelPlanDetailResponse getSharedPlan(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            throw new IllegalArgumentException("Share token is required");
        }

        TravelPlan plan = travelPlanRepository.findByShareTokenAndIsPublicTrue(shareToken.trim())
                .orElseThrow(() -> new IllegalArgumentException("Shared plan not found"));

        return toDetailResponse(plan, false);
    }

    @Override
    @Transactional
    public TravelRouteOptimizationResponse optimizeRoute(AuthUser user, Long planId, OptimizeRouteRequest request) {
        TravelPlan plan = getOwnedPlan(user, planId);

        Integer dayNumber = normalizeDayNumber(request != null ? request.getDayNumber() : null);
        String mode = normalizeMode(request != null ? request.getMode() : null);

        Double startLatitude = request != null ? request.getStartLatitude() : null;
        Double startLongitude = request != null ? request.getStartLongitude() : null;
        if ((startLatitude == null) != (startLongitude == null)) {
            throw new IllegalArgumentException("startLatitude and startLongitude must be provided together");
        }
        if (startLatitude != null) {
            validateCoordinateRange(startLatitude, -90.0, 90.0, "Latitude");
            validateCoordinateRange(startLongitude, -180.0, 180.0, "Longitude");
        }

        List<TravelPlanItem> dayItems = travelPlanItemRepository.findByPlanIdAndDayNumberOrderBySortOrderAscStartTimeAsc(
                plan.getId(), dayNumber);

        if (dayItems.isEmpty()) {
            throw new IllegalArgumentException("No itinerary items found for this day");
        }

        List<TravelPlanItem> itemsWithCoords = dayItems.stream()
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .toList();

        List<TravelPlanItem> itemsWithoutCoords = dayItems.stream()
                .filter(item -> item.getLatitude() == null || item.getLongitude() == null)
                .toList();

        List<TravelPlanItem> orderedWithCoords = new ArrayList<>();
        if (!itemsWithCoords.isEmpty()) {
            List<TravelPlanItem> remaining = new ArrayList<>(itemsWithCoords);
            double currentLat;
            double currentLon;

            if (startLatitude != null && startLongitude != null) {
                currentLat = startLatitude;
                currentLon = startLongitude;
            } else {
                TravelPlanItem first = remaining.remove(0);
                orderedWithCoords.add(first);
                currentLat = first.getLatitude();
                currentLon = first.getLongitude();
            }

            while (!remaining.isEmpty()) {
                double baseLat = currentLat;
                double baseLon = currentLon;
                TravelPlanItem next = remaining.stream()
                        .min(Comparator.comparingDouble(item -> haversineKm(
                                baseLat,
                                baseLon,
                                item.getLatitude(),
                                item.getLongitude())))
                        .orElseThrow();
                orderedWithCoords.add(next);
                remaining.remove(next);
                currentLat = next.getLatitude();
                currentLon = next.getLongitude();
            }
        }

        List<TravelPlanItem> finalOrder = new ArrayList<>(orderedWithCoords);
        finalOrder.addAll(itemsWithoutCoords);

        int order = 1;
        for (TravelPlanItem item : finalOrder) {
            item.setSortOrder(order++);
        }
        travelPlanItemRepository.saveAll(finalOrder);

        List<TravelPlanItem> optimized = travelPlanItemRepository.findByPlanIdAndDayNumberOrderBySortOrderAscStartTimeAsc(
                plan.getId(), dayNumber);

        List<TravelPlanItem> optimizedWithCoords = optimized.stream()
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .toList();

        List<TravelRouteOptimizationResponse.RouteLeg> legs = new ArrayList<>();
        double totalDistanceKm = 0.0;
        int totalEstimatedMinutes = 0;

        for (int i = 0; i < optimizedWithCoords.size() - 1; i++) {
            TravelPlanItem from = optimizedWithCoords.get(i);
            TravelPlanItem to = optimizedWithCoords.get(i + 1);
            double distanceKm = round(haversineKm(
                    from.getLatitude(),
                    from.getLongitude(),
                    to.getLatitude(),
                    to.getLongitude()
            ));
            int estimatedMinutes = estimateMinutes(distanceKm, mode);

            totalDistanceKm += distanceKm;
            totalEstimatedMinutes += estimatedMinutes;

            legs.add(TravelRouteOptimizationResponse.RouteLeg.builder()
                    .fromTitle(labelOf(from))
                    .toTitle(labelOf(to))
                    .distanceKm(distanceKm)
                    .estimatedMinutes(estimatedMinutes)
                    .googleMapsUrl(buildGoogleMapsUrl(
                            from.getLatitude(),
                            from.getLongitude(),
                            to.getLatitude(),
                            to.getLongitude(),
                            mode
                    ))
                    .mapboxDirectionsUrl(buildMapboxUrl(
                            from.getLatitude(),
                            from.getLongitude(),
                            to.getLatitude(),
                            to.getLongitude(),
                            mode
                    ))
                    .build());
        }

        return TravelRouteOptimizationResponse.builder()
                .planId(plan.getId())
                .dayNumber(dayNumber)
                .mode(mode)
                .totalDistanceKm(round(totalDistanceKm))
                .estimatedMinutes(totalEstimatedMinutes)
                .optimizedItems(optimized.stream().map(this::toItemResponse).toList())
                .legs(legs)
                .build();
    }

    private TravelPlan getOwnedPlan(AuthUser user, Long planId) {
        return travelPlanRepository.findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Travel plan not found"));
    }

    private TravelPlanSummaryResponse toSummaryResponse(TravelPlan plan) {
        long itemCount = travelPlanItemRepository.countByPlanId(plan.getId());
        long dayCount = travelPlanItemRepository.countDistinctDays(plan.getId());

        return TravelPlanSummaryResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .startDate(formatDate(plan.getStartDate()))
                .endDate(formatDate(plan.getEndDate()))
                .isPublic(Boolean.TRUE.equals(plan.getIsPublic()))
                .itemCount(itemCount)
                .dayCount(dayCount)
                .createdAt(formatDateTime(plan.getCreatedAt()))
                .updatedAt(formatDateTime(plan.getUpdatedAt()))
                .build();
    }

    private TravelPlanDetailResponse toDetailResponse(TravelPlan plan, boolean includePrivateFields) {
        List<TravelPlanItemResponse> items = travelPlanItemRepository.findByPlanIdOrderByDayNumberAscSortOrderAscStartTimeAsc(plan.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return TravelPlanDetailResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .startDate(formatDate(plan.getStartDate()))
                .endDate(formatDate(plan.getEndDate()))
                .isPublic(Boolean.TRUE.equals(plan.getIsPublic()))
                .shareToken(includePrivateFields ? plan.getShareToken() : null)
                .sharePath(plan.getShareToken() != null ? buildSharePath(plan.getShareToken()) : null)
                .ownerUsername(plan.getUser() != null ? plan.getUser().getUsername() : null)
                .createdAt(formatDateTime(plan.getCreatedAt()))
                .updatedAt(formatDateTime(plan.getUpdatedAt()))
                .items(items)
                .build();
    }

    private TravelPlanItemResponse toItemResponse(TravelPlanItem item) {
        return TravelPlanItemResponse.builder()
                .id(item.getId())
                .dayNumber(item.getDayNumber())
                .sortOrder(item.getSortOrder())
                .itemType(item.getItemType() != null ? item.getItemType().name() : null)
                .referenceId(item.getReferenceId())
                .title(item.getTitle())
                .locationName(item.getLocationName())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .startTime(formatDateTime(item.getStartTime()))
                .endTime(formatDateTime(item.getEndTime()))
                .note(item.getNote())
                .reminderAt(formatDateTime(item.getReminderAt()))
                .createdAt(formatDateTime(item.getCreatedAt()))
                .updatedAt(formatDateTime(item.getUpdatedAt()))
                .build();
    }

    private Integer normalizeDayNumber(Integer dayNumber) {
        if (dayNumber == null || dayNumber <= 0) {
            throw new IllegalArgumentException("dayNumber must be greater than 0");
        }
        return dayNumber;
    }

    private TravelPlanItemType parseItemType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("itemType is required");
        }
        try {
            return TravelPlanItemType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported itemType: " + value);
        }
    }

    private String normalizeItemTitle(String value, TravelPlanItemType itemType) {
        if (itemType == TravelPlanItemType.NOTE) {
            String noteTitle = trimToNull(value);
            return noteTitle == null ? "Note" : noteTitle;
        }
        return requireText(value, "Item title is required");
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid date format. Expected ISO date: yyyy-MM-dd");
        }
    }

    private LocalDate parseDateNullable(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return null;
        }
        return parseDate(value);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FMT);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid datetime format. Expected ISO datetime: yyyy-MM-ddTHH:mm:ss");
        }
    }

    private LocalDateTime parseDateTimeNullable(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return null;
        }
        return parseDateTime(value);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be on or after startTime");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
        validateCoordinateRange(latitude, -90.0, 90.0, "Latitude");
        validateCoordinateRange(longitude, -180.0, 180.0, "Longitude");
    }

    private void validateCoordinateRange(Double value, double min, double max, String label) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException(label + " is out of range");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateUniqueShareToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (travelPlanRepository.existsByShareToken(token));
        return token;
    }

    private String buildSharePath(String shareToken) {
        return "/api/travel-plans/shared/" + shareToken;
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FMT);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FMT);
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

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private int estimateMinutes(double distanceKm, String mode) {
        double speedKmPerHour = switch (mode) {
            case "driving" -> 35.0;
            case "bicycling" -> 16.0;
            default -> 4.8;
        };
        return Math.max(1, (int) Math.ceil((distanceKm / speedKmPerHour) * 60.0));
    }

    private String buildGoogleMapsUrl(double fromLat, double fromLon, double toLat, double toLon, String mode) {
        return String.format(
                Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%.6f,%.6f&destination=%.6f,%.6f&travelmode=%s",
                fromLat,
                fromLon,
                toLat,
                toLon,
                googleMode(mode)
        );
    }

    private String buildMapboxUrl(double fromLat, double fromLon, double toLat, double toLon, String mode) {
        return String.format(
                Locale.US,
                "https://www.mapbox.com/directions/?origin=%.6f,%.6f&destination=%.6f,%.6f&profile=mapbox/%s",
                fromLon,
                fromLat,
                toLon,
                toLat,
                mapboxMode(mode)
        );
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

    private String labelOf(TravelPlanItem item) {
        String title = trimToNull(item.getTitle());
        if (title != null) {
            return title;
        }
        String location = trimToNull(item.getLocationName());
        if (location != null) {
            return location;
        }
        return "Stop " + item.getId();
    }
}

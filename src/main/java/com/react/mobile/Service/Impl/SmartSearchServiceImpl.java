package com.react.mobile.Service.Impl;

import com.react.mobile.DTO.request.ItineraryRequest;
import com.react.mobile.DTO.request.SmartSearchRequest;
import com.react.mobile.DTO.response.DiscoveryBrowseResponse;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.DTO.response.ItineraryResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.PlaceEmbedding;
import com.react.mobile.Repository.PlaceEmbeddingRepository;
import com.react.mobile.Service.DiscoveryService;
import com.react.mobile.Service.SmartSearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartSearchServiceImpl implements SmartSearchService {

    private final DiscoveryService discoveryService;
    private final PlaceEmbeddingRepository placeEmbeddingRepository;
    private final ObjectMapper objectMapper;

    // Master tag vocabulary for vector computation
    private static final List<String> TAG_VOCAB = List.of(
            "food", "restaurant", "cafe", "street_food", "bar", "bakery",
            "museum", "temple", "pagoda", "church", "historic", "heritage",
            "park", "garden", "lake", "beach", "mountain", "nature",
            "shopping", "market", "mall", "boutique",
            "sports", "gym", "stadium", "adventure", "hiking", "cycling",
            "nightlife", "entertainment", "cinema", "theater", "karaoke",
            "hotel", "hostel", "resort", "spa", "wellness",
            "viewpoint", "landmark", "monument", "bridge", "tower",
            "tour", "experience", "workshop", "cooking_class", "art"
    );

    // Mood → tag weight mapping
    private static final Map<String, Map<String, Double>> MOOD_WEIGHTS = Map.of(
            "romantic", Map.of("cafe", 2.0, "restaurant", 2.0, "lake", 1.5, "viewpoint", 2.0, "park", 1.5, "spa", 1.5),
            "adventure", Map.of("hiking", 2.5, "adventure", 2.5, "cycling", 2.0, "mountain", 2.0, "nature", 1.5, "tour", 1.5),
            "cultural", Map.of("museum", 2.5, "temple", 2.0, "pagoda", 2.0, "historic", 2.0, "heritage", 2.0, "art", 1.5),
            "foodie", Map.of("food", 3.0, "restaurant", 2.5, "cafe", 2.0, "street_food", 2.5, "bakery", 1.5, "market", 2.0),
            "relaxing", Map.of("spa", 2.5, "park", 2.0, "garden", 2.0, "lake", 2.0, "beach", 2.0, "cafe", 1.5),
            "nightlife", Map.of("nightlife", 3.0, "bar", 2.5, "entertainment", 2.0, "karaoke", 1.5, "restaurant", 1.5),
            "shopping", Map.of("shopping", 3.0, "market", 2.5, "mall", 2.0, "boutique", 2.0)
    );

    @Override
    @Transactional(readOnly = true)
    public List<DiscoveryItemResponse> similarPlaces(AuthUser user, String placeId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        // Get the source place detail to extract tags
        DiscoveryBrowseResponse browseResult = discoveryService.browse(
                user, null, "ALL", null, null, null, 50.0,
                "RELEVANCE", null, null, 40, 0);

        List<DiscoveryItemResponse> allItems = browseResult.getItems();
        if (allItems == null || allItems.isEmpty()) {
            return List.of();
        }

        // Find source item
        DiscoveryItemResponse sourceItem = allItems.stream()
                .filter(item -> placeId.equals(item.getId()))
                .findFirst()
                .orElse(null);

        if (sourceItem == null && !allItems.isEmpty()) {
            // If source item not found in browse, still compute similarity using tag vectors
            // from stored embeddings or return top rated items
            return allItems.stream()
                    .filter(item -> !placeId.equals(item.getId()))
                    .limit(safeLimit)
                    .collect(Collectors.toList());
        }

        if (sourceItem == null) {
            return List.of();
        }

        // Compute tag vectors and find similar places
        double[] sourceVector = computeTagVector(sourceItem.getTags(), sourceItem.getCategory());
        String sourceCategory = sourceItem.getCategory();

        return allItems.stream()
                .filter(item -> !placeId.equals(item.getId()))
                .map(item -> {
                    double[] itemVector = computeTagVector(item.getTags(), item.getCategory());
                    double similarity = cosineSimilarity(sourceVector, itemVector);
                    // Boost same-category items
                    if (sourceCategory != null && sourceCategory.equals(item.getCategory())) {
                        similarity += 0.15;
                    }
                    return new AbstractMap.SimpleEntry<>(item, similarity);
                })
                .sorted(Map.Entry.<DiscoveryItemResponse, Double>comparingByValue().reversed())
                .limit(safeLimit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscoveryItemResponse> smartSearch(AuthUser user, SmartSearchRequest request) {
        double maxDistance = request.getMaxDistanceKm() != null ? request.getMaxDistanceKm() : 15.0;
        int maxPrice = 4;
        if (request.getBudget() != null) {
            if (request.getBudget() <= 50000) maxPrice = 1;
            else if (request.getBudget() <= 200000) maxPrice = 2;
            else if (request.getBudget() <= 500000) maxPrice = 3;
        }

        // Determine category from mood
        String category = "ALL";
        String mood = request.getMood() != null ? request.getMood().toLowerCase(Locale.ROOT) : "";
        if (mood.contains("food") || mood.contains("ăn")) category = "CUISINE";
        else if (mood.contains("cultur") || mood.contains("văn hóa")) category = "ATTRACTION";
        else if (mood.contains("adventure") || mood.contains("phiêu lưu")) category = "ACTIVITY";

        DiscoveryBrowseResponse browseResult = discoveryService.browse(
                user, null, category, null, maxPrice, null, maxDistance,
                "RELEVANCE",
                request.getLatitude(), request.getLongitude(),
                30, 0);

        if (browseResult.getItems() == null) {
            return List.of();
        }

        Map<String, Double> moodWeights = MOOD_WEIGHTS.getOrDefault(mood, Map.of());

        return browseResult.getItems().stream()
                .map(item -> {
                    double score = item.getRating() * 10;
                    score += item.getPopularityScore() * 0.5;
                    score -= item.getDistanceKm() * 2;

                    // Mood boost
                    if (item.getTags() != null) {
                        for (String tag : item.getTags()) {
                            String normalizedTag = tag.toLowerCase(Locale.ROOT).replace(" ", "_");
                            score += moodWeights.getOrDefault(normalizedTag, 0.0) * 5;
                        }
                    }

                    // Time fit
                    if (request.getFreeHours() != null && request.getFreeHours() < 2 && item.getDistanceKm() > 5) {
                        score -= 20;
                    }

                    return new AbstractMap.SimpleEntry<>(item, score);
                })
                .sorted(Map.Entry.<DiscoveryItemResponse, Double>comparingByValue().reversed())
                .limit(15)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItineraryResponse generateItinerary(AuthUser user, ItineraryRequest request) {
        double durationHours = request.getDurationHours() != null ? request.getDurationHours() : 4.0;
        double maxDistance = request.getMaxDistanceKm() != null ? request.getMaxDistanceKm() : 10.0;
        String mood = request.getMood() != null ? request.getMood().toLowerCase(Locale.ROOT) : "relaxing";

        LocalTime startTimeObj;
        try {
            startTimeObj = request.getStartTime() != null
                    ? LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"))
                    : LocalTime.of(18, 0);
        } catch (Exception e) {
            startTimeObj = LocalTime.of(18, 0);
        }

        // Get places scored by mood
        SmartSearchRequest smartReq = new SmartSearchRequest();
        smartReq.setBudget(request.getBudget());
        smartReq.setMood(mood);
        smartReq.setMaxDistanceKm(maxDistance);
        smartReq.setLatitude(request.getLatitude());
        smartReq.setLongitude(request.getLongitude());

        List<DiscoveryItemResponse> candidates = smartSearch(user, smartReq);
        if (candidates.isEmpty()) {
            return ItineraryResponse.builder()
                    .totalHours(durationHours)
                    .startTime(startTimeObj.toString())
                    .endTime(startTimeObj.plusMinutes((long) (durationHours * 60)).toString())
                    .mood(mood)
                    .slots(List.of())
                    .build();
        }

        // Greedy slot-filling: allocate 30-90 min per stop
        List<ItineraryResponse.ItinerarySlot> slots = new ArrayList<>();
        double remainingMinutes = durationHours * 60;
        LocalTime currentTime = startTimeObj;
        int order = 1;
        Set<String> visited = new HashSet<>();

        for (DiscoveryItemResponse item : candidates) {
            if (remainingMinutes < 30) break;
            if (visited.contains(item.getId())) continue;
            visited.add(item.getId());

            int duration = estimateDuration(item.getCategory());
            if (duration > remainingMinutes) duration = (int) remainingMinutes;

            LocalTime slotEnd = currentTime.plusMinutes(duration);

            slots.add(ItineraryResponse.ItinerarySlot.builder()
                    .order(order++)
                    .startTime(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .endTime(slotEnd.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .durationMinutes(duration)
                    .placeId(item.getId())
                    .placeName(item.getName())
                    .category(item.getCategory())
                    .rating(item.getRating())
                    .priceLevel(item.getPriceLevel())
                    .distanceKm(item.getDistanceKm())
                    .latitude(item.getLatitude())
                    .longitude(item.getLongitude())
                    .directionsUrl(item.getDirectionsUrl())
                    .note(generateNote(item, mood))
                    .build());

            currentTime = slotEnd.plusMinutes(15); // 15 min travel between stops
            remainingMinutes -= (duration + 15);
        }

        LocalTime finalEnd = startTimeObj.plusMinutes((long) (durationHours * 60));

        return ItineraryResponse.builder()
                .totalHours(durationHours)
                .startTime(startTimeObj.format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(finalEnd.format(DateTimeFormatter.ofPattern("HH:mm")))
                .mood(mood)
                .slots(slots)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double[] computeTagVector(List<String> tags, String category) {
        double[] vector = new double[TAG_VOCAB.size()];
        if (tags != null) {
            for (String tag : tags) {
                String normalized = tag.toLowerCase(Locale.ROOT).replace(" ", "_");
                int idx = TAG_VOCAB.indexOf(normalized);
                if (idx >= 0) {
                    vector[idx] = 1.0;
                }
                // Also check partial matches
                for (int i = 0; i < TAG_VOCAB.size(); i++) {
                    if (normalized.contains(TAG_VOCAB.get(i)) || TAG_VOCAB.get(i).contains(normalized)) {
                        vector[i] = Math.max(vector[i], 0.5);
                    }
                }
            }
        }
        // Category boost
        if (category != null) {
            String cat = category.toLowerCase(Locale.ROOT);
            for (int i = 0; i < TAG_VOCAB.size(); i++) {
                if (TAG_VOCAB.get(i).contains(cat) || cat.contains(TAG_VOCAB.get(i))) {
                    vector[i] = Math.max(vector[i], 0.3);
                }
            }
        }
        return vector;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private int estimateDuration(String category) {
        if (category == null) return 45;
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "CUISINE" -> 60;
            case "ATTRACTION" -> 75;
            case "ACTIVITY" -> 90;
            default -> 45;
        };
    }

    private String generateNote(DiscoveryItemResponse item, String mood) {
        if ("CUISINE".equalsIgnoreCase(item.getCategory())) {
            return "🍜 Thưởng thức ẩm thực tại " + item.getName();
        }
        if ("ATTRACTION".equalsIgnoreCase(item.getCategory())) {
            return "📸 Tham quan và chụp ảnh tại " + item.getName();
        }
        if ("ACTIVITY".equalsIgnoreCase(item.getCategory())) {
            return "🎯 Trải nghiệm hoạt động tại " + item.getName();
        }
        return "📍 Ghé thăm " + item.getName();
    }
}

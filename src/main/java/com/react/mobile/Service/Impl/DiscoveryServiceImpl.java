package com.react.mobile.Service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.mobile.DTO.response.DiscoveryBrowseResponse;
import com.react.mobile.DTO.response.DiscoveryDetailResponse;
import com.react.mobile.DTO.response.DiscoveryItemResponse;
import com.react.mobile.Entity.AuthUser;
import com.react.mobile.Entity.DiscoveryBookmark;
import com.react.mobile.Entity.Enums.InterestType;
import com.react.mobile.Entity.UserLocation;
import com.react.mobile.Entity.UserProfile;
import com.react.mobile.Repository.DiscoveryBookmarkRepository;
import com.react.mobile.Repository.UserLocationRepository;
import com.react.mobile.Repository.UserProfileRepository;
import com.react.mobile.Service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String OVERPASS_BASE_URL = "https://overpass-api.de/api/interpreter";
    private static final String WIKIDATA_BASE_URL = "https://www.wikidata.org/wiki/Special:EntityData";
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_LAT = 16.0678;
    private static final double DEFAULT_LON = 108.2208;
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})");

    private final DiscoveryBookmarkRepository discoveryBookmarkRepository;
    private final UserLocationRepository userLocationRepository;
    private final UserProfileRepository userProfileRepository;
    private final com.react.mobile.Service.SocialService socialService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final Map<String, String> wikidataImageCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public DiscoveryBrowseResponse browse(
            AuthUser authUser,
            String query,
            String category,
            Double minRating,
            Integer maxPriceLevel,
            Integer minPopularity,
            Double maxDistanceKm,
            String sortBy,
            Double latitude,
            Double longitude,
            Integer limit,
            Integer page
    ) {
        ReferencePoint originReference = resolveReference(authUser, latitude, longitude);
        String safeQuery = normalizeQuery(query);
        String safeCategory = normalizeCategory(category);
        String safeSortBy = normalizeSort(sortBy);

        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 40));
        int safePage = page == null ? 0 : Math.max(0, page);
        int totalNeeded = (safePage + 1) * safeLimit;
        double safeMinRating = minRating == null ? 0.0 : Math.max(0.0, Math.min(minRating, 5.0));
        int safeMaxPriceLevel = maxPriceLevel == null ? 4 : Math.max(1, Math.min(maxPriceLevel, 4));
        int safeMinPopularity = minPopularity == null ? 0 : Math.max(0, Math.min(minPopularity, 100));
        double safeMaxDistanceKm = maxDistanceKm == null ? 30.0 : Math.max(1.0, Math.min(maxDistanceKm, 200.0));

        ProfileContext profile = resolveProfile(authUser);
        Set<String> bookmarkedIds = loadBookmarkedIds(authUser.getId());

        ReferencePoint discoveryReference = originReference;
        String effectiveQuery = safeQuery;
        Optional<ReferencePoint> queryAnchor = resolveQueryReference(safeQuery, originReference);
        if (queryAnchor.isPresent()) {
            discoveryReference = queryAnchor.get();
            effectiveQuery = "";
        }
        final ReferencePoint rankingReference = discoveryReference;
        final String rankingQuery = effectiveQuery;

        int fetchLimit = safeQuery.isBlank()
            ? Math.max(40, totalNeeded * 3)
            : Math.max(24, totalNeeded * 2);
        List<LivePlace> livePlaces = fetchLivePlaces(rankingReference, safeCategory, rankingQuery, fetchLimit, safeMaxDistanceKm, profile);
        if (livePlaces.isEmpty() && !safeQuery.isBlank()) {
            livePlaces = fetchLivePlaces(rankingReference, safeCategory, "", fetchLimit, safeMaxDistanceKm, profile);
        }

        List<RankedPlace> ranked = applyBrowseFilters(
                livePlaces,
                rankingReference,
                rankingQuery,
                safeCategory,
                safeMinRating,
                safeMaxPriceLevel,
                safeMinPopularity,
                safeMaxDistanceKm,
                safeSortBy,
                safeLimit,
                profile
        );

        // Avoid silent empty states for specific search input: relax strict filters once.
        if (ranked.isEmpty() && !rankingQuery.isBlank()) {
            ranked = applyBrowseFilters(
                    livePlaces,
                rankingReference,
                rankingQuery,
                    safeCategory,
                    Math.max(0.0, safeMinRating - 0.7),
                    safeMaxPriceLevel,
                    Math.max(0, safeMinPopularity - 20),
                    Math.max(120.0, safeMaxDistanceKm),
                    safeSortBy,
                    safeLimit,
                    profile
            );
        }

        if (ranked.isEmpty() && !rankingQuery.isBlank()) {
            int queryLimit = Math.max(20, safeLimit * 4);
            Map<String, LivePlace> queryOnlyMap = new LinkedHashMap<>();
            List<String> queryTerms = buildQuerySearchTerms(rankingQuery, safeCategory);

            for (String term : queryTerms) {
                List<LivePlace> found = searchNominatim(
                        term,
                        rankingReference,
                        Math.max(60.0, safeMaxDistanceKm * 2.5),
                        Math.max(12, queryLimit / Math.max(1, queryTerms.size())),
                        false
                );
                for (LivePlace place : found) {
                    queryOnlyMap.putIfAbsent(place.id, place);
                    if (queryOnlyMap.size() >= queryLimit) {
                        break;
                    }
                }
                if (queryOnlyMap.size() >= queryLimit) {
                    break;
                }
            }

            ranked = queryOnlyMap.values().stream()
                    .map(place -> rankPlace(place, rankingReference, rankingQuery, profile))
                    .filter(item -> rankingQuery.isBlank() || matchesQuery(item.place, rankingQuery))
                    .filter(item -> "ALL".equals(safeCategory) || safeCategory.equals(item.place.category))
                    .filter(item -> item.place.rating >= Math.max(0.0, safeMinRating - 0.5))
                    .filter(item -> item.place.priceLevel <= safeMaxPriceLevel)
                    .sorted(sortComparator(safeSortBy))
                    .limit(totalNeeded)
                    .toList();
        }

        if (ranked.isEmpty() && !livePlaces.isEmpty() && rankingQuery.isBlank()) {
            ranked = livePlaces.stream()
                .map(place -> rankPlace(place, rankingReference, rankingQuery, profile))
                .sorted(sortComparator(safeSortBy))
                .limit(totalNeeded)
                .toList();
        }

        List<DiscoveryItemResponse> allItems = ranked.stream()
            .map(item -> toItemResponse(originReference, item.place, item.distanceKm, bookmarkedIds.contains(item.place.id)))
                .toList();

        int totalItems = allItems.size();
        int fromIdx = safePage * safeLimit;
        int toIdx = Math.min(fromIdx + safeLimit, totalItems);
        List<DiscoveryItemResponse> items = fromIdx < totalItems
                ? allItems.subList(fromIdx, toIdx)
                : List.of();
        int totalPages = (int) Math.ceil((double) totalItems / safeLimit);

        return DiscoveryBrowseResponse.builder()
                .query(safeQuery)
                .category(safeCategory)
                .minRating(safeMinRating)
                .maxPriceLevel(safeMaxPriceLevel)
                .minPopularity(safeMinPopularity)
                .maxDistanceKm(safeMaxDistanceKm)
                .sortBy(safeSortBy)
                .referenceLatitude(rankingReference.latitude)
                .referenceLongitude(rankingReference.longitude)
            .autocompleteSuggestions(allItems.stream().map(DiscoveryItemResponse::getName).distinct().limit(7).toList())
                .items(items)
                .page(safePage)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .hasNext(toIdx < totalItems)
                .build();
    }

    private Optional<ReferencePoint> resolveQueryReference(String query, ReferencePoint fallback) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        List<String> variants = buildQueryVariants(query);
        for (String variant : variants) {
            try {
                String url = NOMINATIM_BASE_URL + "/search"
                        + "?format=jsonv2"
                        + "&addressdetails=1"
                        + "&limit=5"
                        + "&accept-language=" + encode("vi,en")
                        + "&q=" + encode(variant);

                JsonNode array = readJsonArray(url);
                Optional<ReferencePoint> best = pickBestAnchorFromCandidates(array, query, fallback);
                if (best.isPresent()) {
                    return best;
                }
            } catch (Exception ignored) {
                // Best effort geocode anchor.
            }
        }
        return Optional.empty();
    }

    private List<String> buildQueryVariants(String query) {
        String raw = query.trim();
        if (raw.isBlank()) {
            return List.of();
        }
        String ascii = stripDiacritics(raw);
        return List.of(
                raw + ", vietnam",
                ascii + ", vietnam",
                raw + ", ha noi",
                ascii + ", ha noi",
                raw,
                ascii
        ).stream().map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private Optional<ReferencePoint> pickBestAnchorFromCandidates(JsonNode array, String query, ReferencePoint fallback) {
        if (array == null || !array.isArray() || array.isEmpty()) {
            return Optional.empty();
        }

        String normalizedQuery = stripDiacritics(query.toLowerCase(Locale.ROOT));
        int totalTokens = (int) java.util.Arrays.stream(normalizedQuery.split("\\s+"))
                .filter(token -> token != null && !token.isBlank())
                .count();
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestMatchedTokens = 0;
        ReferencePoint best = null;

        for (JsonNode node : array) {
            String clazz = node.path("class").asText(node.path("category").asText("")).toLowerCase(Locale.ROOT);
            String type = node.path("type").asText("").toLowerCase(Locale.ROOT);
            if (!isAnchorCandidate(clazz, type)) {
                continue;
            }

            double lat = parseDouble(node.path("lat").asText(null), Double.NaN);
            double lon = parseDouble(node.path("lon").asText(null), Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                continue;
            }

            String candidateText = stripDiacritics(
                    (node.path("display_name").asText("") + " " + node.path("name").asText(""))
                            .toLowerCase(Locale.ROOT)
            );

            double score = 0.0;
            if (candidateText.contains(normalizedQuery)) {
                score += 80;
            }

            int matchedTokens = 0;
            for (String token : normalizedQuery.split("\\s+")) {
                if (!token.isBlank() && candidateText.contains(token)) {
                    matchedTokens++;
                }
            }
            score += matchedTokens * 12.0;

            if (Set.of("natural", "waterway", "tourism", "historic", "leisure", "place").contains(clazz)) {
                score += 18;
            }
            if (Set.of("lake", "river", "reservoir", "attraction", "museum", "viewpoint", "park", "islet").contains(type)) {
                score += 20;
            }

            String display = node.path("display_name").asText("").toLowerCase(Locale.ROOT);
            if (display.contains("ha noi") || display.contains("hanoi")) {
                score += 6;
            }
            if (display.contains("viet nam") || display.contains("vietnam")) {
                score += 4;
            }

            score += node.path("importance").asDouble(0.0) * 20.0;

            double distance = haversineKm(fallback.latitude, fallback.longitude, lat, lon);
            if (distance > 1500) {
                score -= 8;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatchedTokens = matchedTokens;
                best = new ReferencePoint(lat, lon);
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        boolean strongTokenMatch = totalTokens == 0 || bestMatchedTokens >= Math.max(1, (int) Math.ceil(totalTokens * 0.7));
        boolean strongScore = bestScore >= 95.0;
        if (!strongTokenMatch || !strongScore) {
            return Optional.empty();
        }

        return Optional.of(best);
    }

    private boolean isAnchorCandidate(String clazz, String type) {
        if (clazz == null || clazz.isBlank()) {
            return false;
        }
        if (Set.of("place", "boundary", "natural", "waterway", "tourism", "historic", "landuse", "leisure").contains(clazz)) {
            return true;
        }
        return Set.of(
                "administrative", "city", "town", "village", "suburb", "quarter", "neighbourhood",
                "lake", "river", "reservoir", "attraction", "museum", "viewpoint", "park", "marketplace"
        ).contains(type);
    }

    private String stripDiacritics(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    private List<String> buildQuerySearchTerms(String query, String category) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String raw = query.trim();
        String ascii = stripDiacritics(raw).trim();
        String asciiLowered = ascii.toLowerCase(Locale.ROOT);

        List<String> terms = new ArrayList<>();
        terms.add(raw);

        if (!ascii.equalsIgnoreCase(raw)) {
            terms.add(ascii);
        }

        String[] tokens = asciiLowered.split("\\s+");
        if (tokens.length >= 2) {
            terms.add(String.join(" ", tokens));
            terms.add("\"" + raw + "\"");
            terms.add("\"" + ascii + "\"");
        }

        if (tokens.length > 1) {
            for (String token : tokens) {
                if (token.length() >= 3) {
                    terms.add(token);
                }
            }
        }

        if (!"ALL".equals(category)) {
            terms.add(raw + " " + categoryKeyword(category));
            terms.add(ascii + " " + categoryKeyword(category));
        }

        return terms.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(12)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> suggestions(
            AuthUser authUser,
            String query,
            Double latitude,
            Double longitude,
            Integer limit
    ) {
        String safeQuery = normalizeQuery(query);
        int safeLimit = limit == null ? 6 : Math.max(1, Math.min(limit, 12));
        ReferencePoint reference = resolveReference(authUser, latitude, longitude);
        ProfileContext profile = resolveProfile(authUser);

        int fetchLimit = Math.max(20, safeLimit * 4);
        Map<String, LivePlace> unique = new LinkedHashMap<>();

        if (safeQuery.isBlank()) {
            List<LivePlace> hotNearby = fetchLivePlaces(reference, "ALL", "", fetchLimit, 25.0, profile);
            for (LivePlace place : hotNearby) {
                unique.putIfAbsent(place.id, place);
                if (unique.size() >= fetchLimit) {
                    break;
                }
            }
        } else {
            List<LivePlace> bounded = searchNominatim(safeQuery, reference, 45.0, fetchLimit, true);
            for (LivePlace place : bounded) {
                unique.putIfAbsent(place.id, place);
            }
            if (unique.size() < safeLimit) {
                List<LivePlace> expanded = searchNominatim(safeQuery, reference, 350.0, fetchLimit, false);
                for (LivePlace place : expanded) {
                    unique.putIfAbsent(place.id, place);
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
            }
        }

        return unique.values().stream()
                .map(place -> rankPlace(place, reference, safeQuery, profile))
                .sorted(
                        Comparator.comparingInt((RankedPlace value) -> value.relevanceScore).reversed()
                                .thenComparingDouble(value -> value.distanceKm)
                )
                .map(value -> value.place.name)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .limit(safeLimit)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiscoveryDetailResponse getDetail(AuthUser authUser, String placeId, Double latitude, Double longitude) {
        ReferencePoint reference = resolveReference(authUser, latitude, longitude);

        LivePlace place = lookupByPlaceId(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Discovery place not found: " + placeId));

        boolean bookmarked = discoveryBookmarkRepository.existsByUserIdAndPlaceId(authUser.getId(), place.id);
        double distanceKm = round(haversineKm(reference.latitude, reference.longitude, place.latitude, place.longitude));

        return DiscoveryDetailResponse.builder()
                .item(toItemResponse(reference, place, distanceKm, bookmarked))
                .longDescription(place.longDescription)
                .imageUrls(place.imageUrls)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscoveryItemResponse> getBookmarks(AuthUser authUser, Double latitude, Double longitude) {
        ReferencePoint reference = resolveReference(authUser, latitude, longitude);
        List<DiscoveryBookmark> saved = discoveryBookmarkRepository.findByUserIdOrderByCreatedAtDesc(authUser.getId());

        if (saved.isEmpty()) {
            return List.of();
        }

        List<String> placeIds = saved.stream().map(DiscoveryBookmark::getPlaceId).toList();
        Map<String, LivePlace> placeMap = lookupByPlaceIds(placeIds)
                .stream()
                .collect(Collectors.toMap(place -> place.id, place -> place, (a, b) -> a));

        List<DiscoveryItemResponse> result = new ArrayList<>();
        for (String placeId : placeIds) {
            LivePlace place = placeMap.get(placeId);
            if (place == null) {
                continue;
            }
            double distanceKm = round(haversineKm(reference.latitude, reference.longitude, place.latitude, place.longitude));
            result.add(toItemResponse(reference, place, distanceKm, true));
        }
        return result;
    }

    @Override
    @Transactional
    public boolean setBookmark(AuthUser authUser, String placeId, boolean bookmarked) {
        if (lookupByPlaceId(placeId).isEmpty()) {
            throw new IllegalArgumentException("Discovery place not found: " + placeId);
        }

        if (bookmarked) {
            discoveryBookmarkRepository.findByUserIdAndPlaceId(authUser.getId(), placeId)
                    .orElseGet(() -> discoveryBookmarkRepository.save(
                            DiscoveryBookmark.builder()
                                    .user(authUser)
                                    .placeId(placeId)
                                    .build()
                    ));

            // Record activity for social feed
            try {
                String placeName = lookupByPlaceId(placeId).map(p -> p.name).orElse(placeId);
                socialService.recordActivity(authUser, "BOOKMARK", "PLACE", placeId, placeName, null);
            } catch (Exception ignored) {}

            return true;
        }

        discoveryBookmarkRepository.deleteByUserIdAndPlaceId(authUser.getId(), placeId);
        return false;
    }

    private List<RankedPlace> applyBrowseFilters(
            List<LivePlace> places,
            ReferencePoint reference,
            String query,
            String category,
            double minRating,
            int maxPriceLevel,
            int minPopularity,
            double maxDistanceKm,
            String sortBy,
            int limit,
            ProfileContext profile
    ) {
        return places.stream()
                .map(place -> rankPlace(place, reference, query, profile))
                .filter(item -> query.isBlank() || matchesQuery(item.place, query))
                .filter(item -> "ALL".equals(category) || category.equals(item.place.category))
                .filter(item -> item.place.rating >= minRating)
                .filter(item -> item.place.priceLevel <= maxPriceLevel)
                .filter(item -> item.place.popularityScore >= minPopularity)
                .filter(item -> item.distanceKm <= maxDistanceKm)
                .sorted(sortComparator(sortBy))
                .limit(limit)
                .toList();
    }

    private boolean matchesQuery(LivePlace place, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = stripDiacritics(query.toLowerCase(Locale.ROOT)).trim();
        if (normalizedQuery.isBlank()) {
            return true;
        }

        String haystack = stripDiacritics(
                (place.name + " " + place.shortDescription + " " + (place.address == null ? "" : place.address) + " " + String.join(" ", place.tags))
                        .toLowerCase(Locale.ROOT)
        );

        if (haystack.contains(normalizedQuery)) {
            return true;
        }

        int matchedTokens = 0;
        int totalTokens = 0;
        for (String token : normalizedQuery.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            totalTokens++;
            if (token.length() <= 2 || haystack.contains(token)) {
                matchedTokens++;
            }
        }

        if (totalTokens == 0) {
            return true;
        }
        return matchedTokens >= Math.max(1, (int) Math.ceil(totalTokens * 0.6));
    }

    private List<LivePlace> fetchLivePlaces(
            ReferencePoint reference,
            String category,
            String query,
            int fetchLimit,
            double maxDistanceKm,
            ProfileContext profile
    ) {
        double safeDistanceKm = Math.max(1.0, Math.min(maxDistanceKm, 200.0));
        Map<String, LivePlace> unique = new LinkedHashMap<>();

        if (query != null && !query.isBlank()) {
            int quickLimit = Math.max(8, Math.min(fetchLimit, 24));
            double quickRadius = Math.max(6.0, Math.min(safeDistanceKm * 1.5, 40.0));
            List<String> queryTerms = buildQuerySearchTerms(query, category);
            int perTermLimit = Math.max(6, quickLimit / Math.max(1, queryTerms.size()));

            for (String term : queryTerms) {
                List<LivePlace> direct = searchNominatim(term, reference, quickRadius, perTermLimit, true);
                for (LivePlace place : direct) {
                    unique.putIfAbsent(place.id, place);
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
                if (unique.size() >= fetchLimit) {
                    break;
                }
            }

            if (unique.size() < Math.min(12, fetchLimit / 2) && !"ALL".equals(category)) {
                List<LivePlace> categoryBoost = searchNominatim(
                        query + " " + categoryKeyword(category),
                        reference,
                        Math.max(quickRadius, 24.0),
                        quickLimit,
                        true
                );
                for (LivePlace place : categoryBoost) {
                    unique.putIfAbsent(place.id, place);
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
            }

            if (unique.size() < Math.max(6, fetchLimit / 4)) {
                for (String term : queryTerms) {
                    List<LivePlace> expandedDirect = searchNominatim(
                            term,
                            reference,
                            Math.max(120.0, safeDistanceKm * 4.0),
                            Math.max(10, Math.min(fetchLimit / Math.max(1, queryTerms.size()), 18)),
                            false
                    );
                    for (LivePlace place : expandedDirect) {
                        unique.putIfAbsent(place.id, place);
                        if (unique.size() >= fetchLimit) {
                            break;
                        }
                    }
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
            }

            if (unique.size() < Math.max(6, fetchLimit / 4)) {
                List<LivePlace> around = searchOverpass(reference, category, safeDistanceKm, fetchLimit);
                for (LivePlace place : around) {
                    unique.putIfAbsent(place.id, place);
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
            }

            return new ArrayList<>(unique.values());
        }

        List<LivePlace> around = searchOverpass(reference, category, safeDistanceKm, fetchLimit);
        for (LivePlace place : around) {
            unique.putIfAbsent(place.id, place);
            if (unique.size() >= fetchLimit) {
                break;
            }
        }

        List<String> searchTerms = buildSearchTerms(category, query, profile);
        if (searchTerms.isEmpty()) {
            return new ArrayList<>(unique.values());
        }

        int perTermLimit = Math.max(8, Math.min(20, (fetchLimit / searchTerms.size()) + 4));
        double searchRadiusKm = Math.max(8.0, Math.min(safeDistanceKm * 2.0, 120.0));
        double fallbackRadiusKm = Math.max(searchRadiusKm, Math.min(safeDistanceKm * 8.0, 450.0));

        for (String term : searchTerms) {
            List<LivePlace> found = searchNominatim(term, reference, searchRadiusKm, perTermLimit, true);
            for (LivePlace place : found) {
                unique.putIfAbsent(place.id, place);
                if (unique.size() >= fetchLimit) {
                    break;
                }
            }
            if (unique.size() >= fetchLimit) {
                break;
            }
        }

        if (unique.size() < Math.min(12, fetchLimit / 2)) {
            int unboundedLimit = Math.max(perTermLimit + 4, 12);
            for (String term : searchTerms) {
                List<LivePlace> found = searchNominatim(term, reference, fallbackRadiusKm, unboundedLimit, false);
                for (LivePlace place : found) {
                    unique.putIfAbsent(place.id, place);
                    if (unique.size() >= fetchLimit) {
                        break;
                    }
                }
                if (unique.size() >= fetchLimit) {
                    break;
                }
            }
        }

        return new ArrayList<>(unique.values());
    }

    private List<String> buildSearchTerms(String category, String query, ProfileContext profile) {
        String safeQuery = normalizeQuery(query);
        List<String> terms = new ArrayList<>();

        if (!safeQuery.isBlank()) {
            terms.add(safeQuery);
            if (!"ALL".equals(category)) {
                terms.add(safeQuery + " " + categoryKeyword(category));
            }
        } else {
            terms.addAll(baseTermsByCategory(category));
            terms.addAll(interestTerms(profile.interests));
            terms.addAll(timeAwareTerms());
        }

        return terms.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(14)
                .toList();
    }

    private String categoryKeyword(String category) {
        if (category == null) {
            return "";
        }
        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case "ATTRACTION" -> "attraction landmark sightseeing";
            case "CUISINE" -> "food restaurant cafe";
            case "ACTIVITY" -> "activity experience event";
            default -> "";
        };
    }

    private List<String> baseTermsByCategory(String category) {
        String normalized = category == null ? "ALL" : category.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ATTRACTION" -> List.of("tourist attraction", "museum", "landmark", "heritage site");
            case "CUISINE" -> List.of("restaurant", "street food", "local cuisine", "cafe");
            case "ACTIVITY" -> List.of("outdoor activity", "local experience", "event", "walking tour");
            default -> List.of("tourist attraction", "local food", "activity", "event");
        };
    }

    private List<String> interestTerms(Set<InterestType> interests) {
        if (interests == null || interests.isEmpty()) {
            return List.of();
        }

        List<String> terms = new ArrayList<>();
        for (InterestType interest : interests) {
            if (interest == null) {
                continue;
            }
            switch (interest) {
                case FOOD -> terms.addAll(List.of("food", "restaurant", "local cuisine"));
                case CULTURE -> terms.addAll(List.of("culture", "museum", "historic"));
                case SHOPPING -> terms.addAll(List.of("shopping", "market", "mall"));
                case NATURE -> terms.addAll(List.of("nature", "park", "beach"));
                case SPORTS -> terms.addAll(List.of("sports", "stadium", "fitness activity"));
                case ADVENTURE -> terms.addAll(List.of("adventure", "hiking", "trail"));
            }
        }
        return terms;
    }

    private List<String> timeAwareTerms() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 11) {
            return List.of("breakfast", "morning coffee", "sunrise spot");
        }
        if (hour >= 11 && hour < 17) {
            return List.of("lunch", "daytime attraction", "afternoon activity");
        }
        if (hour >= 17 && hour < 22) {
            return List.of("dinner", "sunset viewpoint", "evening event");
        }
        return List.of("night market", "late cafe", "night walk");
    }

    private List<LivePlace> searchNominatim(
            String term,
            ReferencePoint reference,
            double radiusKm,
            int limit,
            boolean bounded
    ) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        try {
            StringBuilder url = new StringBuilder(NOMINATIM_BASE_URL + "/search"
                    + "?format=jsonv2"
                    + "&addressdetails=1"
                    + "&extratags=1"
                    + "&namedetails=1"
                    + "&limit=" + limit
                    + "&accept-language=" + encode("vi,en")
                    + "&q=" + encode(term));

            if (bounded) {
                BoundingBox box = buildViewBox(reference.latitude, reference.longitude, radiusKm);
                url.append("&bounded=1");
                url.append("&viewbox=").append(encode(
                        String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", box.left, box.top, box.right, box.bottom)
                ));
            }

            JsonNode array = readJsonArray(url.toString());
            List<LivePlace> result = new ArrayList<>();
            for (JsonNode node : array) {
                LivePlace place = parseNominatimPlace(node);
                if (place == null) {
                    continue;
                }
                double distanceKm = haversineKm(reference.latitude, reference.longitude, place.latitude, place.longitude);
                if (bounded && distanceKm > radiusKm * 1.6) {
                    continue;
                }
                result.add(place);
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Optional<LivePlace> lookupByPlaceId(String placeId) {
        List<LivePlace> places = lookupByPlaceIds(List.of(placeId));
        if (places.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(places.get(0));
    }

    private List<LivePlace> lookupByPlaceIds(List<String> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return List.of();
        }

        List<String> osmTokens = placeIds.stream()
                .map(this::toOsmLookupToken)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        if (osmTokens.isEmpty()) {
            return List.of();
        }

        List<LivePlace> result = new ArrayList<>();
        for (int from = 0; from < osmTokens.size(); from += 20) {
            int to = Math.min(from + 20, osmTokens.size());
            String joined = String.join(",", osmTokens.subList(from, to));
            String url = NOMINATIM_BASE_URL + "/lookup"
                    + "?format=jsonv2"
                    + "&addressdetails=1"
                    + "&extratags=1"
                    + "&namedetails=1"
                    + "&osm_ids=" + encode(joined);
            try {
                JsonNode array = readJsonArray(url);
                for (JsonNode node : array) {
                    LivePlace place = parseNominatimPlace(node);
                    if (place != null) {
                        result.add(place);
                    }
                }
            } catch (Exception ignored) {
                // Best effort lookup; skip failing chunk.
            }
        }

        Map<String, LivePlace> dedup = new LinkedHashMap<>();
        for (LivePlace place : result) {
            dedup.putIfAbsent(place.id, place);
        }
        return new ArrayList<>(dedup.values());
    }

    private List<LivePlace> searchOverpass(ReferencePoint reference, String category, double radiusKm, int limit) {
        try {
            int safeLimit = Math.max(10, Math.min(limit, 120));
            int safeRadiusMeters = (int) Math.round(Math.max(1000.0, Math.min(radiusKm * 1000.0, 30000.0)));
            String overpassQuery = buildOverpassQuery(reference, category, safeRadiusMeters);

            HttpRequest request = HttpRequest.newBuilder(URI.create(OVERPASS_BASE_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(overpassQuery, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(18))
                    .header("Accept", "application/json")
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .header("User-Agent", "ExploreEase/1.0 (contact: explore-ease@example.com)")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                return List.of();
            }

            List<LivePlace> result = new ArrayList<>();
            for (JsonNode element : elements) {
                LivePlace place = parseOverpassPlace(element);
                if (place == null) {
                    continue;
                }
                double distanceKm = haversineKm(reference.latitude, reference.longitude, place.latitude, place.longitude);
                if (distanceKm <= radiusKm * 1.25) {
                    result.add(place);
                }
                if (result.size() >= safeLimit) {
                    break;
                }
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String buildOverpassQuery(ReferencePoint reference, String category, int radiusMeters) {
        String safeCategory = category == null ? "ALL" : category.trim().toUpperCase(Locale.ROOT);
        List<String> selectors = switch (safeCategory) {
            case "ATTRACTION" -> List.of(
                    "[tourism~\"attraction|museum|gallery|viewpoint|artwork|zoo|theme_park\"]",
                    "[historic]",
                    "[amenity~\"museum|arts_centre\"]"
            );
            case "CUISINE" -> List.of(
                    "[amenity~\"restaurant|cafe|fast_food|food_court|bar|pub|biergarten|ice_cream\"]",
                    "[cuisine]"
            );
            case "ACTIVITY" -> List.of(
                    "[leisure~\"park|sports_centre|fitness_centre|marina|stadium|playground|water_park\"]",
                    "[sport]",
                    "[tourism~\"camp_site|picnic_site|trail\"]"
            );
            default -> List.of(
                    "[tourism~\"attraction|museum|gallery|viewpoint|artwork|zoo|theme_park\"]",
                    "[historic]",
                    "[amenity~\"restaurant|cafe|fast_food|food_court|bar|pub|biergarten|ice_cream\"]",
                    "[leisure~\"park|sports_centre|fitness_centre|marina|stadium|playground|water_park\"]",
                    "[sport]",
                    "[shop~\"mall|marketplace|department_store\"]"
            );
        };

        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:20];(\n");
        for (String selector : selectors) {
            query.append("  node")
                    .append(selector)
                    .append("(around:")
                    .append(radiusMeters)
                    .append(",")
                    .append(String.format(Locale.US, "%.6f,%.6f", reference.latitude, reference.longitude))
                    .append(");\n");
            query.append("  way")
                    .append(selector)
                    .append("(around:")
                    .append(radiusMeters)
                    .append(",")
                    .append(String.format(Locale.US, "%.6f,%.6f", reference.latitude, reference.longitude))
                    .append(");\n");
            query.append("  relation")
                    .append(selector)
                    .append("(around:")
                    .append(radiusMeters)
                    .append(",")
                    .append(String.format(Locale.US, "%.6f,%.6f", reference.latitude, reference.longitude))
                    .append(");\n");
        }
        query.append(");out center tags;");
        return query.toString();
    }

    private LivePlace parseOverpassPlace(JsonNode element) {
        String osmType = element.path("type").asText("").trim().toLowerCase(Locale.ROOT);
        String osmId = element.path("id").asText("").trim();
        String id = buildPlaceId(osmType, osmId);
        if (id == null) {
            return null;
        }

        double latitude = element.path("lat").asDouble(Double.NaN);
        double longitude = element.path("lon").asDouble(Double.NaN);
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            JsonNode center = element.path("center");
            latitude = center.path("lat").asDouble(Double.NaN);
            longitude = center.path("lon").asDouble(Double.NaN);
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return null;
        }

        JsonNode tagsNode = element.path("tags");
        if (!tagsNode.isObject()) {
            return null;
        }
        Map<String, String> tagsMap = jsonObjectToMap(tagsNode);

        String name = pickFirstNonBlank(
            tagsMap.getOrDefault("name:vi", ""),
            tagsMap.getOrDefault("name:en", ""),
            tagsMap.getOrDefault("name", "")
        );
        if (name.isBlank()) {
            return null;
        }

        String osmCategory = pickFirstNonBlank(
            tagsMap.getOrDefault("tourism", ""),
            tagsMap.getOrDefault("amenity", ""),
            tagsMap.getOrDefault("leisure", ""),
            tagsMap.getOrDefault("historic", "")
        ).toLowerCase(Locale.ROOT);
        String osmTypeTag = pickFirstNonBlank(
            tagsMap.getOrDefault("amenity", ""),
            tagsMap.getOrDefault("tourism", ""),
            tagsMap.getOrDefault("leisure", ""),
            tagsMap.getOrDefault("shop", ""),
            tagsMap.getOrDefault("sport", "")
        ).toLowerCase(Locale.ROOT);

        Set<String> tags = extractTags(name, osmCategory, osmTypeTag, tagsMap);
        String category = classifyCategory(osmCategory, osmTypeTag, tags);

        String openingHours = trimToNull(tagsMap.get("opening_hours"));
        Boolean openNow = inferOpenNow(openingHours);

        int popularityScore = estimatePopularityFromTags(tagsMap, tags);
        double rating = estimateRating(popularityScore);
        int reviewCount = estimateReviewCountFromTags(popularityScore, tagsMap);
        int priceLevel = estimatePriceLevel(tags, tagsMap);

        String displayName = buildDisplayNameFromTags(tagsMap);
        String shortDescription = buildShortDescription(category, tags, displayName);
        String longDescription = buildLongDescription(name, category, displayName, openingHours, tags);
        List<String> imageUrls = extractImageUrls(tagsMap);

        return new LivePlace(
                id,
                name,
                category,
                tags,
                shortDescription,
                longDescription,
                displayName,
                latitude,
                longitude,
                rating,
                reviewCount,
                priceLevel,
                popularityScore,
                openingHours,
                openNow,
                imageUrls,
                isFamilyFriendly(tags),
                isGroupFriendly(tags),
                isSoloFriendly(tags)
        );
    }

    private Map<String, String> jsonObjectToMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return result;
        }
        node.properties().forEach(entry -> {
            String value = entry.getValue().asText("").trim();
            if (!value.isEmpty()) {
                result.put(entry.getKey(), value);
            }
        });
        return result;
    }

    private JsonNode readJsonArray(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "ExploreEase/1.0 (contact: explore-ease@example.com)")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Provider request failed: " + response.statusCode());
        }

        JsonNode payload = objectMapper.readTree(response.body());
        if (payload == null || !payload.isArray()) {
            return objectMapper.createArrayNode();
        }
        return payload;
    }

    private LivePlace parseNominatimPlace(JsonNode node) {
        String id = buildPlaceId(node.path("osm_type").asText(""), node.path("osm_id").asText(""));
        if (id == null) {
            return null;
        }

        double latitude = parseDouble(node.path("lat").asText(null), Double.NaN);
        double longitude = parseDouble(node.path("lon").asText(null), Double.NaN);
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return null;
        }

        String rawName = node.path("name").asText("").trim();
        JsonNode namedetails = node.path("namedetails");
        if (rawName.isEmpty() && namedetails.isObject()) {
            rawName = pickFirstNonBlank(
                    namedetails.path("name:en").asText(""),
                    namedetails.path("name").asText(""),
                    namedetails.path("name:vi").asText("")
            );
        }

        String displayName = node.path("display_name").asText("").trim();
        String name = rawName.isEmpty() ? firstDisplayToken(displayName) : rawName;
        if (name.isEmpty()) {
            return null;
        }

        JsonNode extratags = node.path("extratags");
        String osmCategory = node.path("category").asText("").toLowerCase(Locale.ROOT);
        String osmType = node.path("type").asText("").toLowerCase(Locale.ROOT);

        Set<String> tags = extractTags(name, osmCategory, osmType, extratags);
        String category = classifyCategory(osmCategory, osmType, tags);

        String openingHours = trimToNull(extratags.path("opening_hours").asText(null));
        Boolean openNow = inferOpenNow(openingHours);

        int popularityScore = estimatePopularity(node, extratags, tags);
        double rating = estimateRating(popularityScore);
        int reviewCount = estimateReviewCount(popularityScore, extratags);
        int priceLevel = estimatePriceLevel(tags, extratags);

        String shortDescription = buildShortDescription(category, tags, displayName);
        String longDescription = buildLongDescription(name, category, displayName, openingHours, tags);
        List<String> imageUrls = extractImageUrls(extratags);

        return new LivePlace(
                id,
                name,
                category,
                tags,
                shortDescription,
                longDescription,
                displayName,
                latitude,
                longitude,
                rating,
                reviewCount,
                priceLevel,
                popularityScore,
                openingHours,
                openNow,
                imageUrls,
                isFamilyFriendly(tags),
                isGroupFriendly(tags),
                isSoloFriendly(tags)
        );
    }

    private RankedPlace rankPlace(LivePlace place, ReferencePoint reference, String query, ProfileContext profile) {
        double distanceKm = round(haversineKm(reference.latitude, reference.longitude, place.latitude, place.longitude));

        int relevance = 40;
        relevance += Math.max(0, 24 - (int) Math.round(distanceKm * 2.8));
        relevance += Math.max(0, (place.popularityScore - 40) / 3);
        relevance += (int) Math.round(place.rating * 2.5);

        if (!query.isBlank()) {
            String q = query.toLowerCase(Locale.ROOT);
            if (place.name.toLowerCase(Locale.ROOT).contains(q)) {
                relevance += 20;
            }
            if (place.shortDescription.toLowerCase(Locale.ROOT).contains(q)) {
                relevance += 8;
            }
            if (place.tags.stream().anyMatch(tag -> tag.contains(q))) {
                relevance += 8;
            }
        }

        for (InterestType interest : profile.interests) {
            String token = interest.name().toLowerCase(Locale.ROOT);
            if (place.tags.contains(token)) {
                relevance += 12;
            }
        }

        if ("FAMILY".equals(profile.travelStyle) && place.familyFriendly) {
            relevance += 8;
        } else if ("GROUP".equals(profile.travelStyle) && place.groupFriendly) {
            relevance += 8;
        } else if ("SOLO".equals(profile.travelStyle) && place.soloFriendly) {
            relevance += 6;
        }

        if (place.openNow != null && place.openNow) {
            relevance += 5;
        }

        relevance = Math.max(1, Math.min(relevance, 99));
        return new RankedPlace(place, distanceKm, relevance);
    }

    private Comparator<RankedPlace> sortComparator(String sortBy) {
        if ("TOP_RATED".equals(sortBy)) {
            return Comparator
                    .comparingDouble((RankedPlace value) -> value.place.rating).reversed()
                    .thenComparingInt(value -> value.place.reviewCount).reversed()
                    .thenComparingDouble(value -> value.distanceKm);
        }
        if ("AZ".equals(sortBy)) {
            return Comparator.comparing(value -> value.place.name);
        }
        return Comparator
                .comparingInt((RankedPlace value) -> value.relevanceScore).reversed()
                .thenComparingDouble(value -> value.distanceKm);
    }

    private DiscoveryItemResponse toItemResponse(ReferencePoint directionOrigin, LivePlace place, double distanceKm, boolean bookmarked) {
        String availabilityLabel = buildAvailabilityLabel(place.openNow, place.popularityScore);
        String summary = place.shortDescription;
        if (place.address != null && !place.address.isBlank()) {
            summary = summary + " • " + firstDisplayToken(place.address);
        }

        return DiscoveryItemResponse.builder()
                .id(place.id)
                .name(place.name)
                .category(place.category)
                .tags(new ArrayList<>(place.tags))
                .shortDescription(summary)
                .latitude(place.latitude)
                .longitude(place.longitude)
                .distanceKm(distanceKm)
                .rating(place.rating)
                .reviewCount(place.reviewCount)
                .priceLevel(place.priceLevel)
                .popularityScore(place.popularityScore)
                .thumbnailUrl(place.imageUrls.isEmpty() ? null : place.imageUrls.get(0))
                .pricingText("$".repeat(place.priceLevel))
                .operationalHours(place.openingHours)
                .openNow(place.openNow)
                .availabilityLabel(availabilityLabel)
                .directionsUrl(buildDirectionsUrl(directionOrigin.latitude, directionOrigin.longitude, place.latitude, place.longitude))
                .bookmarked(bookmarked)
                .build();
    }

    private String buildAvailabilityLabel(Boolean openNow, int popularityScore) {
        if (openNow == null) {
            if (popularityScore >= 85) {
                return "Hot now";
            }
            if (popularityScore >= 70) {
                return "Popular";
            }
            return "Check details";
        }
        if (!openNow) {
            return "Closed now";
        }
        if (popularityScore >= 90) {
            return "Busy now";
        }
        return "Open now";
    }

    private String buildDirectionsUrl(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        return String.format(
                Locale.US,
                "https://www.google.com/maps/dir/?api=1&origin=%.6f,%.6f&destination=%.6f,%.6f&travelmode=walking",
                fromLatitude,
                fromLongitude,
                toLatitude,
                toLongitude
        );
    }

    private Set<String> loadBookmarkedIds(Long userId) {
        return new HashSet<>(
                discoveryBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(DiscoveryBookmark::getPlaceId)
                        .toList()
        );
    }

    private ProfileContext resolveProfile(AuthUser authUser) {
        UserProfile profile = userProfileRepository.findByAuthUser(authUser).orElse(null);
        if (profile == null) {
            return new ProfileContext(Set.of(), "SOLO");
        }

        Set<InterestType> interests = profile.getInterests() == null ? Set.of() : profile.getInterests();
        String travelStyle = profile.getTravelStyle() == null ? "SOLO" : profile.getTravelStyle().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SOLO", "FAMILY", "GROUP").contains(travelStyle)) {
            travelStyle = "SOLO";
        }

        return new ProfileContext(interests, travelStyle);
    }

    private ReferencePoint resolveReference(AuthUser authUser, Double latitude, Double longitude) {
        if (latitude != null || longitude != null) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("latitude and longitude must be provided together");
            }
            validateCoordinates(latitude, longitude);
            return new ReferencePoint(latitude, longitude);
        }

        Optional<UserLocation> saved = userLocationRepository.findByUserId(authUser.getId());
        if (saved.isPresent()) {
            return new ReferencePoint(saved.get().getLatitude(), saved.get().getLongitude());
        }

        return new ReferencePoint(DEFAULT_LAT, DEFAULT_LON);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "ALL";
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (Set.of("ALL", "ATTRACTION", "CUISINE", "ACTIVITY").contains(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Category must be one of: ALL, ATTRACTION, CUISINE, ACTIVITY");
    }

    private String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "RELEVANCE";
        }
        String normalized = sortBy.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RELEVANCE", "TOP_RATED", "AZ" -> normalized;
            default -> throw new IllegalArgumentException("Sort must be one of: RELEVANCE, TOP_RATED, AZ");
        };
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private BoundingBox buildViewBox(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.max(0.2, Math.cos(Math.toRadians(latitude))));

        double left = longitude - lonDelta;
        double right = longitude + lonDelta;
        double top = latitude + latDelta;
        double bottom = latitude - latDelta;

        return new BoundingBox(left, top, right, bottom);
    }

    private String buildPlaceId(String osmType, String osmId) {
        if (osmType == null || osmId == null || osmType.isBlank() || osmId.isBlank()) {
            return null;
        }
        String normalizedType = osmType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("node", "way", "relation").contains(normalizedType)) {
            return null;
        }
        return "osm:" + normalizedType + ":" + osmId.trim();
    }

    private Optional<String> toOsmLookupToken(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }

        String[] parts = placeId.split(":");
        if (parts.length != 3 || !"osm".equals(parts[0])) {
            return Optional.empty();
        }

        String prefix = switch (parts[1]) {
            case "node" -> "N";
            case "way" -> "W";
            case "relation" -> "R";
            default -> "";
        };

        if (prefix.isEmpty() || parts[2].isBlank()) {
            return Optional.empty();
        }
        return Optional.of(prefix + parts[2]);
    }

    private Set<String> extractTags(String name, String osmCategory, String osmType, JsonNode extratags) {
        Set<String> tags = new HashSet<>();

        addTag(tags, osmCategory);
        addTag(tags, osmType);

        if (name != null) {
            String lowered = name.toLowerCase(Locale.ROOT);
            for (String token : lowered.split("[^a-z0-9]+")) {
                addTag(tags, token);
            }
            if (lowered.contains("beach")) addTag(tags, "nature");
            if (lowered.contains("museum")) addTag(tags, "culture");
            if (lowered.contains("market")) addTag(tags, "shopping");
            if (lowered.contains("hike") || lowered.contains("trail")) addTag(tags, "adventure");
            if (lowered.contains("restaurant") || lowered.contains("cafe")) addTag(tags, "food");
        }

        if (extratags != null && extratags.isObject()) {
            addTag(tags, extratags.path("tourism").asText(null));
            addTag(tags, extratags.path("leisure").asText(null));
            addTag(tags, extratags.path("sport").asText(null));

            String cuisine = extratags.path("cuisine").asText(null);
            if (cuisine != null) {
                for (String part : cuisine.split("[;,]") ) {
                    addTag(tags, part);
                }
                addTag(tags, "food");
            }

            String openingHours = extratags.path("opening_hours").asText(null);
            if (openingHours != null && !openingHours.isBlank()) {
                addTag(tags, "opening_hours");
            }
        }

        return tags;
    }

    private Set<String> extractTags(String name, String osmCategory, String osmType, Map<String, String> tagsMap) {
        Set<String> tags = new HashSet<>();
        addTag(tags, osmCategory);
        addTag(tags, osmType);

        if (name != null) {
            String lowered = name.toLowerCase(Locale.ROOT);
            for (String token : lowered.split("[^a-z0-9]+")) {
                addTag(tags, token);
            }
        }

        if (tagsMap != null) {
            addTag(tags, tagsMap.get("tourism"));
            addTag(tags, tagsMap.get("amenity"));
            addTag(tags, tagsMap.get("leisure"));
            addTag(tags, tagsMap.get("shop"));
            addTag(tags, tagsMap.get("sport"));
            String cuisine = tagsMap.get("cuisine");
            if (cuisine != null) {
                for (String part : cuisine.split("[;,]")) {
                    addTag(tags, part);
                }
                addTag(tags, "food");
            }
            if (hasValue(tagsMap, "opening_hours")) {
                addTag(tags, "opening_hours");
            }
        }

        return tags;
    }

    private void addTag(Set<String> tags, String rawTag) {
        if (rawTag == null) {
            return;
        }
        String normalized = rawTag.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return;
        }
        tags.add(normalized);
    }

    private String classifyCategory(String osmCategory, String osmType, Set<String> tags) {
        if (Set.of("restaurant", "cafe", "fast_food", "food_court", "bar", "pub").contains(osmType)
                || tags.contains("food")
                || tags.contains("cuisine")) {
            return "CUISINE";
        }

        if (Set.of("tourism", "historic").contains(osmCategory)
                || Set.of("attraction", "museum", "gallery", "viewpoint", "beach", "monument", "temple", "castle", "ruins").contains(osmType)
                || tags.contains("culture")
                || tags.contains("heritage")) {
            return "ATTRACTION";
        }

        return "ACTIVITY";
    }

    private int estimatePopularity(JsonNode node, JsonNode extratags, Set<String> tags) {
        double importance = node.path("importance").asDouble(0.0);
        int placeRank = node.path("place_rank").asInt(0);

        int score = 38;
        score += (int) Math.round(Math.min(28.0, importance * 120.0));
        score += Math.max(0, placeRank - 18);

        if (hasValue(extratags, "website")) {
            score += 9;
        }
        if (hasValue(extratags, "wikipedia") || hasValue(extratags, "wikidata")) {
            score += 13;
        }
        if (hasValue(extratags, "opening_hours")) {
            score += 6;
        }
        if (hasValue(extratags, "phone")) {
            score += 4;
        }
        if (hasValue(extratags, "image") || hasValue(extratags, "wikimedia_commons")) {
            score += 8;
        }

        if (tags.contains("beach") || tags.contains("museum") || tags.contains("market")) {
            score += 5;
        }

        return Math.max(1, Math.min(score, 99));
    }

    private double estimateRating(int popularityScore) {
        double rating = 3.2 + (popularityScore / 100.0) * 1.7;
        rating = Math.max(3.0, Math.min(rating, 5.0));
        return roundOne(rating);
    }

    private int estimateReviewCount(int popularityScore, JsonNode extratags) {
        int base = popularityScore * 9;
        if (hasValue(extratags, "wikipedia") || hasValue(extratags, "wikidata")) {
            base += 180;
        }
        if (hasValue(extratags, "website")) {
            base += 90;
        }
        return Math.max(15, Math.min(base, 5000));
    }

    private int estimatePriceLevel(Set<String> tags, JsonNode extratags) {
        String cuisine = extratags.path("cuisine").asText("").toLowerCase(Locale.ROOT);
        if (cuisine.contains("fine_dining") || cuisine.contains("steak")) {
            return 4;
        }
        if (cuisine.contains("seafood") || cuisine.contains("japanese") || cuisine.contains("korean")) {
            return 3;
        }
        if (tags.contains("fast_food") || cuisine.contains("street_food")) {
            return 1;
        }
        return 2;
    }

    private int estimatePriceLevel(Set<String> tags, Map<String, String> tagsMap) {
        String cuisine = Optional.ofNullable(tagsMap.get("cuisine")).orElse("").toLowerCase(Locale.ROOT);
        String fee = Optional.ofNullable(tagsMap.get("fee")).orElse("").toLowerCase(Locale.ROOT);
        if (cuisine.contains("fine_dining") || cuisine.contains("steak")) {
            return 4;
        }
        if (cuisine.contains("seafood") || cuisine.contains("japanese") || cuisine.contains("korean") || "yes".equals(fee)) {
            return 3;
        }
        if (tags.contains("fast_food") || cuisine.contains("street_food")) {
            return 1;
        }
        return 2;
    }

    private List<String> extractImageUrls(JsonNode extratags) {
        List<String> imageUrls = new ArrayList<>();

        String directImage = trimToNull(extratags.path("image").asText(null));
        if (directImage != null && (directImage.startsWith("http://") || directImage.startsWith("https://"))) {
            imageUrls.add(directImage);
        }

        String commonsFile = trimToNull(extratags.path("wikimedia_commons").asText(null));
        if (commonsFile != null) {
            imageUrls.add(toCommonsFilePath(commonsFile));
        }

        String wikidata = trimToNull(extratags.path("wikidata").asText(null));
        if (wikidata != null) {
            String cached = wikidataImageCache.get(wikidata);
            if (cached == null) {
                cached = fetchWikidataImage(wikidata);
                if (cached != null) {
                    wikidataImageCache.put(wikidata, cached);
                }
            }
            if (cached != null) {
                imageUrls.add(cached);
            }
        }

        return imageUrls.stream().distinct().limit(4).toList();
    }

    private List<String> extractImageUrls(Map<String, String> tagsMap) {
        List<String> imageUrls = new ArrayList<>();

        String directImage = trimToNull(tagsMap.get("image"));
        if (directImage != null && (directImage.startsWith("http://") || directImage.startsWith("https://"))) {
            imageUrls.add(directImage);
        }

        String commonsFile = trimToNull(tagsMap.get("wikimedia_commons"));
        if (commonsFile != null) {
            imageUrls.add(toCommonsFilePath(commonsFile));
        }

        String wikidata = trimToNull(tagsMap.get("wikidata"));
        if (wikidata != null) {
            String cached = wikidataImageCache.get(wikidata);
            if (cached == null) {
                cached = fetchWikidataImage(wikidata);
                if (cached != null) {
                    wikidataImageCache.put(wikidata, cached);
                }
            }
            if (cached != null) {
                imageUrls.add(cached);
            }
        }

        return imageUrls.stream().distinct().limit(4).toList();
    }

    private int estimatePopularityFromTags(Map<String, String> tagsMap, Set<String> tags) {
        int score = 45;
        if (hasValue(tagsMap, "website")) score += 9;
        if (hasValue(tagsMap, "wikipedia") || hasValue(tagsMap, "wikidata")) score += 13;
        if (hasValue(tagsMap, "opening_hours")) score += 7;
        if (hasValue(tagsMap, "phone") || hasValue(tagsMap, "contact:phone")) score += 5;
        if (hasValue(tagsMap, "image") || hasValue(tagsMap, "wikimedia_commons")) score += 8;
        if (hasValue(tagsMap, "brand")) score += 4;
        if (tags.contains("museum") || tags.contains("market") || tags.contains("beach") || tags.contains("park")) {
            score += 5;
        }
        return Math.max(1, Math.min(score, 99));
    }

    private int estimateReviewCountFromTags(int popularityScore, Map<String, String> tagsMap) {
        int base = popularityScore * 8;
        if (hasValue(tagsMap, "wikipedia") || hasValue(tagsMap, "wikidata")) {
            base += 150;
        }
        if (hasValue(tagsMap, "website")) {
            base += 70;
        }
        return Math.max(20, Math.min(base, 4500));
    }

    private String buildDisplayNameFromTags(Map<String, String> tagsMap) {
        String street = trimToNull(tagsMap.get("addr:street"));
        String house = trimToNull(tagsMap.get("addr:housenumber"));
        String city = trimToNull(tagsMap.get("addr:city"));
        String district = trimToNull(tagsMap.get("addr:district"));

        List<String> parts = new ArrayList<>();
        if (street != null && house != null) {
            parts.add(house + " " + street);
        } else if (street != null) {
            parts.add(street);
        }
        if (district != null) {
            parts.add(district);
        }
        if (city != null) {
            parts.add(city);
        }
        return String.join(", ", parts);
    }

    private String fetchWikidataImage(String wikidataId) {
        try {
            String url = WIKIDATA_BASE_URL + "/" + encode(wikidataId) + ".json";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ExploreEase/1.0 (contact: explore-ease@example.com)")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode entity = root.path("entities").path(wikidataId).path("claims").path("P18");
            if (!entity.isArray() || entity.isEmpty()) {
                return null;
            }

            String fileName = entity.get(0)
                    .path("mainsnak")
                    .path("datavalue")
                    .path("value")
                    .asText("")
                    .trim();
            if (fileName.isEmpty()) {
                return null;
            }

            return toCommonsFilePath(fileName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toCommonsFilePath(String value) {
        String normalized = value.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("file:")) {
            normalized = normalized.substring(5);
        }
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encode(normalized);
    }

    private String buildShortDescription(String category, Set<String> tags, String displayName) {
        StringBuilder builder = new StringBuilder();
        if ("ATTRACTION".equals(category)) {
            builder.append("Local attraction");
        } else if ("CUISINE".equals(category)) {
            builder.append("Food & drink spot");
        } else {
            builder.append("Local activity point");
        }

        List<String> topTags = tags.stream()
                .filter(tag -> tag.length() > 2)
                .filter(tag -> !Set.of("amenity", "tourism", "historic", "opening_hours").contains(tag))
                .limit(3)
                .toList();

        if (!topTags.isEmpty()) {
            builder.append(" - ").append(String.join(", ", topTags));
        }
        if (displayName != null && !displayName.isBlank()) {
            builder.append(".");
        }

        return builder.toString();
    }

    private String buildLongDescription(String name, String category, String displayName, String openingHours, Set<String> tags) {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" is a ").append(category.toLowerCase(Locale.ROOT)).append(" discovery from live map data.");

        if (displayName != null && !displayName.isBlank()) {
            builder.append(" Address: ").append(displayName).append(".");
        }
        if (openingHours != null) {
            builder.append(" Opening hours: ").append(openingHours).append(".");
        }

        List<String> topTags = tags.stream().limit(5).toList();
        if (!topTags.isEmpty()) {
            builder.append(" Tags: ").append(String.join(", ", topTags)).append(".");
        }

        return builder.toString();
    }

    private Boolean inferOpenNow(String openingHours) {
        if (openingHours == null || openingHours.isBlank()) {
            return null;
        }

        Matcher matcher = TIME_RANGE_PATTERN.matcher(openingHours);
        if (!matcher.find()) {
            return null;
        }

        int fromHour = Integer.parseInt(matcher.group(1));
        int fromMin = Integer.parseInt(matcher.group(2));
        int toHour = Integer.parseInt(matcher.group(3));
        int toMin = Integer.parseInt(matcher.group(4));

        LocalTime now = LocalTime.now();
        LocalTime from = LocalTime.of(Math.min(fromHour, 23), Math.min(fromMin, 59));
        LocalTime to = LocalTime.of(Math.min(toHour, 23), Math.min(toMin, 59));

        if (from.equals(to)) {
            return true;
        }
        if (from.isBefore(to)) {
            return !now.isBefore(from) && now.isBefore(to);
        }
        return !now.isBefore(from) || now.isBefore(to);
    }

    private boolean hasValue(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return false;
        }
        String value = node.path(field).asText("").trim();
        return !value.isEmpty();
    }

    private boolean hasValue(Map<String, String> tagsMap, String field) {
        if (tagsMap == null) {
            return false;
        }
        String value = tagsMap.get(field);
        return value != null && !value.trim().isEmpty();
    }

    private boolean isFamilyFriendly(Set<String> tags) {
        return tags.contains("park") || tags.contains("market") || tags.contains("museum") || tags.contains("beach");
    }

    private boolean isGroupFriendly(Set<String> tags) {
        return tags.contains("market") || tags.contains("sports") || tags.contains("food") || tags.contains("tour");
    }

    private boolean isSoloFriendly(Set<String> tags) {
        return tags.contains("cafe") || tags.contains("museum") || tags.contains("beach") || tags.contains("hiking");
    }

    private String firstDisplayToken(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.split(",");
        if (parts.length == 0) {
            return displayName;
        }
        return parts[0].trim();
    }

    private String pickFirstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return fallback;
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

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double roundOne(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class RankedPlace {
        private final LivePlace place;
        private final double distanceKm;
        private final int relevanceScore;

        private RankedPlace(LivePlace place, double distanceKm, int relevanceScore) {
            this.place = place;
            this.distanceKm = distanceKm;
            this.relevanceScore = relevanceScore;
        }
    }

    private static final class ReferencePoint {
        private final double latitude;
        private final double longitude;

        private ReferencePoint(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final class BoundingBox {
        private final double left;
        private final double top;
        private final double right;
        private final double bottom;

        private BoundingBox(double left, double top, double right, double bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private static final class ProfileContext {
        private final Set<InterestType> interests;
        private final String travelStyle;

        private ProfileContext(Set<InterestType> interests, String travelStyle) {
            this.interests = interests;
            this.travelStyle = travelStyle;
        }
    }

    private static final class LivePlace {
        private final String id;
        private final String name;
        private final String category;
        private final Set<String> tags;
        private final String shortDescription;
        private final String longDescription;
        private final String address;
        private final double latitude;
        private final double longitude;
        private final double rating;
        private final int reviewCount;
        private final int priceLevel;
        private final int popularityScore;
        private final String openingHours;
        private final Boolean openNow;
        private final List<String> imageUrls;
        private final boolean familyFriendly;
        private final boolean groupFriendly;
        private final boolean soloFriendly;

        private LivePlace(
                String id,
                String name,
                String category,
                Set<String> tags,
                String shortDescription,
                String longDescription,
                String address,
                double latitude,
                double longitude,
                double rating,
                int reviewCount,
                int priceLevel,
                int popularityScore,
                String openingHours,
                Boolean openNow,
                List<String> imageUrls,
                boolean familyFriendly,
                boolean groupFriendly,
                boolean soloFriendly
        ) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.tags = tags;
            this.shortDescription = shortDescription;
            this.longDescription = longDescription;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.rating = rating;
            this.reviewCount = reviewCount;
            this.priceLevel = priceLevel;
            this.popularityScore = popularityScore;
            this.openingHours = openingHours;
            this.openNow = openNow;
            this.imageUrls = imageUrls;
            this.familyFriendly = familyFriendly;
            this.groupFriendly = groupFriendly;
            this.soloFriendly = soloFriendly;
        }
    }

}

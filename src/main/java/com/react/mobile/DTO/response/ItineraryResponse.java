package com.react.mobile.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItineraryResponse {
    private double totalHours;
    private String startTime;
    private String endTime;
    private String mood;
    private List<ItinerarySlot> slots;

    @Data
    @Builder
    public static class ItinerarySlot {
        private int order;
        private String startTime;
        private String endTime;
        private int durationMinutes;
        private String placeId;
        private String placeName;
        private String category;
        private double rating;
        private int priceLevel;
        private double distanceKm;
        private double latitude;
        private double longitude;
        private String directionsUrl;
        private String note;
    }
}

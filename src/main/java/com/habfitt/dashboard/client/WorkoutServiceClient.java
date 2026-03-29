package com.habfitt.dashboard.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class WorkoutServiceClient {

    private final RestTemplate restTemplate;
    private final String workoutBaseUrl;

    public WorkoutServiceClient(@Value("${workout.service-url}") String workoutBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.workoutBaseUrl = workoutBaseUrl;
    }

    /** Fetch the active workout plan for the current user. Returns empty if none. */
    public Optional<ActivePlanDto> getActivePlan(String bearerToken) {
        try {
            ResponseEntity<ActivePlanDto> response = restTemplate.exchange(
                workoutBaseUrl + "/plans/active",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(bearerToken)),
                ActivePlanDto.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to fetch active plan: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Fetch a specific week of the plan. Returns empty on any error. */
    public Optional<WeekDto> getWeek(UUID planId, int weekNumber, String bearerToken) {
        try {
            ResponseEntity<WeekDto> response = restTemplate.exchange(
                workoutBaseUrl + "/plans/" + planId + "/weeks/" + weekNumber,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(bearerToken)),
                WeekDto.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to fetch week {}: {}", weekNumber, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpHeaders authHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActivePlanDto(
        UUID id,
        String programmeName,
        String templateId,
        String format,
        int totalWeeks,
        int currentWeek,
        String status
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeekDto(
        UUID id,
        int weekNumber,
        boolean isDeload,
        String status,
        List<SessionDto> sessions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionDto(
        UUID id,
        int dayOfWeek,
        String sessionName,
        int estimatedDurationMinutes,
        int exerciseCount,
        String status
    ) {}
}

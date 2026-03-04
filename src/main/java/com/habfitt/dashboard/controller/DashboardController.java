package com.habfitt.dashboard.controller;

import com.habfitt.dashboard.model.DashboardResponse;
import com.habfitt.dashboard.model.DashboardResponse.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DashboardController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] DAY_SHORTS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
        @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone,
        Authentication authentication
    ) {
        ZoneId zoneId = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String greeting = buildGreeting(now);

        DashboardResponse response = new DashboardResponse(
            greeting,
            "NO_PLAN",
            new StreakInfo(0),
            null,
            buildWeeklyAdherence(now),
            new Stats(0, 0, 0),
            "Start your first workout plan and build your daily momentum!"
        );

        return ResponseEntity.ok(response);
    }

    private String buildGreeting(ZonedDateTime now) {
        int hour = now.getHour();
        if (hour < 12) return "Good morning!";
        if (hour < 17) return "Good afternoon!";
        return "Good evening!";
    }

    private ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    private List<AdherenceDay> buildWeeklyAdherence(ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        // Find Monday of current week
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        List<AdherenceDay> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            String status = day.equals(today) ? "TODAY" : "SCHEDULED";
            String dayShort = DAY_SHORTS[day.getDayOfWeek().getValue() - 1];
            days.add(new AdherenceDay(dayShort, status, day.format(DATE_FMT)));
        }
        return days;
    }
}

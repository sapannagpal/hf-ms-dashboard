package com.habfitt.dashboard.controller;

import com.habfitt.dashboard.client.WorkoutServiceClient;
import com.habfitt.dashboard.client.WorkoutServiceClient.*;
import com.habfitt.dashboard.model.DashboardResponse;
import com.habfitt.dashboard.model.DashboardResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] DAY_SHORTS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final WorkoutServiceClient workoutClient;

    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
        @RequestHeader(value = "X-Timezone", defaultValue = "UTC") String timezone,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        Authentication authentication
    ) {
        ZoneId zoneId = parseZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String greeting = buildGreeting(now);
        List<AdherenceDay> weeklyAdherence = buildWeeklyAdherence(now);

        // No auth header → NO_PLAN fallback
        if (authHeader == null) {
            return ResponseEntity.ok(noPlanResponse(greeting, weeklyAdherence));
        }

        // Fetch active plan from Workout MS
        Optional<ActivePlanDto> planOpt = workoutClient.getActivePlan(authHeader);
        if (planOpt.isEmpty()) {
            return ResponseEntity.ok(noPlanResponse(greeting, weeklyAdherence));
        }

        ActivePlanDto plan = planOpt.get();

        // Fetch current week sessions
        Optional<WeekDto> weekOpt = workoutClient.getWeek(plan.id(), plan.currentWeek(), authHeader);
        if (weekOpt.isEmpty()) {
            return ResponseEntity.ok(noPlanResponse(greeting, weeklyAdherence));
        }

        WeekDto week = weekOpt.get();
        int todayDow = now.getDayOfWeek().getValue(); // 1=Mon..7=Sun

        // Find today's session
        Optional<SessionDto> todaySession = week.sessions().stream()
            .filter(s -> s.dayOfWeek() == todayDow)
            .findFirst();

        // Count completed sessions this week
        long completedThisWeek = week.sessions().stream()
            .filter(s -> "COMPLETED".equals(s.status()))
            .count();

        // Determine state
        String state;
        TodayInfo todayInfo = null;
        String coachNote;

        if (todaySession.isPresent()) {
            SessionDto session = todaySession.get();
            boolean isCompleted = "COMPLETED".equals(session.status());
            state = isCompleted ? "COMPLETED" : "WORKOUT_TODAY";
            todayInfo = new TodayInfo(
                new WorkoutInfo(session.sessionName(), session.estimatedDurationMinutes(),
                    session.exerciseCount(), isCompleted ? session.exerciseCount() : 0),
                isCompleted
            );
            coachNote = isCompleted
                ? "Great work today! Rest up and come back stronger."
                : "You've got " + session.sessionName() + " today. Let's get it done!";
        } else {
            state = "REST_DAY";
            coachNote = "Rest day — recovery is part of the plan. Stay hydrated!";
        }

        // Weeks remaining = total - current
        int daysRemaining = (plan.totalWeeks() - plan.currentWeek()) * 7;

        DashboardResponse response = new DashboardResponse(
            greeting,
            state,
            new StreakInfo((int) completedThisWeek),
            todayInfo,
            weeklyAdherence,
            new Stats((int) completedThisWeek, (int) completedThisWeek, daysRemaining),
            coachNote
        );

        return ResponseEntity.ok(response);
    }

    private DashboardResponse noPlanResponse(String greeting, List<AdherenceDay> weeklyAdherence) {
        return new DashboardResponse(
            greeting, "NO_PLAN", new StreakInfo(0), null, weeklyAdherence,
            new Stats(0, 0, 0),
            "Start your first workout plan and build your daily momentum!"
        );
    }

    private String buildGreeting(ZonedDateTime now) {
        int hour = now.getHour();
        if (hour < 12) return "Good morning!";
        if (hour < 17) return "Good afternoon!";
        return "Good evening!";
    }

    private ZoneId parseZone(String timezone) {
        try { return ZoneId.of(timezone); } catch (Exception e) { return ZoneId.of("UTC"); }
    }

    private List<AdherenceDay> buildWeeklyAdherence(ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        List<AdherenceDay> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            String status = day.equals(today) ? "TODAY" : (day.isBefore(today) ? "PAST" : "SCHEDULED");
            days.add(new AdherenceDay(DAY_SHORTS[day.getDayOfWeek().getValue() - 1], status, day.format(DATE_FMT)));
        }
        return days;
    }
}

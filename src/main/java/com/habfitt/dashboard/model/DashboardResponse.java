package com.habfitt.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(
    String greeting,
    String state,
    StreakInfo streak,
    TodayInfo today,
    List<AdherenceDay> weeklyAdherence,
    Stats stats,
    String coachNote
) {

    public record StreakInfo(int current) {}

    public record TodayInfo(WorkoutInfo workout, boolean isCompleted) {}

    public record WorkoutInfo(
        String name,
        int estimatedDuration,
        int totalExercises,
        int completedExercises
    ) {}

    public record AdherenceDay(String dayShort, String status, String date) {}

    public record Stats(int workoutsThisWeek, int totalWorkouts, int daysRemaining) {}
}

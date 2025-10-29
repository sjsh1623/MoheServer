package com.mohe.spring.enums;

import java.time.LocalTime;

/**
 * 시간대 구분 Enum
 *
 * <p>하루를 5개 시간대로 구분하여 시간 기반 추천에 사용합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public enum TimeSlot {

    /** 새벽 (0-5시) */
    EARLY_MORNING(0, 5, "새벽", "early_morning"),

    /** 아침 (6-11시) */
    MORNING(6, 11, "아침", "morning"),

    /** 오후 (12-17시) */
    AFTERNOON(12, 17, "오후", "afternoon"),

    /** 저녁 (18-22시) */
    EVENING(18, 22, "저녁", "evening"),

    /** 밤 (23시) */
    LATE_NIGHT(23, 23, "밤", "late_night");

    private final int startHour;
    private final int endHour;
    private final String displayName;
    private final String key;

    TimeSlot(int startHour, int endHour, String displayName, String key) {
        this.startHour = startHour;
        this.endHour = endHour;
        this.displayName = displayName;
        this.key = key;
    }

    /**
     * 현재 시간에 해당하는 TimeSlot 반환
     *
     * @return 현재 시간대
     */
    public static TimeSlot fromCurrentTime() {
        return fromTime(LocalTime.now());
    }

    /**
     * 주어진 시간에 해당하는 TimeSlot 반환
     *
     * @param time 시간
     * @return 해당 시간대
     */
    public static TimeSlot fromTime(LocalTime time) {
        int hour = time.getHour();

        for (TimeSlot slot : values()) {
            if (hour >= slot.startHour && hour <= slot.endHour) {
                return slot;
            }
        }

        // 0시 이전 (23시 이후)는 LATE_NIGHT
        return LATE_NIGHT;
    }

    /**
     * 시간(hour)으로 TimeSlot 반환
     *
     * @param hour 시간 (0-23)
     * @return 해당 시간대
     */
    public static TimeSlot fromHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }

        for (TimeSlot slot : values()) {
            if (hour >= slot.startHour && hour <= slot.endHour) {
                return slot;
            }
        }

        return LATE_NIGHT;
    }

    // Getters
    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return displayName + " (" + startHour + "-" + endHour + "시)";
    }
}

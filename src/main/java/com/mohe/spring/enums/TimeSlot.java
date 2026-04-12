package com.mohe.spring.enums;

import java.time.LocalTime;

/**
 * 시간대 구분 Enum
 *
 * <p>하루를 6개 시간대로 구분하여 시간 기반 추천에 사용합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public enum TimeSlot {

    /** 새벽 (0-5시) */
    DAWN(0, 5, "새벽", "dawn"),

    /** 아침 (6-9시) */
    MORNING(6, 9, "아침", "morning"),

    /** 오전 (10-11시) */
    LATE_MORNING(10, 11, "오전", "late_morning"),

    /** 오후 (12-17시) */
    AFTERNOON(12, 17, "오후", "afternoon"),

    /** 저녁 (18-21시) */
    EVENING(18, 21, "저녁", "evening"),

    /** 밤 (22-23시) */
    NIGHT(22, 23, "밤", "night");

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

        // Fallback: 밤
        return NIGHT;
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

        return NIGHT;
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

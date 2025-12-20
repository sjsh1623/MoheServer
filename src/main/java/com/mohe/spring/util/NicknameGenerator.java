package com.mohe.spring.util;

import java.util.List;
import java.util.Random;

/**
 * Generates random Korean nicknames for anonymous reviews
 */
public class NicknameGenerator {

    private static final Random random = new Random();

    // Adjectives (형용사)
    private static final List<String> ADJECTIVES = List.of(
        "행복한", "즐거운", "따뜻한", "친절한", "귀여운",
        "멋진", "예쁜", "활발한", "조용한", "신나는",
        "상냥한", "밝은", "똑똑한", "재미있는", "웃음이 많은",
        "포근한", "시원한", "달콤한", "소중한", "특별한",
        "용감한", "부지런한", "착한", "정직한", "유쾌한"
    );

    // Nouns (동물/자연)
    private static final List<String> NOUNS = List.of(
        "고양이", "강아지", "토끼", "곰", "펭귄",
        "다람쥐", "호랑이", "여우", "사자", "코끼리",
        "팬더", "거북이", "올빼미", "독수리", "햄스터",
        "수달", "코알라", "기린", "물개", "앵무새",
        "나비", "꿀벌", "별", "달", "구름",
        "바람", "햇살", "무지개", "꽃", "나무"
    );

    /**
     * Generate a random nickname in format "형용사 명사"
     * Example: "행복한 고양이", "용감한 호랑이"
     */
    public static String generate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        return adjective + " " + noun;
    }

    /**
     * Generate a deterministic nickname based on a seed (e.g., review ID)
     * This ensures the same review always gets the same nickname
     */
    public static String generateFromSeed(long seed) {
        Random seededRandom = new Random(seed);
        String adjective = ADJECTIVES.get(seededRandom.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(seededRandom.nextInt(NOUNS.size()));
        return adjective + " " + noun;
    }
}

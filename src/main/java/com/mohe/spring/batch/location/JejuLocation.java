package com.mohe.spring.batch.location;

/**
 * 제주특별자치도 행정동 Enum (동만 수록)
 * - 제주시 19개, 서귀포시 12개 = 총 31개 행정동
 * 출처(검증): 위키백과 '제주시의 행정동', '서귀포시의 행정동' 분류 페이지.
 * - 제주시 행정동 19개: 건입동, 노형동, 도두동, 봉개동, 삼도1동, 삼도2동, 삼양동, 아라동, 연동, 오라동, 외도동, 용담1동, 용담2동, 이도1동, 이도2동, 이호동, 일도1동, 일도2동, 화북동
 * - 서귀포시 행정동 12개: 대륜동, 대천동, 동홍동, 서홍동, 송산동, 영천동, 예래동, 정방동, 중문동, 중앙동, 천지동, 효돈동
 */
public enum JejuLocation {
    // 제주시(19)
    JEJU_JEJU_GEONIP_DONG("제주특별자치도", "제주시", "건입동"),
    JEJU_JEJU_NOHYEONG_DONG("제주특별자치도", "제주시", "노형동"),
    JEJU_JEJU_DODU_DONG("제주특별자치도", "제주시", "도두동"),
    JEJU_JEJU_BONGGAE_DONG("제주특별자치도", "제주시", "봉개동"),
    JEJU_JEJU_SAMDO_1_DONG("제주특별자치도", "제주시", "삼도1동"),
    JEJU_JEJU_SAMDO_2_DONG("제주특별자치도", "제주시", "삼도2동"),
    JEJU_JEJU_SAMYANG_DONG("제주특별자치도", "제주시", "삼양동"),
    JEJU_JEJU_ARA_DONG("제주특별자치도", "제주시", "아라동"),
    JEJU_JEJU_YEON_DONG("제주특별자치도", "제주시", "연동"),
    JEJU_JEJU_ORA_DONG("제주특별자치도", "제주시", "오라동"),
    JEJU_JEJU_OEDO_DONG("제주특별자치도", "제주시", "외도동"),
    JEJU_JEJU_YONGDAM_1_DONG("제주특별자치도", "제주시", "용담1동"),
    JEJU_JEJU_YONGDAM_2_DONG("제주특별자치도", "제주시", "용담2동"),
    JEJU_JEJU_IDO_1_DONG("제주특별자치도", "제주시", "이도1동"),
    JEJU_JEJU_IDO_2_DONG("제주특별자치도", "제주시", "이도2동"),
    JEJU_JEJU_IHO_DONG("제주특별자치도", "제주시", "이호동"),
    JEJU_JEJU_ILDO_1_DONG("제주특별자치도", "제주시", "일도1동"),
    JEJU_JEJU_ILDO_2_DONG("제주특별자치도", "제주시", "일도2동"),
    JEJU_JEJU_HWABUK_DONG("제주특별자치도", "제주시", "화북동"),

    // 서귀포시(12)
    JEJU_SEOGWIPO_DAERYUN_DONG("제주특별자치도", "서귀포시", "대륜동"),
    JEJU_SEOGWIPO_DAECHEON_DONG("제주특별자치도", "서귀포시", "대천동"),
    JEJU_SEOGWIPO_DONGHONG_DONG("제주특별자치도", "서귀포시", "동홍동"),
    JEJU_SEOGWIPO_SEOHONG_DONG("제주특별자치도", "서귀포시", "서홍동"),
    JEJU_SEOGWIPO_SONGSAN_DONG("제주특별자치도", "서귀포시", "송산동"),
    JEJU_SEOGWIPO_YEONGCHEON_DONG("제주특별자치도", "서귀포시", "영천동"),
    JEJU_SEOGWIPO_YERAE_DONG("제주특별자치도", "서귀포시", "예래동"),
    JEJU_SEOGWIPO_JEONGBANG_DONG("제주특별자치도", "서귀포시", "정방동"),
    JEJU_SEOGWIPO_JUNGMUN_DONG("제주특별자치도", "서귀포시", "중문동"),
    JEJU_SEOGWIPO_JUNGANG_DONG("제주특별자치도", "서귀포시", "중앙동"),
    JEJU_SEOGWIPO_CHEONJI_DONG("제주특별자치도", "서귀포시", "천지동"),
    JEJU_SEOGWIPO_HYODON_DONG("제주특별자치도", "서귀포시", "효돈동");

    private final String province;  // "제주특별자치도"
    private final String city;      // "제주시" or "서귀포시"
    private final String dong;      // 행정동 명

    JejuLocation(String province, String city, String dong) {
        this.province = province;
        this.city = city;
        this.dong = dong;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getDong() {
        return dong;
    }

    /**
     * 풀네임: 예) "제주특별자치도 제주시 노형동"
     */
    public String getFullName() {
        return province + " " + city + " " + dong;
    }

    /**
     * "제주특별자치도 제주시 노형동" 같은 전체 한글 명칭으로 Enum을 찾습니다.
     */
    public static JejuLocation fromFullName(String fullKoreanName) {
        for (JejuLocation v : values()) {
            if (v.getFullName().equals(fullKoreanName)) return v;
        }
        throw new IllegalArgumentException("Unknown Jeju admin-dong: " + fullKoreanName);
    }
}

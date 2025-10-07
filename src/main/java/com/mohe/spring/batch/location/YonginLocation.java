package com.mohe.spring.batch.location;

/**
 * 경기도 용인특례시 행정동 Enum (동만 수록, 읍/면 제외)
 * - 수지구, 기흥구, 처인구의 행정동 총 32개
 * 검증 출처: 위키백과 '용인시의 행정동' 분류 및 각 구청 행정복지센터 현황 페이지.
 */
public enum YonginLocation {
    // 수지구
    YONGIN_SUJI_PUNGDEOKCHEON_1_DONG("경기도", "용인특례시", "수지구", "풍덕천1동"),
    YONGIN_SUJI_PUNGDEOKCHEON_2_DONG("경기도", "용인특례시", "수지구", "풍덕천2동"),
    YONGIN_SUJI_JUKJEON_1_DONG("경기도", "용인특례시", "수지구", "죽전1동"),
    YONGIN_SUJI_JUKJEON_2_DONG("경기도", "용인특례시", "수지구", "죽전2동"),
    YONGIN_SUJI_JUKJEON_3_DONG("경기도", "용인특례시", "수지구", "죽전3동"),
    YONGIN_SUJI_DONGCHEON_DONG("경기도", "용인특례시", "수지구", "동천동"),
    YONGIN_SUJI_SANGHYEON_1_DONG("경기도", "용인특례시", "수지구", "상현1동"),
    YONGIN_SUJI_SANGHYEON_2_DONG("경기도", "용인특례시", "수지구", "상현2동"),
    YONGIN_SUJI_SANGHYEON_3_DONG("경기도", "용인특례시", "수지구", "상현3동"),
    YONGIN_SUJI_SEONGBOK_DONG("경기도", "용인특례시", "수지구", "성복동"),
    YONGIN_SUJI_SINBONG_DONG("경기도", "용인특례시", "수지구", "신봉동"),

    // 기흥구
    YONGIN_GIHEUNG_SINGAL_DONG("경기도", "용인특례시", "기흥구", "신갈동"),
    YONGIN_GIHEUNG_GUGAL_DONG("경기도", "용인특례시", "기흥구", "구갈동"),
    YONGIN_GIHEUNG_GIHEUNG_DONG("경기도", "용인특례시", "기흥구", "기흥동"),
    YONGIN_GIHEUNG_SANGGAL_DONG("경기도", "용인특례시", "기흥구", "상갈동"),
    YONGIN_GIHEUNG_BORA_DONG("경기도", "용인특례시", "기흥구", "보라동"),
    YONGIN_GIHEUNG_YEONGDEOK_1_DONG("경기도", "용인특례시", "기흥구", "영덕1동"),
    YONGIN_GIHEUNG_YEONGDEOK_2_DONG("경기도", "용인특례시", "기흥구", "영덕2동"),
    YONGIN_GIHEUNG_GUSEONG_DONG("경기도", "용인특례시", "기흥구", "구성동"),
    YONGIN_GIHEUNG_MABUK_DONG("경기도", "용인특례시", "기흥구", "마북동"),
    YONGIN_GIHEUNG_CHEONGDEOK_DONG("경기도", "용인특례시", "기흥구", "청덕동"),
    YONGIN_GIHEUNG_BOJEONG_DONG("경기도", "용인특례시", "기흥구", "보정동"),
    YONGIN_GIHEUNG_DONGBACK_1_DONG("경기도", "용인특례시", "기흥구", "동백1동"),
    YONGIN_GIHEUNG_DONGBACK_2_DONG("경기도", "용인특례시", "기흥구", "동백2동"),
    YONGIN_GIHEUNG_DONGBACK_3_DONG("경기도", "용인특례시", "기흥구", "동백3동"),
    YONGIN_GIHEUNG_SANGHA_DONG("경기도", "용인특례시", "기흥구", "상하동"),
    YONGIN_GIHEUNG_SEONONG_DONG("경기도", "용인특례시", "기흥구", "서농동"),

    // 처인구
    YONGIN_CHEOIN_JUNGANG_DONG("경기도", "용인특례시", "처인구", "중앙동"),
    YONGIN_CHEOIN_DONGBU_DONG("경기도", "용인특례시", "처인구", "동부동"),
    YONGIN_CHEOIN_YEOKBUK_DONG("경기도", "용인특례시", "처인구", "역북동"),
    YONGIN_CHEOIN_YURIM_1_DONG("경기도", "용인특례시", "처인구", "유림1동"),
    YONGIN_CHEOIN_YURIM_2_DONG("경기도", "용인특례시", "처인구", "유림2동"),
    YONGIN_CHEOIN_SAMGA_DONG("경기도", "용인특례시", "처인구", "삼가동");

    private final String province;  // "경기도"
    private final String city;      // "용인특례시"
    private final String district;  // "수지구", "기흥구", "처인구"
    private final String dong;      // 행정동 명

    YonginLocation(String province, String city, String district, String dong) {
        this.province = province;
        this.city = city;
        this.district = district;
        this.dong = dong;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getDong() {
        return dong;
    }

    /**
     * 풀네임: 예) "경기도 용인특례시 수지구 풍덕천1동"
     */
    public String getFullName() {
        return province + " " + city + " " + district + " " + dong;
    }

    /**
     * "경기도 용인특례시 수지구 풍덕천1동" 같은 전체 한글 명칭으로 Enum을 찾습니다.
     */
    public static YonginLocation fromFullName(String fullKoreanName) {
        for (YonginLocation v : values()) {
            if (v.getFullName().equals(fullKoreanName)) return v;
        }
        throw new IllegalArgumentException("Unknown Yongin admin-dong: " + fullKoreanName);
    }
}

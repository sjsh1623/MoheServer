package com.mohe.spring.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 장소 카테고리 Enum
 *
 * <p>추천 시스템에서 사용되는 장소 카테고리를 정의합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public enum PlaceCategory {

    // ============================================
    // 음료
    // ============================================
    CAFE("카페", "cafe", "커피 한 잔과 함께 여유를",
         Arrays.asList("카페", "커피", "디저트", "coffee", "cafe"), "☕"),

    BRUNCH_CAFE("브런치카페", "brunch_cafe", "맛있는 브런치로 하루를 시작",
                Arrays.asList("브런치", "카페", "아침식사", "brunch"), "🥐"),

    BAKERY("베이커리", "bakery", "갓 구운 빵 냄새가 가득한",
           Arrays.asList("베이커리", "빵집", "디저트", "bakery", "bread"), "🥖"),

    TEA_HOUSE("티하우스", "tea_house", "따뜻한 차 한 잔의 여유",
              Arrays.asList("차", "티하우스", "전통차", "tea"), "🍵"),

    DESSERT_CAFE("디저트카페", "dessert_cafe", "달콤한 디저트와 함께",
                 Arrays.asList("디저트", "카페", "케이크", "dessert"), "🍰"),

    // ============================================
    // 식사
    // ============================================
    RESTAURANT("맛집", "restaurant", "맛있는 한 끼 식사를",
               Arrays.asList("맛집", "음식점", "레스토랑", "restaurant", "food"), "🍽️"),

    FINE_DINING("파인다이닝", "fine_dining", "특별한 식사 경험을",
                Arrays.asList("레스토랑", "파인다이닝", "고급", "fine dining"), "🍷"),

    KOREAN_FOOD("한식당", "korean_food", "한국의 맛을 느끼는",
                Arrays.asList("한식", "한정식", "맛집", "korean food"), "🍚"),

    ITALIAN("이탈리안", "italian", "이탈리아의 풍미를",
            Arrays.asList("이탈리안", "파스타", "피자", "italian"), "🍝"),

    JAPANESE("일식당", "japanese", "일본의 정통 맛을",
             Arrays.asList("일식", "초밥", "라멘", "japanese", "sushi"), "🍣"),

    // ============================================
    // 주류
    // ============================================
    BAR("바", "bar", "분위기 좋은 바에서 칵테일을",
        Arrays.asList("바", "칵테일", "술집", "bar", "cocktail"), "🍸"),

    WINE_BAR("와인바", "wine_bar", "와인 한 잔과 함께 여유를",
             Arrays.asList("와인바", "와인", "바", "wine bar", "wine"), "🍷"),

    CRAFT_BEER("크래프트비어", "craft_beer", "특별한 수제맥주를",
               Arrays.asList("맥주", "크래프트", "펍", "craft beer", "beer"), "🍺"),

    ROOFTOP_BAR("루프탑바", "rooftop_bar", "루프탑에서 즐기는 야경",
                Arrays.asList("루프탑", "바", "야경", "rooftop"), "🌃"),

    PUB("펍", "pub", "편안한 분위기의 펍에서",
        Arrays.asList("펍", "호프", "맥주", "pub"), "🍻"),

    // ============================================
    // 문화/예술
    // ============================================
    GALLERY("갤러리", "gallery", "예술 작품을 감상하며",
            Arrays.asList("갤러리", "미술관", "전시", "gallery", "art"), "🖼️"),

    MUSEUM("박물관", "museum", "역사와 문화를 체험",
           Arrays.asList("박물관", "전시관", "문화", "museum"), "🏛️"),

    EXHIBITION("전시회", "exhibition", "다양한 전시를 관람",
               Arrays.asList("전시", "전시회", "갤러리", "exhibition"), "🎨"),

    BOOKSTORE("서점", "bookstore", "책과 함께하는 시간",
              Arrays.asList("서점", "책방", "북카페", "bookstore", "book"), "📚"),

    LIBRARY_CAFE("북카페", "library_cafe", "책과 커피가 있는 공간",
                 Arrays.asList("북카페", "카페", "서점", "library cafe"), "📖"),

    // ============================================
    // 체험/활동
    // ============================================
    WORKSHOP("공방", "workshop", "손으로 만드는 즐거움",
             Arrays.asList("공방", "체험", "만들기", "workshop", "craft"), "🎨"),

    CRAFT_STUDIO("공예공방", "craft_studio", "나만의 작품 만들기",
                 Arrays.asList("공예", "공방", "체험", "craft studio"), "✂️"),

    POTTERY("도예공방", "pottery", "도자기 만들기 체험",
            Arrays.asList("도예", "도자기", "공방", "pottery"), "🏺"),

    // ============================================
    // 야외/자연
    // ============================================
    PARK("공원", "park", "자연 속에서 힐링",
         Arrays.asList("공원", "자연", "산책", "park"), "🌳"),

    WALKING_TRAIL("산책로", "walking_trail", "걷기 좋은 산책로",
                  Arrays.asList("산책로", "트레킹", "걷기", "walking", "trail"), "🚶"),

    SCENIC_SPOT("전망대", "scenic_spot", "멋진 경치를 감상",
                Arrays.asList("전망대", "야경", "뷰", "view", "scenic"), "🌆"),

    HANGANG_PARK("한강공원", "hangang_park", "한강에서 여유를",
                 Arrays.asList("한강", "공원", "야외", "hangang"), "🌊"),

    // ============================================
    // 쇼핑
    // ============================================
    SHOPPING_MALL("쇼핑몰", "shopping_mall", "쇼핑의 즐거움",
                  Arrays.asList("쇼핑몰", "쇼핑", "백화점", "shopping mall"), "🛍️"),

    VINTAGE_SHOP("빈티지샵", "vintage_shop", "빈티지 아이템 찾기",
                 Arrays.asList("빈티지", "중고", "샵", "vintage"), "👗"),

    LOCAL_MARKET("로컬마켓", "local_market", "특색있는 로컬 상점",
                 Arrays.asList("마켓", "시장", "로컬", "market", "local"), "🏪"),

    // ============================================
    // 엔터테인먼트
    // ============================================
    CINEMA("영화관", "cinema", "영화 한 편 어떠세요",
           Arrays.asList("영화관", "영화", "시네마", "cinema", "movie"), "🎬"),

    THEATER("공연장", "theater", "공연 관람",
            Arrays.asList("공연장", "극장", "공연", "theater"), "🎭"),

    LIVE_MUSIC("라이브음악", "live_music", "라이브 음악을 즐기는",
               Arrays.asList("라이브", "음악", "공연", "live music"), "🎵"),

    JAZZ_BAR("재즈바", "jazz_bar", "재즈 선율과 함께",
             Arrays.asList("재즈", "바", "라이브", "jazz bar", "jazz"), "🎷"),

    // ============================================
    // 취미/운동
    // ============================================
    HOBBY("취미생활", "hobby", "나만의 취미 시간",
          Arrays.asList("취미", "여가", "활동", "hobby"), "🎯"),

    SPORTS("스포츠", "sports", "운동으로 활력을",
           Arrays.asList("스포츠", "운동", "헬스", "sports", "fitness"), "⚽"),

    YOGA_STUDIO("요가스튜디오", "yoga_studio", "요가로 몸과 마음을",
                Arrays.asList("요가", "필라테스", "운동", "yoga"), "🧘"),

    // ============================================
    // 24시간
    // ============================================
    LATE_NIGHT_CAFE("심야카페", "late_night_cafe", "밤늦게 열린 카페",
                    Arrays.asList("24시", "카페", "심야", "late night"), "🌙"),

    NIGHT_RESTAURANT("심야식당", "night_restaurant", "밤에도 맛있는 한 끼",
                     Arrays.asList("심야", "식당", "24시", "night restaurant"), "🍜");

    // ============================================
    // 필드
    // ============================================
    private final String displayName;      // 사용자에게 보이는 이름
    private final String key;              // API에서 사용하는 키
    private final String description;      // 설명문구
    private final List<String> keywords;   // 검색 키워드
    private final String emoji;            // 이모지

    PlaceCategory(String displayName, String key, String description,
                  List<String> keywords, String emoji) {
        this.displayName = displayName;
        this.key = key;
        this.description = description;
        this.keywords = keywords;
        this.emoji = emoji;
    }

    /**
     * 키워드로 PlaceCategory 찾기
     *
     * @param keyword 검색 키워드
     * @return 일치하는 PlaceCategory, 없으면 null
     */
    public static PlaceCategory fromKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String lowerKeyword = keyword.toLowerCase().trim();

        for (PlaceCategory category : values()) {
            for (String kw : category.keywords) {
                if (kw.toLowerCase().contains(lowerKeyword) ||
                    lowerKeyword.contains(kw.toLowerCase())) {
                    return category;
                }
            }
        }

        return null;
    }

    /**
     * Key로 PlaceCategory 찾기
     *
     * @param key 카테고리 키
     * @return 일치하는 PlaceCategory, 없으면 null
     */
    public static PlaceCategory fromKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        for (PlaceCategory category : values()) {
            if (category.key.equalsIgnoreCase(key.trim())) {
                return category;
            }
        }

        return null;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getEmoji() {
        return emoji;
    }

    @Override
    public String toString() {
        return emoji + " " + displayName;
    }
}

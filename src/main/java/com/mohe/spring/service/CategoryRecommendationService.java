package com.mohe.spring.service;

import com.mohe.spring.dto.CategoryDto;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.dto.SuggestedCategoriesResponse;
import com.mohe.spring.entity.Place;
import com.mohe.spring.enums.CategoryRecommendationRule;
import com.mohe.spring.enums.PlaceCategory;
import com.mohe.spring.enums.TimeSlot;
import com.mohe.spring.enums.WeatherCondition;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.repository.PlaceImageRepository;
import com.mohe.spring.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 카테고리 추천 서비스
 *
 * <p>시간대와 날씨 정보를 기반으로 적합한 장소 카테고리를 추천합니다.</p>
 */
@Service
public class CategoryRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryRecommendationService.class);

    private final WeatherService weatherService;
    private final BookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;
    private final PlaceService placeService;
    private final PlaceImageRepository placeImageRepository;

    // MBTI별 선호 카테고리 (fallback용)
    private static final Map<String, List<PlaceCategory>> MBTI_PREFERENCES = Map.ofEntries(
        Map.entry("ENFP", List.of(PlaceCategory.CAFE, PlaceCategory.WORKSHOP, PlaceCategory.EXHIBITION, PlaceCategory.LIVE_MUSIC)),
        Map.entry("ENFJ", List.of(PlaceCategory.RESTAURANT, PlaceCategory.GALLERY, PlaceCategory.FLOWER_CAFE, PlaceCategory.TERRACE)),
        Map.entry("INFP", List.of(PlaceCategory.LIBRARY_CAFE, PlaceCategory.GALLERY, PlaceCategory.WALKING_TRAIL, PlaceCategory.TEA_HOUSE)),
        Map.entry("INFJ", List.of(PlaceCategory.BOOKSTORE, PlaceCategory.TEA_HOUSE, PlaceCategory.BOTANICAL_GARDEN, PlaceCategory.CAFE)),
        Map.entry("ENTP", List.of(PlaceCategory.BAR, PlaceCategory.ROOFTOP_BAR, PlaceCategory.ESCAPE_ROOM, PlaceCategory.CRAFT_BEER)),
        Map.entry("ENTJ", List.of(PlaceCategory.FINE_DINING, PlaceCategory.LOUNGE_BAR, PlaceCategory.RESTAURANT, PlaceCategory.SCENIC_SPOT)),
        Map.entry("INTP", List.of(PlaceCategory.BOOKSTORE, PlaceCategory.MUSEUM, PlaceCategory.STUDY_CAFE, PlaceCategory.CAFE)),
        Map.entry("INTJ", List.of(PlaceCategory.MUSEUM, PlaceCategory.CAFE, PlaceCategory.WINE_BAR, PlaceCategory.GALLERY)),
        Map.entry("ESFP", List.of(PlaceCategory.KARAOKE, PlaceCategory.BOWLING, PlaceCategory.PUB, PlaceCategory.CRAFT_BEER)),
        Map.entry("ESTP", List.of(PlaceCategory.SPORTS, PlaceCategory.BAR, PlaceCategory.BOARD_GAME, PlaceCategory.ROOFTOP_BAR)),
        Map.entry("ISFP", List.of(PlaceCategory.VINTAGE_SHOP, PlaceCategory.WORKSHOP, PlaceCategory.PHOTO_BOOTH, PlaceCategory.FLOWER_CAFE)),
        Map.entry("ISTP", List.of(PlaceCategory.CRAFT_BEER, PlaceCategory.BILLIARDS, PlaceCategory.CAFE, PlaceCategory.FITNESS)),
        Map.entry("ESFJ", List.of(PlaceCategory.RESTAURANT, PlaceCategory.KOREAN_FOOD, PlaceCategory.BAKERY, PlaceCategory.DESSERT_CAFE)),
        Map.entry("ESTJ", List.of(PlaceCategory.FINE_DINING, PlaceCategory.RESTAURANT, PlaceCategory.MEAT, PlaceCategory.KOREAN_FOOD)),
        Map.entry("ISFJ", List.of(PlaceCategory.CAFE, PlaceCategory.BAKERY, PlaceCategory.PARK, PlaceCategory.TEA_HOUSE)),
        Map.entry("ISTJ", List.of(PlaceCategory.CAFE, PlaceCategory.RESTAURANT, PlaceCategory.BOOKSTORE, PlaceCategory.PARK))
    );

    // 시간대별 × 카테고리별 표시 타이틀 (frontend에 그대로 전달)
    private static final Map<TimeSlot, Map<String, String>> TIME_CATEGORY_TITLES = buildTimeCategoryTitles();

    private static Map<TimeSlot, Map<String, String>> buildTimeCategoryTitles() {
        Map<TimeSlot, Map<String, String>> map = new EnumMap<>(TimeSlot.class);

        // ============================================
        // DAWN (새벽, 0-5시)
        // ============================================
        Map<String, String> dawn = new LinkedHashMap<>();
        dawn.put("late_night_cafe", "새벽에 열린 카페 찾고 있어요");
        dawn.put("night_restaurant", "새벽에도 문 연 곳이 있어요");
        dawn.put("late_night_food", "출출한 새벽, 야식 한 그릇");
        dawn.put("porridge", "속 풀리는 해장이 필요해요");
        dawn.put("stew", "뜨끈한 국물로 속을 달래요");
        dawn.put("ramen", "따끈한 라멘으로 마무리해요");
        dawn.put("korean_food", "든든한 한 그릇으로 해장을");
        dawn.put("bar", "조용한 새벽, 한 잔 기울여요");
        dawn.put("pub", "새벽까지 이어지는 한 잔");
        dawn.put("wine_bar", "새벽의 와인 한 잔");
        dawn.put("jazz_bar", "재즈 선율과 함께하는 새벽");
        dawn.put("lounge_bar", "고요한 새벽의 라운지");
        dawn.put("craft_beer", "새벽에 즐기는 수제맥주");
        dawn.put("izakaya", "새벽 이자카야의 운치");
        dawn.put("indoor_pocha", "포차에서의 진한 새벽");
        dawn.put("karaoke", "새벽 감성으로 한 곡");
        dawn.put("tea_house", "따뜻한 차 한 잔으로 새벽을");
        dawn.put("scenic_spot", "새벽 밤하늘을 감상해요");
        dawn.put("hangang_park", "새벽 한강에서 고요함을");
        map.put(TimeSlot.DAWN, dawn);

        // ============================================
        // MORNING (아침, 6-9시)
        // ============================================
        Map<String, String> morning = new LinkedHashMap<>();
        morning.put("cafe", "모닝 커피 한 잔 어때요?");
        morning.put("brunch_cafe", "든든한 브런치로 하루를 시작해요");
        morning.put("bakery", "갓 구운 빵 냄새 맡으러 가요");
        morning.put("dessert_cafe", "상큼한 디저트와 아침을");
        morning.put("tea_house", "따뜻한 차 한 잔으로 여는 아침");
        morning.put("library_cafe", "책과 함께 여유로운 아침을");
        morning.put("study_cafe", "집중하기 좋은 아침이에요");
        morning.put("park", "산뜻한 아침 공기 마시러 가요");
        morning.put("walking_trail", "아침 산책으로 상쾌하게");
        morning.put("hangang_park", "한강의 아침 바람을 느껴요");
        morning.put("botanical_garden", "아침 식물원 산책 어때요?");
        morning.put("flower_cafe", "꽃 향기 가득한 아침을");
        morning.put("terrace", "햇살 드는 테라스에서 아침을");
        morning.put("yoga", "아침 요가로 몸을 깨워요");
        morning.put("bookstore", "책 한 권과 함께 시작하는 아침");
        morning.put("gallery", "고요한 아침, 갤러리 산책");
        morning.put("porridge", "속 편한 아침 한 그릇");
        morning.put("spa", "아침 스파로 컨디션을 올려요");
        morning.put("korean_food", "든든한 한식으로 여는 하루");
        morning.put("stew", "뜨끈한 국물로 아침을");
        map.put(TimeSlot.MORNING, morning);

        // ============================================
        // LATE_MORNING (오전, 10-11시)
        // ============================================
        Map<String, String> lateMorning = new LinkedHashMap<>();
        lateMorning.put("brunch_cafe", "늦은 아침, 브런치 타임이에요");
        lateMorning.put("cafe", "여유롭게 커피 한 잔 할까요?");
        lateMorning.put("bakery", "따끈한 빵과 함께하는 오전");
        lateMorning.put("dessert_cafe", "달콤한 디저트로 기분 전환을");
        lateMorning.put("salad", "가볍고 건강한 한 끼 어때요?");
        lateMorning.put("sandwich", "간단하게 샌드위치 한 끼");
        lateMorning.put("park", "오전의 공원에서 여유를");
        lateMorning.put("botanical_garden", "식물원에서 초록을 만끽해요");
        lateMorning.put("flower_cafe", "꽃 내음 가득한 오전을");
        lateMorning.put("terrace", "햇살 좋은 테라스에서 브런치");
        lateMorning.put("bookstore", "조용한 오전, 책 한 권 어때요?");
        lateMorning.put("library_cafe", "북카페에서 여유롭게");
        lateMorning.put("gallery", "오전 갤러리 산책 어때요?");
        lateMorning.put("exhibition", "한적한 오전, 전시 관람을");
        lateMorning.put("museum", "오전의 박물관 나들이");
        lateMorning.put("shopping_mall", "오전 쇼핑으로 기분 전환을");
        lateMorning.put("board_game", "친구와 보드게임 어때요?");
        lateMorning.put("manga_cafe", "만화카페에서 여유로운 오전을");
        lateMorning.put("chocolate", "진한 초콜릿으로 달콤하게");
        lateMorning.put("tea_house", "차 한 잔의 여유가 필요해요");
        lateMorning.put("korean_food", "정갈한 한식 한 상 어때요?");
        map.put(TimeSlot.LATE_MORNING, lateMorning);

        // ============================================
        // AFTERNOON (오후, 12-17시)
        // ============================================
        Map<String, String> afternoon = new LinkedHashMap<>();
        afternoon.put("cafe", "오후의 여유로운 커피 타임이에요");
        afternoon.put("dessert_cafe", "달콤한 디저트가 필요한 오후예요");
        afternoon.put("restaurant", "든든한 점심 한 끼 어때요?");
        afternoon.put("korean_food", "든든한 한식 한 상이 땡겨요");
        afternoon.put("park", "오후 햇살 아래 산책을");
        afternoon.put("hangang_park", "한강에서 오후의 여유를");
        afternoon.put("walking_trail", "걷기 좋은 오후예요");
        afternoon.put("gallery", "오후 갤러리 관람 어때요?");
        afternoon.put("museum", "문화가 있는 오후를 보내요");
        afternoon.put("exhibition", "전시회에서 영감을 얻어요");
        afternoon.put("shopping_mall", "오후에 쇼핑 한 바퀴");
        afternoon.put("workshop", "직접 만드는 즐거움을 느껴요");
        afternoon.put("bookstore", "책 향기 가득한 오후");
        afternoon.put("terrace", "테라스에서 오후를 만끽해요");
        afternoon.put("scenic_spot", "멋진 뷰와 함께하는 오후");
        afternoon.put("botanical_garden", "초록이 가득한 오후 산책");
        afternoon.put("photo_booth", "추억을 남기고 싶은 오후");
        afternoon.put("cinema", "영화 한 편 보는 오후 어때요?");
        afternoon.put("escape_room", "색다른 오후 액티비티는 어때요?");
        afternoon.put("board_game", "보드게임으로 즐거운 오후를");
        afternoon.put("bowling", "오후에 즐기는 볼링 한 판");
        afternoon.put("manga_cafe", "만화카페에서 릴렉스");
        afternoon.put("spa", "스파로 힐링하는 오후");
        afternoon.put("stew", "뜨끈한 국물이 생각나는 오후");
        afternoon.put("ramen", "얼큰한 라멘 한 그릇 어때요?");
        afternoon.put("tea_house", "차 한 잔과 함께하는 오후");
        map.put(TimeSlot.AFTERNOON, afternoon);

        // ============================================
        // EVENING (저녁, 18-21시)
        // ============================================
        Map<String, String> evening = new LinkedHashMap<>();
        evening.put("restaurant", "든든한 저녁 식사 어떠세요?");
        evening.put("fine_dining", "특별한 저녁을 보내요");
        evening.put("korean_food", "한식으로 든든한 저녁을");
        evening.put("italian", "이탈리안으로 저녁 데이트");
        evening.put("meat", "고기 구워 먹는 저녁 어때요?");
        evening.put("shabu", "따끈한 샤브샤브로 저녁을");
        evening.put("stew", "뜨끈한 찌개로 저녁을");
        evening.put("ramen", "진한 라멘으로 저녁을");
        evening.put("bar", "하루 끝에 한 잔 어떠세요?");
        evening.put("wine_bar", "와인 한 잔으로 마무리해요");
        evening.put("craft_beer", "시원한 맥주 한 잔 하러 가요");
        evening.put("pub", "펍에서 편안한 저녁을");
        evening.put("jazz_bar", "재즈 선율과 함께하는 저녁");
        evening.put("izakaya", "이자카야의 정취를 즐겨요");
        evening.put("rooftop_bar", "야경 보며 한 잔 어때요?");
        evening.put("indoor_pocha", "포차에서 소주 한 잔");
        evening.put("cafe", "조용한 저녁의 카페 타임");
        evening.put("scenic_spot", "반짝이는 야경을 감상해요");
        evening.put("hangang_park", "한강의 야경을 즐겨요");
        evening.put("date_spot", "로맨틱한 저녁 데이트 어때요?");
        evening.put("terrace", "선선한 저녁, 테라스 자리");
        evening.put("live_music", "라이브 음악과 함께하는 저녁");
        evening.put("theater", "공연 관람으로 특별한 저녁을");
        evening.put("tea_house", "차 한 잔으로 저녁을 마무리해요");
        map.put(TimeSlot.EVENING, evening);

        // ============================================
        // NIGHT (밤, 22-23시)
        // ============================================
        Map<String, String> night = new LinkedHashMap<>();
        night.put("late_night_cafe", "늦은 밤, 카페에서 한숨 돌려요");
        night.put("night_restaurant", "밤에도 든든하게 한 끼");
        night.put("late_night_food", "야식엔 역시 족발이죠");
        night.put("chicken", "치맥이 땡기는 밤이에요");
        night.put("bar", "늦은 밤, 한 잔 더 할까요?");
        night.put("pub", "밤을 이어가는 펍 한 잔");
        night.put("wine_bar", "깊어가는 밤의 와인 한 잔");
        night.put("craft_beer", "시원한 맥주로 밤을 즐겨요");
        night.put("jazz_bar", "재즈가 흐르는 밤");
        night.put("izakaya", "이자카야에서 깊어가는 밤");
        night.put("lounge_bar", "라운지에서 우아한 밤을");
        night.put("indoor_pocha", "포차에서 밤을 불태워요");
        night.put("rooftop_bar", "야경 보며 밤바람을");
        night.put("karaoke", "노래방에서 스트레스 해소!");
        night.put("hangang_park", "밤의 한강을 산책해요");
        night.put("ramen", "야식으로 라멘 한 그릇");
        night.put("stew", "늦은 밤, 속 풀리는 국물");
        night.put("porridge", "속 편한 밤의 한 그릇");
        night.put("korean_food", "밤에 든든한 한식 한 상");
        night.put("tea_house", "차 한 잔으로 차분한 밤");
        map.put(TimeSlot.NIGHT, night);

        return map;
    }

    // MBTI별 "오늘은 이런 곳 어때요?" 타이틀
    private static final Map<String, List<String>> MBTI_TITLES = Map.ofEntries(
        Map.entry("ENFP", List.of("오늘은 이런 곳 어때요?", "새로운 곳 탐험해볼까요?", "영감이 필요한 하루")),
        Map.entry("ENFJ", List.of("오늘은 이런 곳 어때요?", "함께하면 더 좋은 곳", "특별한 하루를 만들어요")),
        Map.entry("INFP", List.of("오늘은 이런 곳 어때요?", "조용히 나만의 시간을", "감성 충전이 필요한 날")),
        Map.entry("INFJ", List.of("오늘은 이런 곳 어때요?", "마음이 편해지는 곳", "깊이 있는 시간을")),
        Map.entry("ENTP", List.of("오늘은 이런 곳 어때요?", "뭔가 재밌는 거 없을까?", "색다른 경험을 찾아서")),
        Map.entry("ENTJ", List.of("오늘은 이런 곳 어때요?", "특별한 곳으로 가볼까요?", "품격 있는 시간을")),
        Map.entry("INTP", List.of("오늘은 이런 곳 어때요?", "혼자만의 시간이 필요해", "집중하기 좋은 곳")),
        Map.entry("INTJ", List.of("오늘은 이런 곳 어때요?", "효율적인 하루를 위해", "나만의 공간을 찾아서")),
        Map.entry("ESFP", List.of("오늘은 이런 곳 어때요?", "신나는 하루 보내요!", "놀 거리가 필요해!")),
        Map.entry("ESTP", List.of("오늘은 이런 곳 어때요?", "액티브한 하루!", "뭔가 짜릿한 거 없을까?")),
        Map.entry("ISFP", List.of("오늘은 이런 곳 어때요?", "예쁜 곳 찾아 떠나요", "감성 가득한 곳으로")),
        Map.entry("ISTP", List.of("오늘은 이런 곳 어때요?", "조용히 즐길 수 있는 곳", "나만 아는 숨은 공간")),
        Map.entry("ESFJ", List.of("오늘은 이런 곳 어때요?", "맛있는 곳 찾았어요!", "함께 가면 좋은 곳")),
        Map.entry("ESTJ", List.of("오늘은 이런 곳 어때요?", "검증된 맛집으로!", "확실한 곳으로 가요")),
        Map.entry("ISFJ", List.of("오늘은 이런 곳 어때요?", "편안한 곳에서 쉬어요", "소소한 행복을 찾아서")),
        Map.entry("ISTJ", List.of("오늘은 이런 곳 어때요?", "믿을 수 있는 곳으로", "안정적인 선택"))
    );

    public CategoryRecommendationService(
            WeatherService weatherService,
            BookmarkRepository bookmarkRepository,
            PlaceRepository placeRepository,
            PlaceService placeService,
            PlaceImageRepository placeImageRepository) {
        this.weatherService = weatherService;
        this.bookmarkRepository = bookmarkRepository;
        this.placeRepository = placeRepository;
        this.placeService = placeService;
        this.placeImageRepository = placeImageRepository;
    }

    /**
     * 현재 시간과 날씨 기반 카테고리 추천
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 추천 카테고리 응답
     */
    public SuggestedCategoriesResponse getSuggestedCategories(Double latitude, Double longitude) {
        // 1. 현재 시간대 파악
        TimeSlot currentTimeSlot = TimeSlot.fromCurrentTime();
        logger.info("Current time slot: {}", currentTimeSlot.getDisplayName());

        // 2. 날씨 정보 가져오기
        WeatherCondition weatherCondition = getWeatherCondition(latitude, longitude);
        logger.info("Weather condition: {}", weatherCondition.getDisplayName());

        // 3. 추천 규칙 찾기
        CategoryRecommendationRule rule = CategoryRecommendationRule.findRule(currentTimeSlot, weatherCondition);
        logger.info("Applied rule: {} / {}", rule.getTimeSlot().getDisplayName(), rule.getWeatherCondition().getDisplayName());

        // 4. 추천 카테고리 변환
        List<CategoryDto> suggestedCategories = rule.getRecommendedCategories().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());

        // 5. 응답 생성
        SuggestedCategoriesResponse response = new SuggestedCategoriesResponse();
        response.setTimeSlot(currentTimeSlot.getDisplayName());
        response.setWeather(weatherCondition.getDisplayName());
        response.setReason(rule.getReasonText());
        response.setSuggestedCategories(suggestedCategories);
        response.setLocation(new SuggestedCategoriesResponse.LocationInfo(latitude, longitude));

        return response;
    }

    /**
     * 카테고리만 (DB 미사용, 즉시 응답)
     */
    public Map<String, Object> getHomeCategoriesOnly(Double lat, Double lon) {
        Map<String, Object> result = new LinkedHashMap<>();
        TimeSlot currentTimeSlot = TimeSlot.fromCurrentTime();
        WeatherCondition weatherCondition = getWeatherCondition(lat, lon);
        CategoryRecommendationRule rule = CategoryRecommendationRule.findRule(currentTimeSlot, weatherCondition);

        result.put("timeSlot", currentTimeSlot.getDisplayName());
        result.put("weather", weatherCondition.getDisplayName());
        result.put("reason", rule.getReasonText());

        List<Map<String, Object>> categoryRows = buildCategoryRows(rule, currentTimeSlot);
        result.put("categoryRows", categoryRows);
        return result;
    }

    /**
     * MBTI row만 (DB 사용, 별도 로드)
     */
    public Map<String, Object> getMbtiRowOnly(Double lat, Double lon, String mbti, int limit) {
        if (mbti == null || mbti.isBlank()) return null;
        return buildMbtiRow(mbti.toUpperCase().trim(), lat, lon, limit);
    }

    /**
     * 홈 화면 통합 데이터 (레거시 호환)
     */
    public Map<String, Object> getHomeData(Double lat, Double lon, String mbti, int placesPerCategory) {
        Map<String, Object> result = new LinkedHashMap<>();

        TimeSlot currentTimeSlot = TimeSlot.fromCurrentTime();

        // 날씨 + MBTI 병렬 실행
        CompletableFuture<WeatherCondition> weatherFuture = CompletableFuture.supplyAsync(() -> getWeatherCondition(lat, lon));
        CompletableFuture<Map<String, Object>> mbtiFuture = (mbti != null && !mbti.isBlank())
                ? CompletableFuture.supplyAsync(() -> buildMbtiRow(mbti.toUpperCase().trim(), lat, lon, placesPerCategory))
                : CompletableFuture.completedFuture(null);

        WeatherCondition weatherCondition;
        try {
            weatherCondition = weatherFuture.join();
        } catch (Exception e) {
            logger.warn("Weather fetch failed: {}", e.getMessage());
            weatherCondition = WeatherCondition.SUNNY;
        }

        CategoryRecommendationRule rule = CategoryRecommendationRule.findRule(currentTimeSlot, weatherCondition);
        result.put("timeSlot", currentTimeSlot.getDisplayName());
        result.put("weather", weatherCondition.getDisplayName());
        result.put("reason", rule.getReasonText());

        try {
            Map<String, Object> mbtiRow = mbtiFuture.join();
            if (mbtiRow != null) {
                result.put("mbtiRow", mbtiRow);
            }
        } catch (Exception e) {
            logger.warn("MBTI row failed: {}", e.getMessage());
        }

        result.put("categoryRows", buildCategoryRows(rule, currentTimeSlot));
        return result;
    }

    private List<Map<String, Object>> buildCategoryRows(CategoryRecommendationRule rule, TimeSlot timeSlot) {
        List<Map<String, Object>> categoryRows = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();

        for (PlaceCategory category : rule.getRecommendedCategories()) {
            categoryRows.add(categoryToMap(category, timeSlot));
            usedKeys.add(category.getKey());
        }

        List<PlaceCategory> filler = List.of(
            PlaceCategory.CAFE, PlaceCategory.RESTAURANT, PlaceCategory.KOREAN_FOOD,
            PlaceCategory.JAPANESE_FOOD, PlaceCategory.CHINESE_FOOD, PlaceCategory.WESTERN_FOOD,
            PlaceCategory.CHICKEN, PlaceCategory.MEAT, PlaceCategory.SEAFOOD,
            PlaceCategory.DESSERT_CAFE, PlaceCategory.BAKERY, PlaceCategory.BAR,
            PlaceCategory.WINE_BAR, PlaceCategory.CRAFT_BEER, PlaceCategory.PUB,
            PlaceCategory.GALLERY, PlaceCategory.MUSEUM, PlaceCategory.WORKSHOP,
            PlaceCategory.PARK, PlaceCategory.WALKING_TRAIL, PlaceCategory.BOOKSTORE,
            PlaceCategory.SHOPPING_MALL, PlaceCategory.CINEMA, PlaceCategory.KARAOKE,
            PlaceCategory.BOARD_GAME, PlaceCategory.ESCAPE_ROOM, PlaceCategory.SPA,
            PlaceCategory.FITNESS, PlaceCategory.HANOK, PlaceCategory.INSTAGRAMMABLE
        );
        for (PlaceCategory cat : filler) {
            if (categoryRows.size() >= 30) break;
            if (!usedKeys.contains(cat.getKey())) {
                categoryRows.add(categoryToMap(cat, timeSlot));
                usedKeys.add(cat.getKey());
            }
        }
        return categoryRows;
    }

    /**
     * MBTI 기반 첫 줄 데이터 구성
     */
    private Map<String, Object> buildMbtiRow(String mbti, Double lat, Double lon, int limit) {
        Map<String, Object> row = new LinkedHashMap<>();

        // 타이틀
        List<String> titles = MBTI_TITLES.getOrDefault(mbti, List.of("오늘은 이런 곳 어때요?"));
        row.put("title", titles.get(new Random().nextInt(titles.size())));
        row.put("mbti", mbti);

        // 1차: 동일 MBTI 사용자 좋아요 순
        List<Place> mbtiPlaces;
        try {
            mbtiPlaces = bookmarkRepository.findPopularPlacesByMbti(mbti, lat, lon, 50.0, limit);
        } catch (Exception e) {
            logger.warn("MBTI bookmark query failed: {}", e.getMessage());
            mbtiPlaces = List.of();
        }

        if (mbtiPlaces.size() >= 3) {
            row.put("source", "bookmark");
            row.put("places", placesToSimpleDtos(mbtiPlaces, lat, lon));
            return row;
        }

        // 2차 fallback: MBTI 선호 카테고리 키워드로 병렬 검색
        List<PlaceCategory> preferredCategories = MBTI_PREFERENCES.get(mbti);
        if (preferredCategories == null) return null;

        String[] koDays = {"일", "월", "화", "수", "목", "금", "토"};
        String day = koDays[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1];
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        // 모든 카테고리 쿼리를 병렬 실행
        List<CompletableFuture<List<Place>>> futures = preferredCategories.stream()
                .map(cat -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String kws = cat.getKeywords().stream().map(String::toLowerCase).collect(Collectors.joining(","));
                        return placeRepository.findNearbyPlacesByCategory(lat, lon, 30.0, kws, 5, day, time);
                    } catch (Exception e) {
                        return List.<Place>of();
                    }
                }))
                .toList();

        Set<Long> seenIds = new HashSet<>();
        List<Place> fallbackPlaces = new ArrayList<>();
        for (CompletableFuture<List<Place>> future : futures) {
            if (fallbackPlaces.size() >= limit) break;
            try {
                for (Place p : future.join()) {
                    if (fallbackPlaces.size() >= limit) break;
                    if (seenIds.add(p.getId())) fallbackPlaces.add(p);
                }
            } catch (Exception e) {
                logger.debug("MBTI fallback search failed: {}", e.getMessage());
            }
        }

        if (!fallbackPlaces.isEmpty()) {
            row.put("source", "keyword");
            row.put("places", placesToSimpleDtos(
                    fallbackPlaces.stream().limit(limit).toList(), lat, lon));
            return row;
        }

        return null;
    }

    private Map<String, Object> categoryToMap(PlaceCategory category, TimeSlot timeSlot) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", category.getKey());
        row.put("displayName", category.getDisplayName());
        row.put("description", category.getDescription());
        row.put("emoji", category.getEmoji());
        row.put("displayTitle", getCategoryTitle(category.getKey(), timeSlot));
        return row;
    }

    /**
     * 시간대 + 카테고리 키에 맞는 사용자 표시 타이틀 반환
     *
     * @param categoryKey 카테고리 키 (예: cafe, bar)
     * @param timeSlot 시간대
     * @return Korean display title (없으면 카테고리 description으로 fallback)
     */
    private String getCategoryTitle(String categoryKey, TimeSlot timeSlot) {
        if (categoryKey == null || timeSlot == null) {
            return null;
        }
        Map<String, String> titlesForSlot = TIME_CATEGORY_TITLES.get(timeSlot);
        if (titlesForSlot != null) {
            String title = titlesForSlot.get(categoryKey);
            if (title != null) {
                return title;
            }
        }
        // Fallback: category description (시간대 무관한 기본 문구)
        PlaceCategory cat = PlaceCategory.fromKey(categoryKey);
        return cat != null ? cat.getDescription() : null;
    }

    /**
     * 장소 리스트 → DTO 리스트 (이미지 배치 로드, N+1 방지)
     */
    private List<Map<String, Object>> placesToSimpleDtos(List<Place> places, Double userLat, Double userLon) {
        if (places.isEmpty()) return List.of();

        // 이미지 배치 조회 (1 쿼리)
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        Map<Long, String> imageMap = new HashMap<>();
        try {
            List<Object[]> rows = placeImageRepository.findFirstImagesByPlaceIds(placeIds);
            for (Object[] row : rows) {
                imageMap.put(((Number) row[0]).longValue(), (String) row[1]);
            }
        } catch (Exception e) {
            logger.warn("Batch image load failed: {}", e.getMessage());
        }

        return places.stream().map(place -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", place.getId());
            dto.put("name", place.getName());
            dto.put("rating", place.getRating());
            dto.put("roadAddress", place.getRoadAddress());

            if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                String cat = place.getCategory().stream()
                        .filter(c -> !c.equalsIgnoreCase("음식점") && !c.equalsIgnoreCase("restaurant"))
                        .findFirst().orElse(place.getCategory().get(0));
                dto.put("category", cat);
            }

            dto.put("imageUrl", imageMap.get(place.getId()));

            if (place.getLatitude() != null && place.getLongitude() != null && userLat != null && userLon != null) {
                double dist = 6371 * Math.acos(Math.min(1.0, Math.max(-1.0,
                        Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(place.getLatitude().doubleValue())) *
                        Math.cos(Math.toRadians(place.getLongitude().doubleValue()) - Math.toRadians(userLon)) +
                        Math.sin(Math.toRadians(userLat)) * Math.sin(Math.toRadians(place.getLatitude().doubleValue()))
                )));
                dto.put("distance", Math.round(dist * 10) / 10.0);
            }

            return dto;
        }).toList();
    }

    /**
     * 날씨 정보 가져오기
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 날씨 상태
     */
    private WeatherCondition getWeatherCondition(Double latitude, Double longitude) {
        try {
            WeatherData weatherData = weatherService.getCurrentWeather(latitude, longitude);
            if (weatherData != null && weatherData.getConditionText() != null) {
                return WeatherCondition.fromText(weatherData.getConditionText());
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch weather data: {}", e.getMessage());
        }

        // 날씨 정보를 가져오지 못한 경우 기본값
        return WeatherCondition.SUNNY;
    }

    /**
     * PlaceCategory를 CategoryDto로 변환
     *
     * @param category PlaceCategory
     * @return CategoryDto
     */
    private CategoryDto toCategoryDto(PlaceCategory category) {
        return new CategoryDto(
                category.getKey(),
                category.getDisplayName(),
                category.getDescription(),
                category.getEmoji()
        );
    }
}

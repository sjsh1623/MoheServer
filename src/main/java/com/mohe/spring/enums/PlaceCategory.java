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
                     Arrays.asList("심야", "식당", "24시", "night restaurant"), "🍜"),

    // ============================================
    // 음식 세부 카테고리
    // ============================================
    JAPANESE_FOOD("일식", "japanese_food", "일본의 정통 맛을",
                  Arrays.asList("일식", "초밥", "라멘", "돈카츠", "우동", "japanese", "sushi", "ramen"), "🍣"),

    CHINESE_FOOD("중식", "chinese_food", "중국의 풍미를",
                 Arrays.asList("중식", "중국집", "짜장", "짬뽕", "마라", "chinese"), "🥟"),

    WESTERN_FOOD("양식", "western_food", "서양의 맛을",
                 Arrays.asList("양식", "스테이크", "파스타", "버거", "western"), "🥩"),

    ASIAN_FOOD("아시안", "asian_food", "이국적인 맛을",
               Arrays.asList("아시안", "태국", "베트남", "쌀국수", "커리", "asian"), "🍜"),

    FRENCH("프렌치", "french", "프랑스의 맛을",
           Arrays.asList("프렌치", "프랑스", "비스트로", "french"), "🥐"),

    CHICKEN("치킨", "chicken", "바삭한 치킨",
            Arrays.asList("치킨", "통닭", "후라이드", "양념치킨", "chicken"), "🍗"),

    SEAFOOD("해산물", "seafood", "신선한 해산물",
            Arrays.asList("해산물", "횟집", "회", "조개", "seafood"), "🦐"),

    MEAT("고기", "meat", "고기 구워 먹는 즐거움",
         Arrays.asList("고기", "삼겹살", "소고기", "구이", "meat", "bbq"), "🥓"),

    NOODLE("면요리", "noodle", "면 요리의 세계",
           Arrays.asList("면", "국수", "칼국수", "냉면", "noodle"), "🍝"),

    STEW("탕/찌개", "stew", "뜨끈한 찌개 한 그릇",
         Arrays.asList("찌개", "탕", "김치찌개", "된장찌개", "순두부", "stew"), "🍲"),

    SNACK_BAR("분식", "snack_bar", "분식의 추억",
              Arrays.asList("분식", "떡볶이", "김밥", "라볶이", "튀김"), "🍢"),

    PORRIDGE("죽/국밥", "porridge", "속이 편한 한 끼",
             Arrays.asList("죽", "국밥", "해장", "porridge"), "🥣"),

    SALAD("샐러드", "salad", "건강한 한 끼",
          Arrays.asList("샐러드", "비건", "채소", "salad"), "🥗"),

    JOKBAL("족발/보쌈", "jokbal", "야식의 제왕",
           Arrays.asList("족발", "보쌈", "jokbal"), "🐷"),

    GOPCHANG("곱창", "gopchang", "곱창의 유혹",
             Arrays.asList("곱창", "막창", "대창", "gopchang"), "🔥"),

    SHABU("샤브샤브", "shabu", "따끈한 샤브샤브",
          Arrays.asList("샤브샤브", "훠궈", "shabu"), "♨️"),

    BUFFET("뷔페", "buffet", "마음껏 먹는 즐거움",
           Arrays.asList("뷔페", "뷔페", "buffet"), "🍱"),

    SUSHI("스시", "sushi", "신선한 스시",
          Arrays.asList("스시", "초밥", "오마카세", "sushi"), "🍣"),

    RAMEN("라멘", "ramen", "진한 라멘 한 그릇",
          Arrays.asList("라멘", "ramen"), "🍜"),

    PIZZA("피자", "pizza", "치즈가 쭉 늘어나는",
          Arrays.asList("피자", "pizza"), "🍕"),

    BURGER("버거", "burger", "육즙 가득 버거",
           Arrays.asList("버거", "햄버거", "burger"), "🍔"),

    SANDWICH("샌드위치", "sandwich", "간단하게 한 끼",
             Arrays.asList("샌드위치", "sandwich"), "🥪"),

    TOAST("토스트", "toast", "바삭한 토스트",
          Arrays.asList("토스트", "toast"), "🍞"),

    OMURICE("오므라이스", "omurice", "폭신한 오므라이스",
            Arrays.asList("오므라이스", "omurice"), "🍳"),

    CURRY("카레", "curry", "진한 카레 한 접시",
          Arrays.asList("카레", "curry"), "🍛"),

    DUMPLING("만두", "dumpling", "만두 한 접시",
             Arrays.asList("만두", "교자", "dumpling"), "🥟"),

    HOTDOG("핫도그", "hotdog", "길거리 핫도그",
           Arrays.asList("핫도그", "hotdog"), "🌭"),

    RICE_CAKE("떡", "rice_cake", "쫀득한 떡",
              Arrays.asList("떡", "떡카페", "rice cake"), "🍡"),

    SUNDAE("순대", "sundae", "순대 한 접시",
           Arrays.asList("순대", "sundae"), "🌭"),

    GRILLED_FISH("생선구이", "grilled_fish", "생선 구이 한 상",
                 Arrays.asList("생선구이", "고등어", "grilled fish"), "🐟"),

    EEL("장어", "eel", "보양식 장어",
        Arrays.asList("장어", "민물장어", "eel"), "🐍"),

    CHICKEN_DISH("닭요리", "chicken_dish", "다양한 닭 요리",
                 Arrays.asList("닭갈비", "닭볶음탕", "찜닭"), "🐔"),

    DUCK("오리", "duck", "오리 요리 한 상",
         Arrays.asList("오리", "훈제오리", "duck"), "🦆"),

    LAMB_SKEWER("양꼬치", "lamb_skewer", "불향 가득 양꼬치",
                Arrays.asList("양꼬치", "양고기", "lamb"), "🍢"),

    BEEF_TRIPE("대창", "beef_tripe", "대창의 매력",
               Arrays.asList("대창", "beef tripe"), "🔥"),

    TAKOYAKI("타코야키", "takoyaki", "동글동글 타코야키",
             Arrays.asList("타코야키", "takoyaki"), "🐙"),

    YAKITORI("야키토리", "yakitori", "꼬치 한 잔",
             Arrays.asList("야키토리", "꼬치", "yakitori"), "🍢"),

    LATE_NIGHT_FOOD("야식", "late_night_food", "밤에 먹는 야식",
                    Arrays.asList("야식", "심야", "late night food"), "🌙"),

    MEXICAN("멕시칸", "mexican", "타코 파티",
            Arrays.asList("멕시칸", "타코", "부리또", "mexican"), "🌮"),

    MEDITERRANEAN("지중해", "mediterranean", "지중해의 맛",
                  Arrays.asList("지중해", "mediterranean"), "🫒"),

    // ============================================
    // 음료/디저트 세부
    // ============================================
    TEA("차", "tea", "따뜻한 차 한 잔",
        Arrays.asList("차", "전통차", "tea"), "🍵"),

    JUICE("주스", "juice", "신선한 주스",
          Arrays.asList("주스", "스무디", "juice"), "🧃"),

    BINGSU("빙수", "bingsu", "시원한 빙수",
           Arrays.asList("빙수", "팥빙수", "bingsu"), "🍧"),

    WAFFLE("와플", "waffle", "바삭한 와플",
           Arrays.asList("와플", "waffle"), "🧇"),

    DONUT("도넛", "donut", "달콤한 도넛",
          Arrays.asList("도넛", "donut"), "🍩"),

    TART("타르트", "tart", "과일 타르트",
         Arrays.asList("타르트", "tart"), "🥧"),

    CHOCOLATE("초콜릿", "chocolate", "진한 초콜릿",
              Arrays.asList("초콜릿", "chocolate"), "🍫"),

    COOKIE("쿠키", "cookie", "바삭한 쿠키",
           Arrays.asList("쿠키", "cookie"), "🍪"),

    SCONE("스콘", "scone", "갓 구운 스콘",
          Arrays.asList("스콘", "scone"), "🧁"),

    // ============================================
    // 주류 세부
    // ============================================
    IZAKAYA("이자카야", "izakaya", "일본식 선술집",
            Arrays.asList("이자카야", "사케", "izakaya"), "🏮"),

    HOF("호프", "hof", "호프집에서 한 잔",
        Arrays.asList("호프", "생맥주", "hof"), "🍺"),

    MAKGEOLLI("막걸리", "makgeolli", "전통 막걸리 한 잔",
              Arrays.asList("막걸리", "전통주", "makgeolli"), "🍶"),

    LOUNGE_BAR("라운지바", "lounge_bar", "고급스러운 라운지",
               Arrays.asList("라운지", "라운지바", "lounge"), "🥂"),

    INDOOR_POCHA("실내포차", "indoor_pocha", "실내 포차에서 한 잔",
                 Arrays.asList("포차", "실내포차", "indoor pocha"), "🏮"),

    LIVE_BAR("라이브바", "live_bar", "라이브 바에서",
             Arrays.asList("라이브바", "라이브", "live bar"), "🎸"),

    CLUB("클럽", "club", "클럽에서 즐기는 밤",
         Arrays.asList("클럽", "나이트", "club"), "💃"),

    ROOFTOP("루프탑", "rooftop", "루프탑에서의 한 잔",
            Arrays.asList("루프탑", "옥상", "rooftop"), "🌆"),

    // ============================================
    // 뷰티/웰니스
    // ============================================
    SPA("스파", "spa", "힐링 스파",
        Arrays.asList("스파", "마사지", "spa"), "💆"),

    FITNESS("피트니스", "fitness", "운동으로 활력을",
            Arrays.asList("피트니스", "헬스", "운동", "fitness"), "💪"),

    YOGA("요가", "yoga", "요가로 힐링",
         Arrays.asList("요가", "필라테스", "yoga"), "🧘"),

    NAIL("네일", "nail", "네일아트",
         Arrays.asList("네일", "네일아트", "nail"), "💅"),

    HAIR("헤어", "hair", "헤어살롱",
         Arrays.asList("헤어", "미용실", "hair"), "💇"),

    SKINCARE("스킨케어", "skincare", "피부 관리",
             Arrays.asList("스킨케어", "피부관리", "skincare"), "✨"),

    SAUNA("사우나", "sauna", "사우나에서 힐링",
          Arrays.asList("사우나", "찜질방", "sauna"), "♨️"),

    // ============================================
    // 특색 카페
    // ============================================
    FLOWER_CAFE("플라워카페", "flower_cafe", "꽃과 함께하는 시간",
                Arrays.asList("플라워", "꽃", "flower cafe"), "🌸"),

    LARGE_CAFE("대형카페", "large_cafe", "넓은 카페",
               Arrays.asList("대형카페", "넓은", "large cafe"), "🏢"),

    STUDY_CAFE("스터디카페", "study_cafe", "공부하기 좋은 카페",
               Arrays.asList("스터디카페", "공부", "study cafe"), "📝"),

    MANGA_CAFE("만화카페", "manga_cafe", "만화카페에서 힐링",
               Arrays.asList("만화카페", "만화방", "manga cafe"), "📖"),

    // ============================================
    // 놀거리/엔터테인먼트 세부
    // ============================================
    KARAOKE("노래방", "karaoke", "노래 부르기",
            Arrays.asList("노래방", "코인노래방", "karaoke"), "🎤"),

    BOARD_GAME("보드게임", "board_game", "보드게임 카페",
               Arrays.asList("보드게임", "board game"), "🎲"),

    ESCAPE_ROOM("방탈출", "escape_room", "방탈출 카페",
                Arrays.asList("방탈출", "escape room"), "🔐"),

    BOWLING("볼링", "bowling", "볼링장",
            Arrays.asList("볼링", "bowling"), "🎳"),

    BILLIARDS("당구", "billiards", "당구장",
              Arrays.asList("당구", "billiards"), "🎱"),

    GOLF("골프", "golf", "골프 치기",
         Arrays.asList("골프", "스크린골프", "golf"), "⛳"),

    SWIMMING("수영", "swimming", "수영장",
             Arrays.asList("수영", "수영장", "swimming"), "🏊"),

    TENNIS("테니스", "tennis", "테니스 치기",
           Arrays.asList("테니스", "tennis"), "🎾"),

    PC_ROOM("PC방", "pc_room", "PC방에서 게임",
            Arrays.asList("PC방", "피씨방", "pc room"), "🖥️"),

    VR("VR", "vr", "VR 체험",
       Arrays.asList("VR", "가상현실", "vr"), "🥽"),

    PHOTO_BOOTH("포토부스", "photo_booth", "인생네컷",
                Arrays.asList("포토부스", "인생네컷", "photo booth"), "📸"),

    PHOTO_STUDIO("사진스튜디오", "photo_studio", "사진 찍기 좋은 곳",
                 Arrays.asList("사진", "포토존", "photo studio"), "📷"),

    // ============================================
    // 야외/여행
    // ============================================
    CAMPING("캠핑", "camping", "캠핑의 낭만",
            Arrays.asList("캠핑", "글램핑", "camping"), "⛺"),

    PENSION("펜션", "pension", "펜션에서 쉬기",
            Arrays.asList("펜션", "pension"), "🏡"),

    HOTEL("호텔", "hotel", "호텔 스테이",
          Arrays.asList("호텔", "hotel"), "🏨"),

    AMUSEMENT_PARK("놀이공원", "amusement_park", "놀이공원에서 신나게",
                   Arrays.asList("놀이공원", "테마파크", "amusement park"), "🎢"),

    ZOO("동물원", "zoo", "동물원 나들이",
        Arrays.asList("동물원", "zoo"), "🦁"),

    AQUARIUM("아쿠아리움", "aquarium", "수족관 구경",
             Arrays.asList("아쿠아리움", "수족관", "aquarium"), "🐠"),

    BOTANICAL_GARDEN("식물원", "botanical_garden", "식물원 산책",
                     Arrays.asList("식물원", "botanical garden"), "🌿"),

    HIKING("등산", "hiking", "등산으로 건강하게",
           Arrays.asList("등산", "산", "hiking"), "🏔️"),

    DRIVE("드라이브", "drive", "드라이브 코스",
          Arrays.asList("드라이브", "drive"), "🚗"),

    // ============================================
    // 쇼핑 세부
    // ============================================
    TRADITIONAL_MARKET("전통시장", "traditional_market", "전통시장 구경",
                       Arrays.asList("전통시장", "시장", "traditional market"), "🏪"),

    DEPARTMENT("백화점", "department", "백화점 쇼핑",
               Arrays.asList("백화점", "department"), "🏬"),

    VINTAGE("빈티지", "vintage", "빈티지 아이템 찾기",
            Arrays.asList("빈티지", "중고", "vintage"), "👗"),

    FLEA_MARKET("플리마켓", "flea_market", "플리마켓 구경",
                Arrays.asList("플리마켓", "벼룩시장", "flea market"), "🛒"),

    // ============================================
    // 분위기/상황
    // ============================================
    VIEW("뷰맛집", "view", "뷰가 좋은 곳",
         Arrays.asList("뷰", "전망", "야경", "view"), "🌇"),

    TERRACE("테라스", "terrace", "야외 테라스",
            Arrays.asList("테라스", "야외", "terrace"), "☀️"),

    HANOK("한옥", "hanok", "한옥 분위기",
          Arrays.asList("한옥", "전통", "hanok"), "🏯"),

    RETRO("레트로", "retro", "레트로 감성",
          Arrays.asList("레트로", "복고", "뉴트로", "retro"), "📻"),

    INSTAGRAMMABLE("인스타감성", "instagrammable", "인스타 감성 가득",
                   Arrays.asList("인스타", "감성", "핫플", "instagrammable"), "📱"),

    PRIVATE("프라이빗", "private", "프라이빗 공간",
            Arrays.asList("룸", "프라이빗", "단체", "private"), "🔒"),

    LATE_NIGHT("심야", "late_night", "밤늦게 열린 곳",
               Arrays.asList("심야", "야간", "늦은밤", "late night"), "🌙"),

    // ============================================
    // 대상별
    // ============================================
    PET_FRIENDLY("펫프렌들리", "pet_friendly", "반려동물과 함께",
                 Arrays.asList("반려동물", "펫", "강아지", "고양이", "pet friendly"), "🐾"),

    KIDS_FRIENDLY("키즈프렌들리", "kids_friendly", "아이와 함께",
                  Arrays.asList("키즈", "아이", "가족", "kids friendly"), "👶"),

    SOLO_DINING("혼밥", "solo_dining", "혼자 먹기 좋은 곳",
                Arrays.asList("혼밥", "혼자", "1인", "solo"), "🍽️"),

    DATE_SPOT("데이트", "date_spot", "데이트 코스",
              Arrays.asList("데이트", "커플", "date"), "❤️"),

    GROUP_DINING("단체식사", "group_dining", "단체 모임",
                 Arrays.asList("단체", "모임", "회식", "group"), "👥"),

    BUSINESS_DINING("비즈니스", "business_dining", "비즈니스 미팅",
                    Arrays.asList("비즈니스", "접대", "business"), "💼"),

    // ============================================
    // 기타
    // ============================================
    NEW_PLACE("신상", "new_place", "새로 생긴 곳",
              Arrays.asList("신상", "오픈", "new"), "🆕"),

    VALUE("가성비", "value", "가성비 좋은 곳",
          Arrays.asList("가성비", "저렴", "value"), "💰"),

    POJANGMACHA("포장마차", "pojangmacha", "추억의 포장마차",
                Arrays.asList("포장마차", "노포", "pojangmacha"), "🏮"),

    COWORKING("코워킹", "coworking", "코워킹 스페이스",
              Arrays.asList("코워킹", "작업", "coworking"), "💻"),

    VLOG_SPOT("브이로그", "vlog_spot", "브이로그 찍기 좋은 곳",
              Arrays.asList("브이로그", "유튜브", "vlog"), "🎥"),

    WAITING_SPOT("대기장소", "waiting_spot", "기다리기 좋은 곳",
                 Arrays.asList("대기", "기다림", "waiting"), "⏰");

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

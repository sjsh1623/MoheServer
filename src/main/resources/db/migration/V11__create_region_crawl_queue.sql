-- V11: 지역 크롤링 우선순위 큐
-- 사용자 위치 기반 우선순위 + 전국 자동 순환 관리

CREATE TABLE IF NOT EXISTS region_crawl_queue (
    id              BIGSERIAL PRIMARY KEY,
    sido_code       VARCHAR(10) NOT NULL,      -- Tour API 기준 시도 코드
    sigungu_code    VARCHAR(10),               -- Tour API 기준 시군구 코드 (NULL이면 시도 전체)
    sido_name       VARCHAR(50),
    sigungu_name    VARCHAR(50),
    priority        INTEGER NOT NULL DEFAULT 5, -- 1=사용자 요청(최우선), 3=재수집, 5=자동순환
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED
    source          VARCHAR(30) NOT NULL DEFAULT 'scheduled', -- user_trigger, scheduled, rescan
    user_lat        DOUBLE PRECISION,           -- 요청 사용자 위치 (user_trigger용)
    user_lng        DOUBLE PRECISION,
    requested_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    last_collected_at TIMESTAMP,               -- 마지막 수집 완료 시간
    error_message   TEXT,
    success_count   INTEGER DEFAULT 0,
    duplicate_count INTEGER DEFAULT 0,
    error_count     INTEGER DEFAULT 0,
    UNIQUE (sido_code, sigungu_code)           -- 동일 지역 중복 방지
);

CREATE INDEX IF NOT EXISTS idx_rcq_status_priority ON region_crawl_queue(status, priority, requested_at);
CREATE INDEX IF NOT EXISTS idx_rcq_sido ON region_crawl_queue(sido_code);
CREATE INDEX IF NOT EXISTS idx_rcq_last_collected ON region_crawl_queue(last_collected_at);

-- 전국 시군구 초기 데이터 삽입 (전국 자동 순환용)
-- Tour API 기준 sido_code (1=서울, 2=인천, ..., 39=제주)
INSERT INTO region_crawl_queue (sido_code, sigungu_code, sido_name, sigungu_name, priority, source)
VALUES
-- 서울특별시 (25구)
('1', '1', '서울', '강남구', 5, 'scheduled'), ('1', '2', '서울', '강동구', 5, 'scheduled'),
('1', '3', '서울', '강북구', 5, 'scheduled'), ('1', '4', '서울', '강서구', 5, 'scheduled'),
('1', '5', '서울', '관악구', 5, 'scheduled'), ('1', '6', '서울', '광진구', 5, 'scheduled'),
('1', '7', '서울', '구로구', 5, 'scheduled'), ('1', '8', '서울', '금천구', 5, 'scheduled'),
('1', '9', '서울', '노원구', 5, 'scheduled'), ('1', '10', '서울', '도봉구', 5, 'scheduled'),
('1', '11', '서울', '동대문구', 5, 'scheduled'), ('1', '12', '서울', '동작구', 5, 'scheduled'),
('1', '13', '서울', '마포구', 5, 'scheduled'), ('1', '14', '서울', '서대문구', 5, 'scheduled'),
('1', '15', '서울', '서초구', 5, 'scheduled'), ('1', '16', '서울', '성동구', 5, 'scheduled'),
('1', '17', '서울', '성북구', 5, 'scheduled'), ('1', '18', '서울', '송파구', 5, 'scheduled'),
('1', '19', '서울', '양천구', 5, 'scheduled'), ('1', '20', '서울', '영등포구', 5, 'scheduled'),
('1', '21', '서울', '용산구', 5, 'scheduled'), ('1', '22', '서울', '은평구', 5, 'scheduled'),
('1', '23', '서울', '종로구', 5, 'scheduled'), ('1', '24', '서울', '중구', 5, 'scheduled'),
('1', '25', '서울', '중랑구', 5, 'scheduled'),
-- 인천광역시
('2', '1', '인천', '강화군', 5, 'scheduled'), ('2', '2', '인천', '계양구', 5, 'scheduled'),
('2', '3', '인천', '남동구', 5, 'scheduled'), ('2', '4', '인천', '동구', 5, 'scheduled'),
('2', '5', '인천', '미추홀구', 5, 'scheduled'), ('2', '6', '인천', '부평구', 5, 'scheduled'),
('2', '7', '인천', '서구', 5, 'scheduled'), ('2', '8', '인천', '연수구', 5, 'scheduled'),
('2', '9', '인천', '옹진군', 5, 'scheduled'), ('2', '10', '인천', '중구', 5, 'scheduled'),
-- 대전광역시
('3', '1', '대전', '대덕구', 5, 'scheduled'), ('3', '2', '대전', '동구', 5, 'scheduled'),
('3', '3', '대전', '서구', 5, 'scheduled'), ('3', '4', '대전', '유성구', 5, 'scheduled'),
('3', '5', '대전', '중구', 5, 'scheduled'),
-- 대구광역시
('4', '1', '대구', '군위군', 5, 'scheduled'), ('4', '2', '대구', '남구', 5, 'scheduled'),
('4', '3', '대구', '달서구', 5, 'scheduled'), ('4', '4', '대구', '달성군', 5, 'scheduled'),
('4', '5', '대구', '동구', 5, 'scheduled'), ('4', '6', '대구', '북구', 5, 'scheduled'),
('4', '7', '대구', '서구', 5, 'scheduled'), ('4', '8', '대구', '수성구', 5, 'scheduled'),
('4', '9', '대구', '중구', 5, 'scheduled'),
-- 광주광역시
('5', '1', '광주', '광산구', 5, 'scheduled'), ('5', '2', '광주', '남구', 5, 'scheduled'),
('5', '3', '광주', '동구', 5, 'scheduled'), ('5', '4', '광주', '북구', 5, 'scheduled'),
('5', '5', '광주', '서구', 5, 'scheduled'),
-- 부산광역시
('6', '1', '부산', '강서구', 5, 'scheduled'), ('6', '2', '부산', '금정구', 5, 'scheduled'),
('6', '3', '부산', '기장군', 5, 'scheduled'), ('6', '4', '부산', '남구', 5, 'scheduled'),
('6', '5', '부산', '동구', 5, 'scheduled'), ('6', '6', '부산', '동래구', 5, 'scheduled'),
('6', '7', '부산', '부산진구', 5, 'scheduled'), ('6', '8', '부산', '북구', 5, 'scheduled'),
('6', '9', '부산', '사상구', 5, 'scheduled'), ('6', '10', '부산', '사하구', 5, 'scheduled'),
('6', '11', '부산', '서구', 5, 'scheduled'), ('6', '12', '부산', '수영구', 5, 'scheduled'),
('6', '13', '부산', '연제구', 5, 'scheduled'), ('6', '14', '부산', '영도구', 5, 'scheduled'),
('6', '15', '부산', '중구', 5, 'scheduled'), ('6', '16', '부산', '해운대구', 5, 'scheduled'),
-- 울산광역시
('7', '1', '울산', '남구', 5, 'scheduled'), ('7', '2', '울산', '동구', 5, 'scheduled'),
('7', '3', '울산', '북구', 5, 'scheduled'), ('7', '4', '울산', '울주군', 5, 'scheduled'),
('7', '5', '울산', '중구', 5, 'scheduled'),
-- 세종특별자치시
('8', NULL, '세종', '세종시', 5, 'scheduled'),
-- 경기도
('31', '1', '경기', '가평군', 5, 'scheduled'), ('31', '2', '경기', '고양시', 5, 'scheduled'),
('31', '3', '경기', '과천시', 5, 'scheduled'), ('31', '4', '경기', '광명시', 5, 'scheduled'),
('31', '5', '경기', '광주시', 5, 'scheduled'), ('31', '6', '경기', '구리시', 5, 'scheduled'),
('31', '7', '경기', '군포시', 5, 'scheduled'), ('31', '8', '경기', '김포시', 5, 'scheduled'),
('31', '9', '경기', '남양주시', 5, 'scheduled'), ('31', '10', '경기', '동두천시', 5, 'scheduled'),
('31', '11', '경기', '부천시', 5, 'scheduled'), ('31', '12', '경기', '성남시', 5, 'scheduled'),
('31', '13', '경기', '수원시', 5, 'scheduled'), ('31', '14', '경기', '시흥시', 5, 'scheduled'),
('31', '15', '경기', '안산시', 5, 'scheduled'), ('31', '16', '경기', '안성시', 5, 'scheduled'),
('31', '17', '경기', '안양시', 5, 'scheduled'), ('31', '18', '경기', '양주시', 5, 'scheduled'),
('31', '19', '경기', '양평군', 5, 'scheduled'), ('31', '20', '경기', '여주시', 5, 'scheduled'),
('31', '21', '경기', '연천군', 5, 'scheduled'), ('31', '22', '경기', '오산시', 5, 'scheduled'),
('31', '23', '경기', '용인시', 5, 'scheduled'), ('31', '24', '경기', '의왕시', 5, 'scheduled'),
('31', '25', '경기', '의정부시', 5, 'scheduled'), ('31', '26', '경기', '이천시', 5, 'scheduled'),
('31', '27', '경기', '파주시', 5, 'scheduled'), ('31', '28', '경기', '평택시', 5, 'scheduled'),
('31', '29', '경기', '포천시', 5, 'scheduled'), ('31', '30', '경기', '하남시', 5, 'scheduled'),
('31', '31', '경기', '화성시', 5, 'scheduled'),
-- 강원도
('32', '1', '강원', '강릉시', 5, 'scheduled'), ('32', '2', '강원', '고성군', 5, 'scheduled'),
('32', '3', '강원', '동해시', 5, 'scheduled'), ('32', '4', '강원', '삼척시', 5, 'scheduled'),
('32', '5', '강원', '속초시', 5, 'scheduled'), ('32', '6', '강원', '양구군', 5, 'scheduled'),
('32', '7', '강원', '양양군', 5, 'scheduled'), ('32', '8', '강원', '영월군', 5, 'scheduled'),
('32', '9', '강원', '원주시', 5, 'scheduled'), ('32', '10', '강원', '인제군', 5, 'scheduled'),
('32', '11', '강원', '정선군', 5, 'scheduled'), ('32', '12', '강원', '철원군', 5, 'scheduled'),
('32', '13', '강원', '춘천시', 5, 'scheduled'), ('32', '14', '강원', '태백시', 5, 'scheduled'),
('32', '15', '강원', '평창군', 5, 'scheduled'), ('32', '16', '강원', '홍천군', 5, 'scheduled'),
('32', '17', '강원', '화천군', 5, 'scheduled'), ('32', '18', '강원', '횡성군', 5, 'scheduled'),
-- 충청북도
('33', '1', '충북', '괴산군', 5, 'scheduled'), ('33', '2', '충북', '단양군', 5, 'scheduled'),
('33', '3', '충북', '보은군', 5, 'scheduled'), ('33', '4', '충북', '영동군', 5, 'scheduled'),
('33', '5', '충북', '옥천군', 5, 'scheduled'), ('33', '6', '충북', '음성군', 5, 'scheduled'),
('33', '7', '충북', '제천시', 5, 'scheduled'), ('33', '8', '충북', '증평군', 5, 'scheduled'),
('33', '9', '충북', '진천군', 5, 'scheduled'), ('33', '10', '충북', '청주시', 5, 'scheduled'),
('33', '11', '충북', '충주시', 5, 'scheduled'),
-- 충청남도
('34', '1', '충남', '계룡시', 5, 'scheduled'), ('34', '2', '충남', '공주시', 5, 'scheduled'),
('34', '3', '충남', '금산군', 5, 'scheduled'), ('34', '4', '충남', '논산시', 5, 'scheduled'),
('34', '5', '충남', '당진시', 5, 'scheduled'), ('34', '6', '충남', '보령시', 5, 'scheduled'),
('34', '7', '충남', '부여군', 5, 'scheduled'), ('34', '8', '충남', '서산시', 5, 'scheduled'),
('34', '9', '충남', '서천군', 5, 'scheduled'), ('34', '10', '충남', '아산시', 5, 'scheduled'),
('34', '11', '충남', '예산군', 5, 'scheduled'), ('34', '12', '충남', '천안시', 5, 'scheduled'),
('34', '13', '충남', '청양군', 5, 'scheduled'), ('34', '14', '충남', '태안군', 5, 'scheduled'),
('34', '15', '충남', '홍성군', 5, 'scheduled'),
-- 경상북도
('35', '1', '경북', '경산시', 5, 'scheduled'), ('35', '2', '경북', '경주시', 5, 'scheduled'),
('35', '3', '경북', '고령군', 5, 'scheduled'), ('35', '4', '경북', '구미시', 5, 'scheduled'),
('35', '5', '경북', '김천시', 5, 'scheduled'), ('35', '6', '경북', '문경시', 5, 'scheduled'),
('35', '7', '경북', '봉화군', 5, 'scheduled'), ('35', '8', '경북', '상주시', 5, 'scheduled'),
('35', '9', '경북', '성주군', 5, 'scheduled'), ('35', '10', '경북', '안동시', 5, 'scheduled'),
('35', '11', '경북', '영덕군', 5, 'scheduled'), ('35', '12', '경북', '영양군', 5, 'scheduled'),
('35', '13', '경북', '영주시', 5, 'scheduled'), ('35', '14', '경북', '영천시', 5, 'scheduled'),
('35', '15', '경북', '예천군', 5, 'scheduled'), ('35', '16', '경북', '울릉군', 5, 'scheduled'),
('35', '17', '경북', '울진군', 5, 'scheduled'), ('35', '18', '경북', '의성군', 5, 'scheduled'),
('35', '19', '경북', '청도군', 5, 'scheduled'), ('35', '20', '경북', '청송군', 5, 'scheduled'),
('35', '21', '경북', '칠곡군', 5, 'scheduled'), ('35', '22', '경북', '포항시', 5, 'scheduled'),
-- 경상남도
('36', '1', '경남', '거제시', 5, 'scheduled'), ('36', '2', '경남', '거창군', 5, 'scheduled'),
('36', '3', '경남', '고성군', 5, 'scheduled'), ('36', '4', '경남', '김해시', 5, 'scheduled'),
('36', '5', '경남', '남해군', 5, 'scheduled'), ('36', '6', '경남', '밀양시', 5, 'scheduled'),
('36', '7', '경남', '사천시', 5, 'scheduled'), ('36', '8', '경남', '산청군', 5, 'scheduled'),
('36', '9', '경남', '양산시', 5, 'scheduled'), ('36', '10', '경남', '의령군', 5, 'scheduled'),
('36', '11', '경남', '진주시', 5, 'scheduled'), ('36', '12', '경남', '창녕군', 5, 'scheduled'),
('36', '13', '경남', '창원시', 5, 'scheduled'), ('36', '14', '경남', '통영시', 5, 'scheduled'),
('36', '15', '경남', '하동군', 5, 'scheduled'), ('36', '16', '경남', '함안군', 5, 'scheduled'),
('36', '17', '경남', '함양군', 5, 'scheduled'), ('36', '18', '경남', '합천군', 5, 'scheduled'),
-- 전라북도
('37', '1', '전북', '고창군', 5, 'scheduled'), ('37', '2', '전북', '군산시', 5, 'scheduled'),
('37', '3', '전북', '김제시', 5, 'scheduled'), ('37', '4', '전북', '남원시', 5, 'scheduled'),
('37', '5', '전북', '무주군', 5, 'scheduled'), ('37', '6', '전북', '부안군', 5, 'scheduled'),
('37', '7', '전북', '순창군', 5, 'scheduled'), ('37', '8', '전북', '완주군', 5, 'scheduled'),
('37', '9', '전북', '익산시', 5, 'scheduled'), ('37', '10', '전북', '임실군', 5, 'scheduled'),
('37', '11', '전북', '장수군', 5, 'scheduled'), ('37', '12', '전북', '전주시', 5, 'scheduled'),
('37', '13', '전북', '정읍시', 5, 'scheduled'), ('37', '14', '전북', '진안군', 5, 'scheduled'),
-- 전라남도
('38', '1', '전남', '강진군', 5, 'scheduled'), ('38', '2', '전남', '고흥군', 5, 'scheduled'),
('38', '3', '전남', '곡성군', 5, 'scheduled'), ('38', '4', '전남', '광양시', 5, 'scheduled'),
('38', '5', '전남', '구례군', 5, 'scheduled'), ('38', '6', '전남', '나주시', 5, 'scheduled'),
('38', '7', '전남', '담양군', 5, 'scheduled'), ('38', '8', '전남', '목포시', 5, 'scheduled'),
('38', '9', '전남', '무안군', 5, 'scheduled'), ('38', '10', '전남', '보성군', 5, 'scheduled'),
('38', '11', '전남', '순천시', 5, 'scheduled'), ('38', '12', '전남', '신안군', 5, 'scheduled'),
('38', '13', '전남', '여수시', 5, 'scheduled'), ('38', '14', '전남', '영광군', 5, 'scheduled'),
('38', '15', '전남', '영암군', 5, 'scheduled'), ('38', '16', '전남', '완도군', 5, 'scheduled'),
('38', '17', '전남', '장성군', 5, 'scheduled'), ('38', '18', '전남', '장흥군', 5, 'scheduled'),
('38', '19', '전남', '진도군', 5, 'scheduled'), ('38', '20', '전남', '함평군', 5, 'scheduled'),
('38', '21', '전남', '해남군', 5, 'scheduled'), ('38', '22', '전남', '화순군', 5, 'scheduled'),
-- 제주도
('39', '1', '제주', '서귀포시', 5, 'scheduled'), ('39', '2', '제주', '제주시', 5, 'scheduled')
ON CONFLICT (sido_code, sigungu_code) DO NOTHING;

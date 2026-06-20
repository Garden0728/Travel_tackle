# 엔티티 정의 문서


---

## User
**테이블**: `users`  
**설명**: 서비스 이용 사용자 정보

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| email | email | String | 이메일 |
| name | name | String | 이름 |
| nationality | nationality | String | 국적 |
| creditBalance | credit_balance | int | 보유 크레딧 |
| freeTrialsUsed | free_trials_used | int | 무료 체험 사용 횟수 |
| createdAt | created_at | LocalDateTime | 가입일시 |

**연관관계**
- `UserPreference` 1:1 (sets)
- `CartItem` 1:N (adds)
- `CreditTransaction` 1:N (has)
- `Trip` 1:N (creates)
- `SavedTrip` 1:N

---

## UserPreference
**테이블**: `user_preferences`  
**설명**: 사용자 여행 취향 설정

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| user | user_id | UUID (FK) | 사용자 참조 |
| travelStyle | travel_style | String | 여행 스타일 |
| budgetLevel | budget_level | String | 예산 수준 |
| preferredRegion | preferred_region | String | 선호 지역 |
| interestTags | 별도 테이블 | Set<InterestTag> | 관심 태그 목록 |

> 관심 태그는 `user_preference_interest_tags` 테이블에 enum 문자열로 저장됩니다.

---

## CartItem
**테이블**: `cart_items`  
**설명**: 사용자가 장바구니에 담은 여행 콘텐츠

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| user | user_id | UUID (FK) | 사용자 참조 |
| tourApiContentId | tour_api_content_id | String | 투어 API 콘텐츠 ID |
| cachedTitle | cached_title | String | 캐시된 콘텐츠 제목 |
| cachedImageUrl | cached_image_url | String | 캐시된 이미지 URL |
| cachedRegionCode | cached_region_code | String | 캐시된 지역 코드 |
| addedAt | added_at | LocalDateTime | 장바구니 추가 일시 |

---

## CreditTransaction
**테이블**: `credit_transactions`  
**설명**: 사용자 크레딧 변동 이력

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| user | user_id | UUID (FK) | 사용자 참조 |
| amount | amount | int | 변동 크레딧 양 (양수: 충전, 음수: 사용) |
| reason | reason | String | 변동 사유 |
| createdAt | created_at | LocalDateTime | 발생 일시 |

---

## Trip
**테이블**: `trips`  
**설명**: 사용자가 생성한 여행 계획

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| user | user_id | UUID (FK) | 작성자 참조 |
| title | title | String | 여행 제목 |
| startDate | start_date | LocalDate | 여행 시작일 |
| endDate | end_date | LocalDate | 여행 종료일 |
| published | is_published | boolean | 공개 여부 |
| createdAt | created_at | LocalDateTime | 생성 일시 |

> Lombok `@Getter`와의 충돌 방지를 위해 Java 필드명은 `published`, 컬럼명은 `is_published`로 분리.

**연관관계**
- `TripDay` 1:N (contains)
- `TripPhoto` 1:N (has)
- `SavedTrip` 1:N (copied_as)

---

## SavedTrip
**테이블**: `saved_trips`  
**설명**: 사용자가 다른 사용자의 여행을 저장(복사)한 기록

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| user | user_id | UUID (FK) | 저장한 사용자 참조 |
| originalTrip | original_trip_id | UUID (FK) | 원본 여행 참조 |
| savedAt | saved_at | LocalDateTime | 저장 일시 |

---

## TripDay
**테이블**: `trip_days`  
**설명**: 여행 계획의 일자별 구성

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| trip | trip_id | UUID (FK) | 여행 참조 |
| dayNumber | day_number | int | 여행 N일차 |
| date | date | LocalDate | 실제 날짜 |

**연관관계**
- `TripItem` 1:N (schedules)

---

## TripPhoto
**테이블**: `trip_photos`  
**설명**: 여행에 첨부된 사진

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| trip | trip_id | UUID (FK) | 여행 참조 |
| imageUrl | image_url | String | 이미지 URL |
| caption | caption | String | 사진 설명 |
| uploadedAt | uploaded_at | LocalDateTime | 업로드 일시 |

---

## TripItem
**테이블**: `trip_items`  
**설명**: 여행 일자에 포함된 개별 일정(장소/활동)

| 필드 (Java) | 컬럼 (DB) | 타입 | 설명 |
|---|---|---|---|
| id | id | UUID (PK) | 고유 식별자 |
| tripDay | trip_day_id | UUID (FK) | 여행 일자 참조 |
| tourApiContentId | tour_api_content_id | String | 투어 API 콘텐츠 ID |
| cachedTitle | cached_title | String | 캐시된 장소 명칭 |
| cachedImageUrl | cached_image_url | String | 캐시된 이미지 URL |
| startTime | start_time | LocalTime | 일정 시작 시간 |
| endTime | end_time | LocalTime | 일정 종료 시간 |
| orderIndex | order_index | int | 해당 날짜 내 순서 |

# FashionProject — 추가한 기능 정리

팀원 참고용 문서입니다. 옷장 · AI 옷 등록 · 착용 기록 · 가상 피팅 쪽에 새로 추가하거나 바꾼 내용을 정리했습니다.

```
fashion_app_shop/
├── fashion_app/   # 안드로이드 앱 (Kotlin, Jetpack Compose)
└── backend/       # PC 서버 (FastAPI, YOLOv8)
```

---

## 실행 방법

### 1. 서버 (PC)

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

- 옷 종류를 분류하는 **DeepFashion2 모델(약 24MB)은 저장소에 없습니다.** 서버가 옷 분류를 처음 요청받을 때
  `backend/models/`에 자동으로 내려받습니다. 첫 요청은 다운로드 때문에 조금 느리고 인터넷이 필요합니다.
- Windows에서 `ultralytics` 설치 중 `cv2` "액세스 거부" 오류가 나면, 이미 설치된 `opencv-python-headless`와
  충돌한 것입니다. 아래처럼 설치하세요.
  ```bash
  pip install --no-deps ultralytics ultralytics-thop polars cloudpickle
  ```
- 서버가 꺼져 있어도 앱은 동작합니다. AI 옷 등록에서 종류를 사용자가 직접 고르게 됩니다.

### 2. 앱

- Android Studio로 `fashion_app` 폴더를 열고 실행합니다.
- 서버 주소는 `data/BackendApi.kt`의 `BASE_URL`입니다.
  - 에뮬레이터: `http://10.0.2.2:8000` (기본값)
  - 실기기: `http://<PC의 IP>:8000` (같은 와이파이, PC 방화벽에서 8000 포트 허용)
- 배경 제거(ML Kit)는 **구글 플레이 서비스가 있는 기기**에서 동작합니다. 첫 사용 때 모델을 내려받습니다.

### 3. 발표 전 체크

- 서버를 켜고 AI 옷 등록을 한 번 해 보기 (모델 다운로드 · 배경 제거 모델 준비)
- 샘플 옷의 지난 착용 기록은 **처음 설치한 날 기준**으로 들어갑니다.
  깔끔한 시연 화면이 필요하면 발표 직전에 앱 데이터를 초기화하거나 새로 설치하세요.

---

## 추가한 기능

### 1. 옷장 데이터 정리
- 흩어져 있던 옷 목록(코드에 고정된 샘플, 메모리에만 있던 옷, 저장된 옷)을 **저장소 하나**(`WardrobeStore`)로 통일
- 옷마다 **고유 번호(id)**, 등록일, 종류·세부 종류·계절을 저장
- 하트(즐겨찾기)가 앱을 껐다 켜도 유지되고, 누른 순서대로 위에 표시
- **샘플 옷 12벌**의 사진을 앱 안(`res/drawable/sample_01~12`)에 넣어 인터넷 없이도 보이게 함
  (쇼핑몰 착용 사진에서 옷만 오려낸 사진과 원래 사진이 섞여 있음)
- 처음 실행할 때 샘플 옷을 약 6개월 전에 등록한 것으로 하고 **지난 착용 기록**을 함께 넣음
  → 설치하자마자 달력 · TOP 5 · 안 입는 옷 알림이 보임

### 2. AI 옷 등록 개선
- 처리 순서: **배경 제거 → 옷 종류 분류 → 옷만 오려내기 → 대표 색 추출**
- **배경 제거(누끼)**: ML Kit 피사체 분리, 폰 안에서 동작
- **옷 종류 분류**: 서버의 DeepFashion2 YOLOv8 모델로 13종류(반팔 상의, 긴바지, 치마, 원피스 등) 구분,
  가방류는 기존 COCO 모델로 보완
- 한 사진에 옷이 여러 벌이면 **후보 칩**으로 바꿔 선택 가능
- **옷만 오려내기**: 고른 옷의 윤곽만 남겨, 사람이 입은 사진에서도 몸·다른 옷을 지움
- 셔츠·니트처럼 AI가 구분 못 하는 **세부 종류는 칩으로 선택**, 계절은 규칙으로 자동 지정(수정 가능)
- 대표 색은 **옷 부분만** 보고 계산하고, 색 이름은 색상·채도·밝기 기준으로 판단
- 사진은 갤러리 주소 대신 **앱 폴더에 보관**(원본 · 배경 제거 · 옷만 세 장)
- 옷장에 **원피스** 탭 추가, 옷 칸에 세부 종류 이름 표시

### 3. 옷 상세 · 수정 · 삭제
- 옷장에서 옷을 누르면 **상세 창**: 분류 · 계절 · 색, 착용 횟수, 마지막 착용일
- **[정보 수정]**: 등록 화면과 같은 모양으로 사진 종류(옷만/배경 제거/원본), 이름, 카테고리, 종류, 세부 종류, 계절 수정
  (저장해 둔 사진 종류 중에서만 선택)
- **[옷장에서 삭제]**: 확인 후 삭제, 앱 폴더의 사진 파일도 함께 삭제

### 4. 착용 기록 · 달력 · 통계
- 옷장의 **[오늘 입은 옷]**: 입은 옷 여러 벌 선택, 날짜 변경, 한 줄 메모
  (하루에 기록 1건 — 같은 날짜로 다시 저장하면 수정)
- **달력**(옷장의 달력 아이콘): 기록한 날에 점, 날짜를 누르면 그날 코디 · 수정 · 삭제
- **통계**: 이번 달 기록 일수, 가장 많이 입은 옷 TOP 5

### 5. 안 입는 옷 알림 (앱 안에서만 표시)
- 옷장 위 카드: **"3개월 넘게 안 입은 옷이 N벌 있어요"**
- 기준일 = 마지막 착용일 (한 번도 안 입었으면 등록일)
- **지금 계절에 안 맞는 옷은 제외**하고, 제외한 옷은 목록 아래에 따로 안내
- 옷마다 **[정리하기]**(삭제) / **[계속 보관]**(누른 날부터 90일을 다시 셈)

### 6. 가상 피팅 개선
- 옷 사진은 **옷만 → 배경 제거** 순으로 사용, 둘 다 없으면 원본을 쓰고 안내 문구 표시
- 상의 · 아우터 · 하의 · 신발 · 액세서리를 **종류별로 한 벌씩 함께** 겹쳐 입기
- 전신 사진에서 ML Kit 포즈 인식으로 **어깨 · 골반 · 발목**을 찾아 자동으로 맞춤
  (상의 → 어깨, 하의 → 골반, 신발 → 발목, 어깨 기울기 반영)
- 크기 슬라이더와 [−][+], 두 손가락으로 크기·회전, 드래그로 이동, **[몸에 맞추기]** 버튼

### 7. 서버
- 새 API **`POST /api/clothing/classify`**: 옷 사진 → 옷 종류 후보 목록(크게 잡힌 순) + 옷 윤곽(흑백 PNG, base64)
  - 같은 옷에 이름이 겹쳐 잡히면 확신도가 높은 쪽만 남김
  - 기존 `/api/object-detection`은 그대로 유지
- DeepFashion2 모델 **자동 다운로드** (자세한 내용은 `backend/README.md`)

---

## 새로 생긴 파일

**앱** (`fashion_app/app/src/main/java/com/jeiu/fashionproject/`)

| 파일 | 역할 |
|---|---|
| `data/SampleWardrobe.kt` | 샘플 옷 12벌 정의 |
| `data/SampleWearHistory.kt` | 첫 실행 때 샘플 옷 등록일 · 지난 착용 기록 넣기 |
| `data/ClothingCategories.kt` | 카테고리 · 종류 · 세부 종류 · 계절 규칙, AI 종류 → 앱 분류 변환표 |
| `data/WearLogStore.kt` | 착용 기록 저장, 날짜 도우미 |
| `ui/viewmodel/BackgroundRemover.kt` | 배경 제거, 옷만 오려내기, 사진 저장 |
| `ui/viewmodel/WearAnalyzer.kt` | 착용 횟수 · 마지막 착용일 · TOP 5 · 안 입는 옷 계산 |
| `ui/viewmodel/FittingPoseDetector.kt` | 가상 피팅용 어깨 · 골반 · 발목 찾기 |
| `ui/screens/WearLogScreen.kt` | 입은 옷 기록 화면 |
| `ui/screens/WearCalendarScreen.kt` | 달력 + 통계 화면 |
| `ui/components/ClothingThumb.kt` | 옷 사진 칸 공용 부품 |
| `ui/components/UnwornAlertCard.kt` | 안 입는 옷 알림 카드 + 목록 |
| `res/drawable/sample_01 ~ sample_12` | 샘플 옷 사진 |

**서버** (`backend/`)

| 파일 | 역할 |
|---|---|
| `fashion_detection.py` | DeepFashion2 + COCO 가방류 분류, 모델 자동 다운로드 |
| `models/` | 모델 파일 저장 위치 (git에는 넣지 않음) |

**크게 바뀐 기존 파일**: `WardrobeStore.kt`, `BackendApi.kt`, `AddClothingViewModel.kt`, `AddClothingScreen.kt`,
`WardrobeScreen.kt`, `VirtualFittingScreen.kt`, `navigation/*`, `backend/main.py`, `backend/schemas.py`

**추가한 라이브러리**: 앱 — ML Kit 피사체 분리(`play-services-mlkit-subject-segmentation`) / 서버 — `ultralytics`

---

## 알려진 한계 · 주의사항

- **세부 종류**(셔츠/니트 등)는 AI가 구분하지 못해 사용자가 고릅니다.
- 바닥에 펼친 옷 사진이나 옷과 배경 색이 비슷한 사진은 분류 · 배경 제거 정확도가 떨어질 수 있습니다.
- 옷만 오려내기에서 옷 위에 겹친 손 · 머리카락이 남을 수 있습니다.
- 가상 피팅은 사진을 겹쳐 올리는 방식이라 옷이 몸 모양대로 휘지 않고, 맞추는 비율은 대략값입니다.
  정면 전신 사진이어야 몸 위치를 잘 찾습니다.
- 신발 · 액세서리는 자동 분류되지 않습니다(가방류만 가능).
- **DeepFashion2 학습 데이터는 비상업 연구용**이라 캡스톤 시연 용도로만 사용합니다.
  모델 출처: 허깅페이스 [Bingsu/adetailer](https://huggingface.co/Bingsu/adetailer) (`deepfashion2_yolov8s-seg.pt`)
- 착용 기록 · 알림 상태는 폰 안(SharedPreferences)에만 저장되어 서버와 동기화되지 않습니다.

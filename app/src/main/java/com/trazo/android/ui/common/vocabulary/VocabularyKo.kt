package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Korean. */
internal val VocabularyKo = Vocabulary(
    goal = mapOf("GeneralFitness" to "일반 체력", "Hypertrophy" to "근비대", "Strength" to "근력"),
    experience = mapOf("Beginner" to "초급", "Intermediate" to "중급", "Advanced" to "고급"),
    limitation = mapOf("Knee" to "무릎", "Shoulder" to "어깨", "LowerBack" to "허리", "Wrist" to "손목", "Elbow" to "팔꿈치"),
    recovery = mapOf("Unknown" to "말하고 싶지 않음", "Good" to "잘 회복됨", "Partial" to "일부 회복됨", "Insufficient" to "회복이 부족함"),
    reason = mapOf("Time" to "시간 부족", "Fatigue" to "피로", "Equipment" to "장비", "Motivation" to "의욕", "Illness" to "질병", "Other" to "기타"),
    phase = mapOf("accumulation" to "축적기", "intensification" to "강화기", "deload" to "디로드", "preparation" to "준비기"),
    pattern = mapOf(
            "Squat" to "스쿼트", "Hinge" to "힙 힌지", "HorizontalPush" to "수평 밀기", "VerticalPush" to "수직 밀기",
            "HorizontalPull" to "수평 당기기", "VerticalPull" to "수직 당기기", "SingleLeg" to "한 다리 운동", "Core" to "코어", "Accessory" to "보조 운동"
        ),
    muscle = mapOf(
            "Chest" to "가슴", "Lats" to "광배근", "UpperBack" to "상부 등", "Deltoids" to "어깨", "Biceps" to "이두근", "Triceps" to "삼두근",
            "Forearms" to "전완", "Abdominals" to "복근", "Obliques" to "복사근", "ErectorSpinae" to "허리", "Glutes" to "둔근",
            "Quadriceps" to "대퇴사두근", "Hamstrings" to "햄스트링", "Calves" to "종아리", "Adductors" to "내전근", "Abductors" to "외전근",
            "HipFlexors" to "고관절 굴곡근", "Serratus" to "전거근", "TibialisAnterior" to "전경골근", "RotatorCuff" to "회전근개"
        ),
    equipment = mapOf(
            "barbell" to "바벨", "dumbbell" to "덤벨", "kettlebell" to "케틀벨", "cable" to "케이블 머신",
            "bench" to "웨이트 벤치", "squat-rack" to "스쿼트 랙", "pull-up-bar" to "풀업 바",
            "low-row-bar" to "낮은 로우용 바", "bodyweight" to "맨몸", "body-only" to "맨몸",
            "band" to "저항 밴드", "bands" to "저항 밴드 세트", "resistance-band" to "저항 밴드",
            "ez-bar" to "EZ 바", "trap-bar" to "트랩 바(헥스 바)", "smith-machine" to "스미스 머신",
            "leg-press-machine" to "레그 프레스 머신", "hack-squat-machine" to "핵 스쿼트 머신",
            "leg-curl-machine" to "레그 컬 머신", "leg-extension-machine" to "레그 익스텐션 머신",
            "dip-station" to "딥 스테이션 / 평행봉", "captains-chair" to "캡틴스 체어",
            "ab-wheel" to "복근 롤러", "step" to "플라이오 박스 / 스텝", "stability-ball" to "짐볼",
            "medicine-ball" to "메디신 볼", "exercise-ball" to "운동 볼", "rings" to "체조 링",
            "dip-bar" to "딥 바", "trx" to "TRX", "suspension-trainer" to "서스펜션 트레이너",
            "rower" to "로잉 머신", "glute-ham-developer" to "GHD 벤치",
            "landmine" to "바벨 랜드마인 어태치먼트", "t-bar-row-handle" to "T바 로우 핸들",
            "preacher-curl-bench" to "프리처 컬 벤치", "other" to "기타",
            "assisted-pull-up-machine" to "어시스트 풀업 머신", "chest-press-machine" to "체스트 프레스 머신",
            "seated-row-machine" to "시티드 로우 머신", "hip-abduction-machine" to "힙 어브덕션 머신",
            "hip-adduction-machine" to "힙 애덕션 머신", "back-extension-machine" to "백 익스텐션 머신",
            "bicep-curl-machine" to "바이셉스 컬 머신", "standing-calf-raise-machine" to "스탠딩 카프 레이즈 머신",
            "seated-calf-raise-machine" to "시티드 카프 레이즈 머신", "chest-fly-machine" to "체스트 플라이 머신",
            "reverse-fly-machine" to "리버스 플라이 머신", "ab-crunch-machine" to "복근 크런치 머신",
            "shoulder-press-machine" to "숄더 프레스 머신", "pec-deck" to "펙 덱",
            "hip-thrust-machine" to "힙 쓰러스트 머신",
            "plate-loaded-lateral-raise-machine" to "플레이트 사이드 레터럴 레이즈 머신",
            "lat-pulldown-machine" to "랫 풀다운 머신"
        ),
    difficulty = mapOf("Beginner" to "초급", "Intermediate" to "중급", "Advanced" to "고급", "Easy" to "쉬움", "Hard" to "어려움"),
    split = mapOf("Automatic" to "자동", "FullBody" to "전신", "UpperLower" to "상체 / 하체", "PushPullLegs" to "밀기 / 당기기 / 하체", "Hybrid" to "하이브리드", "BroSplit" to "부위별")
)

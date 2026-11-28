package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Arabic. */
internal val VocabularyAr = Vocabulary(
    goal = mapOf("GeneralFitness" to "لياقة عامة", "Hypertrophy" to "بناء العضلات", "Strength" to "القوة"),
    experience = mapOf("Beginner" to "مبتدئ", "Intermediate" to "متوسط", "Advanced" to "متقدم"),
    limitation = mapOf("Knee" to "الركبة", "Shoulder" to "الكتف", "LowerBack" to "أسفل الظهر", "Wrist" to "الرسغ", "Elbow" to "المرفق"),
    reason = mapOf("Time" to "ضيق الوقت", "Fatigue" to "الإرهاق", "Equipment" to "المعدات", "Motivation" to "الدافع", "Illness" to "المرض", "Other" to "أخرى"),
    phase = mapOf("accumulation" to "التراكم", "intensification" to "التكثيف", "deload" to "التخفيف", "preparation" to "التحضير"),
    pattern = mapOf(
            "Squat" to "القرفصاء", "Hinge" to "ثني الورك (hip hinge)", "HorizontalPush" to "دفع أفقي", "VerticalPush" to "دفع رأسي",
            "HorizontalPull" to "سحب أفقي", "VerticalPull" to "سحب رأسي", "SingleLeg" to "بساق واحدة", "Core" to "عضلات الجذع", "Accessory" to "تمرين مساعد"
        ),
    muscle = mapOf(
            "Chest" to "الصدر", "Lats" to "عضلات الظهر العريضة", "UpperBack" to "أعلى الظهر", "Deltoids" to "الكتفان", "Biceps" to "ذات الرأسين", "Triceps" to "ثلاثية الرؤوس",
            "Forearms" to "الساعدان", "Abdominals" to "عضلات البطن", "Obliques" to "عضلات البطن المائلة", "ErectorSpinae" to "أسفل الظهر", "Glutes" to "الأرداف",
            "Quadriceps" to "الفخذ الأمامي", "Hamstrings" to "الفخذ الخلفي", "Calves" to "السمانة", "Adductors" to "عضلات التقريب", "Abductors" to "عضلات الإبعاد",
            "HipFlexors" to "ثانيات الورك", "Serratus" to "العضلة المسننة", "TibialisAnterior" to "الظنبوبية الأمامية", "RotatorCuff" to "الكفة المدورة"
        ),
    equipment = mapOf(
            "barbell" to "بار حديد", "dumbbell" to "دمبلز", "kettlebell" to "كيتل بيل", "cable" to "جهاز الكابلات",
            "bench" to "مقعد أوزان", "squat-rack" to "حامل القرفصاء", "pull-up-bar" to "عقلة",
            "low-row-bar" to "بار تجديف منخفض", "bodyweight" to "وزن الجسم", "body-only" to "وزن الجسم",
            "band" to "حزام مقاومة", "bands" to "أحزمة مقاومة", "resistance-band" to "حزام مقاومة",
            "ez-bar" to "بار EZ", "trap-bar" to "بار سداسي", "smith-machine" to "جهاز سميث",
            "leg-press-machine" to "جهاز ضغط الساقين", "hack-squat-machine" to "جهاز هاك سكوات",
            "leg-curl-machine" to "جهاز ثني الساق", "leg-extension-machine" to "جهاز فرد الساق",
            "dip-station" to "جهاز المتوازي", "captains-chair" to "كرسي رفع الساقين",
            "ab-wheel" to "عجلة البطن", "step" to "صندوق بلايومتري / ستيب", "stability-ball" to "كرة التوازن",
            "medicine-ball" to "كرة طبية", "exercise-ball" to "كرة رياضية", "rings" to "حلقات جمباز",
            "dip-bar" to "متوازي", "trx" to "TRX", "suspension-trainer" to "جهاز تمارين التعليق",
            "rower" to "جهاز التجديف", "glute-ham-developer" to "مقعد GHD",
            "landmine" to "ملحق لاندماين للبار", "t-bar-row-handle" to "مقبض تجديف T-bar",
            "preacher-curl-bench" to "مقعد سكوت", "other" to "أخرى",
            "assisted-pull-up-machine" to "جهاز عقلة بمساعدة", "chest-press-machine" to "جهاز ضغط الصدر",
            "seated-row-machine" to "جهاز تجديف جالسًا", "hip-abduction-machine" to "جهاز إبعاد الفخذ",
            "hip-adduction-machine" to "جهاز تقريب الفخذ", "back-extension-machine" to "جهاز تمديد الظهر",
            "bicep-curl-machine" to "جهاز ثني الذراع للبايسبس", "standing-calf-raise-machine" to "جهاز السمانة واقفًا",
            "seated-calf-raise-machine" to "جهاز السمانة جالسًا", "chest-fly-machine" to "جهاز الفراشة",
            "reverse-fly-machine" to "جهاز الفراشة العكسية", "ab-crunch-machine" to "جهاز تمرين البطن",
            "shoulder-press-machine" to "جهاز ضغط الكتف", "pec-deck" to "بيك ديك",
            "hip-thrust-machine" to "جهاز هيب ثراست",
            "plate-loaded-lateral-raise-machine" to "جهاز رفع جانبي بالأقراص",
            "lat-pulldown-machine" to "جهاز السحب العلوي"
        ),
    difficulty = mapOf("Beginner" to "مبتدئ", "Intermediate" to "متوسط", "Advanced" to "متقدم", "Easy" to "سهل", "Hard" to "صعب"),
    split = mapOf("Automatic" to "تلقائي", "FullBody" to "الجسم كله", "UpperLower" to "الجزء العلوي / السفلي", "PushPullLegs" to "دفع / سحب / ساقان", "Hybrid" to "هجين", "BroSplit" to "حسب المجموعات العضلية")
)

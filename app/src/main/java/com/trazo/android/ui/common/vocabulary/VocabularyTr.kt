package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Turkish. */
internal val VocabularyTr = Vocabulary(
    goal = mapOf("GeneralFitness" to "Genel kondisyon", "Hypertrophy" to "Kas gelişimi", "Strength" to "Güç"),
    experience = mapOf("Beginner" to "Başlangıç", "Intermediate" to "Orta", "Advanced" to "İleri"),
    limitation = mapOf("Knee" to "Diz", "Shoulder" to "Omuz", "LowerBack" to "Bel", "Wrist" to "Bilek", "Elbow" to "Dirsek"),
    recovery = mapOf("Unknown" to "Söylemek istemiyorum", "Good" to "İyi dinlenmiş", "Partial" to "Kısmen dinlenmiş", "Insufficient" to "Az dinlenmiş"),
    reason = mapOf("Time" to "Zaman yetersizliği", "Fatigue" to "Yorgunluk", "Equipment" to "Ekipman", "Motivation" to "Motivasyon", "Illness" to "Hastalık", "Other" to "Diğer"),
    phase = mapOf("accumulation" to "Birikim", "intensification" to "Yoğunlaştırma", "deload" to "Hafifletme", "preparation" to "Hazırlık"),
    pattern = mapOf(
            "Squat" to "Squat", "Hinge" to "Kalça menteşesi", "HorizontalPush" to "Yatay itiş", "VerticalPush" to "Dikey itiş",
            "HorizontalPull" to "Yatay çekiş", "VerticalPull" to "Dikey çekiş", "SingleLeg" to "Tek bacak", "Core" to "Core", "Accessory" to "Yardımcı"
        ),
    muscle = mapOf(
            "Chest" to "Göğüs", "Lats" to "Kanat (sırt)", "UpperBack" to "Üst sırt", "Deltoids" to "Omuzlar", "Biceps" to "Biseps", "Triceps" to "Triseps",
            "Forearms" to "Ön kol", "Abdominals" to "Karın", "Obliques" to "Yan karın", "ErectorSpinae" to "Bel", "Glutes" to "Kalça",
            "Quadriceps" to "Ön bacak (quadriceps)", "Hamstrings" to "Arka bacak", "Calves" to "Baldır", "Adductors" to "Bacak içi (addüktör)", "Abductors" to "Bacak dışı (abdüktör)",
            "HipFlexors" to "Kalça fleksörleri", "Serratus" to "Serratus", "TibialisAnterior" to "Tibialis anterior", "RotatorCuff" to "Rotator manşet"
        ),
    equipment = mapOf(
            "barbell" to "Halter (bar)", "dumbbell" to "Dumbbell", "kettlebell" to "Kettlebell", "cable" to "Kablo istasyonu",
            "bench" to "Ağırlık sehpası", "squat-rack" to "Squat rafı", "pull-up-bar" to "Barfiks barı",
            "low-row-bar" to "Alçak kürek barı", "bodyweight" to "Vücut ağırlığı", "body-only" to "Vücut ağırlığı",
            "band" to "Direnç bandı", "bands" to "Direnç bantları", "resistance-band" to "Direnç bandı",
            "ez-bar" to "EZ bar", "trap-bar" to "Trap bar / altıgen bar", "smith-machine" to "Smith makinesi",
            "leg-press-machine" to "Leg press makinesi", "hack-squat-machine" to "Hack squat makinesi",
            "leg-curl-machine" to "Bacak kıvırma makinesi", "leg-extension-machine" to "Bacak açma makinesi",
            "dip-station" to "Dip istasyonu / paralel bar", "captains-chair" to "Kaptan sandalyesi",
            "ab-wheel" to "Karın tekerleği", "step" to "Pliometrik kutu / step", "stability-ball" to "Denge topu",
            "medicine-ball" to "Sağlık topu", "exercise-ball" to "Pilates topu", "rings" to "Jimnastik halkaları",
            "dip-bar" to "Dip barları", "trx" to "TRX", "suspension-trainer" to "Askı antrenörü",
            "rower" to "Kürek makinesi", "glute-ham-developer" to "GHD sehpası",
            "landmine" to "Halter için landmine aparatı", "t-bar-row-handle" to "T-bar kürek tutamağı",
            "preacher-curl-bench" to "Scott sehpası", "other" to "Diğer",
            "assisted-pull-up-machine" to "Destekli barfiks makinesi", "chest-press-machine" to "Göğüs press makinesi",
            "seated-row-machine" to "Oturarak kürek makinesi", "hip-abduction-machine" to "Bacak açma (abdüktör) makinesi",
            "hip-adduction-machine" to "Bacak kapama (addüktör) makinesi", "back-extension-machine" to "Bel açma makinesi",
            "bicep-curl-machine" to "Biseps curl makinesi", "standing-calf-raise-machine" to "Ayakta baldır makinesi",
            "seated-calf-raise-machine" to "Oturarak baldır makinesi", "chest-fly-machine" to "Göğüs fly makinesi",
            "reverse-fly-machine" to "Ters fly makinesi", "ab-crunch-machine" to "Karın crunch makinesi",
            "shoulder-press-machine" to "Omuz press makinesi", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust makinesi",
            "plate-loaded-lateral-raise-machine" to "Plakalı yan omuz kaldırma makinesi",
            "lat-pulldown-machine" to "Lat pulldown makinesi"
        ),
    difficulty = mapOf("Beginner" to "Başlangıç", "Intermediate" to "Orta", "Advanced" to "İleri", "Easy" to "Kolay", "Hard" to "Zor"),
    split = mapOf("Automatic" to "Otomatik", "FullBody" to "Tüm vücut", "UpperLower" to "Üst / alt vücut", "PushPullLegs" to "İtiş / çekiş / bacak", "BroSplit" to "Kas grubuna göre")
)

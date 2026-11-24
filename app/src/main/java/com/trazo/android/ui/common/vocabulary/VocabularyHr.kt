package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Croatian. */
internal val VocabularyHr = Vocabulary(
    goal = mapOf("GeneralFitness" to "Opća kondicija", "Hypertrophy" to "Rast mišića", "Strength" to "Snaga"),
    experience = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednja razina", "Advanced" to "Napredni"),
    limitation = mapOf("Knee" to "Koljeno", "Shoulder" to "Rame", "LowerBack" to "Donji dio leđa", "Wrist" to "Zglob šake", "Elbow" to "Lakat"),
    recovery = mapOf("Unknown" to "Radije ne bih rekao", "Good" to "Dobro oporavljen", "Partial" to "Djelomično oporavljen", "Insufficient" to "Slabo oporavljen"),
    reason = mapOf("Time" to "Nedostatak vremena", "Fatigue" to "Umor", "Equipment" to "Oprema", "Motivation" to "Motivacija", "Illness" to "Bolest", "Other" to "Ostalo"),
    phase = mapOf("accumulation" to "Akumulacija", "intensification" to "Intenzifikacija", "deload" to "Rasterećenje", "preparation" to "Priprema"),
    pattern = mapOf(
            "Squat" to "Čučanj", "Hinge" to "Pregib u kuku (hip hinge)", "HorizontalPush" to "Horizontalno potiskivanje", "VerticalPush" to "Vertikalno potiskivanje",
            "HorizontalPull" to "Horizontalno povlačenje", "VerticalPull" to "Vertikalno povlačenje", "SingleLeg" to "Jednonožno", "Core" to "Core", "Accessory" to "Dodatna vježba"
        ),
    muscle = mapOf(
            "Chest" to "Prsa", "Lats" to "Široki leđni mišić", "UpperBack" to "Gornji dio leđa", "Deltoids" to "Ramena", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Podlaktice", "Abdominals" to "Trbušni mišići", "Obliques" to "Kosi trbušni mišići", "ErectorSpinae" to "Donji dio leđa", "Glutes" to "Gluteusi",
            "Quadriceps" to "Četveroglavi mišić", "Hamstrings" to "Stražnja strana bedara", "Calves" to "Listovi", "Adductors" to "Aduktori", "Abductors" to "Abduktori",
            "HipFlexors" to "Pregibači kuka", "Serratus" to "Prednji nazubljeni mišić", "TibialisAnterior" to "Prednji goljenični mišić", "RotatorCuff" to "Rotatorna manšeta"
        ),
    equipment = mapOf(
            "barbell" to "Šipka", "dumbbell" to "Bučice", "kettlebell" to "Kettlebell", "cable" to "Kabelska stanica",
            "bench" to "Klupa za vježbanje", "squat-rack" to "Stalak za čučnjeve", "pull-up-bar" to "Vratilo za zgibove",
            "low-row-bar" to "Niska šipka za veslanje", "bodyweight" to "Vlastita težina", "body-only" to "Vlastita težina",
            "band" to "Elastična traka", "bands" to "Elastične trake", "resistance-band" to "Elastična traka",
            "ez-bar" to "EZ šipka", "trap-bar" to "Šesterokutna šipka", "smith-machine" to "Smithov stroj",
            "leg-press-machine" to "Preša za noge", "hack-squat-machine" to "Stroj za hack čučanj",
            "leg-curl-machine" to "Stroj za pregib nogu", "leg-extension-machine" to "Stroj za ekstenziju nogu",
            "dip-station" to "Paralelne šipke za sklekove (dips)", "captains-chair" to "Stolica za podizanje nogu",
            "ab-wheel" to "Kotač za trbuh", "step" to "Pliometrijska kutija / step", "stability-ball" to "Lopta za stabilnost",
            "medicine-ball" to "Medicinka", "exercise-ball" to "Fitness lopta", "rings" to "Gimnastički krugovi",
            "dip-bar" to "Paralelne šipke", "trx" to "TRX", "suspension-trainer" to "Trener s trakama",
            "rower" to "Veslački trenažer", "glute-ham-developer" to "GHD klupa",
            "landmine" to "Landmine nastavak za šipku", "t-bar-row-handle" to "Ručka za veslanje T-šipkom",
            "preacher-curl-bench" to "Scott klupa", "other" to "Ostalo",
            "assisted-pull-up-machine" to "Stroj za potpomognute zgibove", "chest-press-machine" to "Stroj za potisak prsa",
            "seated-row-machine" to "Stroj za veslanje sjedeći", "hip-abduction-machine" to "Stroj za abdukciju",
            "hip-adduction-machine" to "Stroj za addukciju", "back-extension-machine" to "Stroj za ekstenziju leđa",
            "bicep-curl-machine" to "Stroj za pregib bicepsa", "standing-calf-raise-machine" to "Stroj za listove stojeći",
            "seated-calf-raise-machine" to "Stroj za listove sjedeći", "chest-fly-machine" to "Stroj za razvlačenje prsa (fly)",
            "reverse-fly-machine" to "Stroj za obrnuti fly", "ab-crunch-machine" to "Stroj za trbušnjake",
            "shoulder-press-machine" to "Stroj za potisak ramena", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Stroj za hip thrust",
            "plate-loaded-lateral-raise-machine" to "Stroj za bočno podizanje s pločama",
            "lat-pulldown-machine" to "Stroj za povlačenje prema dolje"
        ),
    difficulty = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednja razina", "Advanced" to "Napredni", "Easy" to "Lako", "Hard" to "Teško"),
    split = mapOf("Automatic" to "Automatski", "FullBody" to "Cijelo tijelo", "UpperLower" to "Gornji / donji dio tijela", "PushPullLegs" to "Potisak / povlačenje / noge", "BroSplit" to "Po mišićnim skupinama")
)

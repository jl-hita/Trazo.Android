package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Bosnian. */
internal val VocabularyBs = Vocabulary(
    goal = mapOf("GeneralFitness" to "Opća kondicija", "Hypertrophy" to "Rast mišića", "Strength" to "Snaga"),
    experience = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednji nivo", "Advanced" to "Napredni"),
    limitation = mapOf("Knee" to "Koljeno", "Shoulder" to "Rame", "LowerBack" to "Donji dio leđa", "Wrist" to "Ručni zglob", "Elbow" to "Lakat"),
    recovery = mapOf("Unknown" to "Radije ne bih rekao", "Good" to "Dobro oporavljen", "Partial" to "Djelimično oporavljen", "Insufficient" to "Slabo oporavljen"),
    reason = mapOf("Time" to "Nedostatak vremena", "Fatigue" to "Umor", "Equipment" to "Oprema", "Motivation" to "Motivacija", "Illness" to "Bolest", "Other" to "Ostalo"),
    phase = mapOf("accumulation" to "Akumulacija", "intensification" to "Intenzifikacija", "deload" to "Rasterećenje", "preparation" to "Priprema"),
    pattern = mapOf(
            "Squat" to "Čučanj", "Hinge" to "Pregib u kuku (hip hinge)", "HorizontalPush" to "Horizontalno guranje", "VerticalPush" to "Vertikalno guranje",
            "HorizontalPull" to "Horizontalno povlačenje", "VerticalPull" to "Vertikalno povlačenje", "SingleLeg" to "Jednonožno", "Core" to "Core", "Accessory" to "Dodatna vježba"
        ),
    muscle = mapOf(
            "Chest" to "Prsa", "Lats" to "Široki leđni mišić", "UpperBack" to "Gornji dio leđa", "Deltoids" to "Ramena", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Podlaktice", "Abdominals" to "Trbušni mišići", "Obliques" to "Kosi trbušni mišići", "ErectorSpinae" to "Donji dio leđa", "Glutes" to "Gluteusi",
            "Quadriceps" to "Četveroglavi mišić", "Hamstrings" to "Zadnja loža", "Calves" to "Listovi", "Adductors" to "Aduktori", "Abductors" to "Abduktori",
            "HipFlexors" to "Pregibači kuka", "Serratus" to "Prednji nazubljeni mišić", "TibialisAnterior" to "Prednji goljenični mišić", "RotatorCuff" to "Rotatorna manšeta"
        ),
    equipment = mapOf(
            "barbell" to "Šipka", "dumbbell" to "Bučice", "kettlebell" to "Kettlebell", "cable" to "Kablovska stanica",
            "bench" to "Klupa za vježbanje", "squat-rack" to "Stalak za čučnjeve", "pull-up-bar" to "Šipka za zgibove",
            "low-row-bar" to "Niska šipka za veslanje", "bodyweight" to "Vlastita težina", "body-only" to "Vlastita težina",
            "band" to "Elastična traka", "bands" to "Elastične trake", "resistance-band" to "Elastična traka",
            "ez-bar" to "EZ šipka", "trap-bar" to "Šestougaona šipka", "smith-machine" to "Smith mašina",
            "leg-press-machine" to "Presa za noge", "hack-squat-machine" to "Hack čučanj mašina",
            "leg-curl-machine" to "Mašina za pregib nogu", "leg-extension-machine" to "Mašina za ekstenziju nogu",
            "dip-station" to "Paralelne šipke za sklekove (dips)", "captains-chair" to "Stolica za podizanje nogu",
            "ab-wheel" to "Točak za trbuh", "step" to "Pliometrijska kutija / step", "stability-ball" to "Lopta za stabilnost",
            "medicine-ball" to "Medicinka", "exercise-ball" to "Fitness lopta", "rings" to "Gimnastički krugovi",
            "dip-bar" to "Paralelne šipke", "trx" to "TRX", "suspension-trainer" to "Trenažer s trakama",
            "rower" to "Veslački trenažer", "glute-ham-developer" to "GHD klupa",
            "landmine" to "Landmine nastavak za šipku", "t-bar-row-handle" to "Ručka za veslanje T-šipkom",
            "preacher-curl-bench" to "Scott klupa", "other" to "Ostalo",
            "assisted-pull-up-machine" to "Mašina za potpomognute zgibove", "chest-press-machine" to "Mašina za potisak prsa",
            "seated-row-machine" to "Mašina za veslanje u sjedećem položaju", "hip-abduction-machine" to "Mašina za abdukciju",
            "hip-adduction-machine" to "Mašina za addukciju", "back-extension-machine" to "Mašina za ekstenziju leđa",
            "bicep-curl-machine" to "Mašina za pregib bicepsa", "standing-calf-raise-machine" to "Mašina za listove stojeći",
            "seated-calf-raise-machine" to "Mašina za listove sjedeći", "chest-fly-machine" to "Mašina za razvlačenje prsa (fly)",
            "reverse-fly-machine" to "Mašina za obrnuti fly", "ab-crunch-machine" to "Mašina za trbušnjake",
            "shoulder-press-machine" to "Mašina za potisak ramena", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Mašina za hip thrust",
            "plate-loaded-lateral-raise-machine" to "Mašina za bočno podizanje s pločama",
            "lat-pulldown-machine" to "Mašina za povlačenje prema dolje"
        ),
    difficulty = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednji nivo", "Advanced" to "Napredni", "Easy" to "Lako", "Hard" to "Teško"),
    split = mapOf("Automatic" to "Automatski", "FullBody" to "Cijelo tijelo", "UpperLower" to "Gornji / donji dio tijela", "PushPullLegs" to "Guranje / povlačenje / noge", "Hybrid" to "Hibridno", "BroSplit" to "Po mišićnim grupama")
)

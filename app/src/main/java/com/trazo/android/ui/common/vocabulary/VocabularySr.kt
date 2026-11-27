package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Serbian (Latin). */
internal val VocabularySr = Vocabulary(
    goal = mapOf("GeneralFitness" to "Opšta kondicija", "Hypertrophy" to "Rast mišića", "Strength" to "Snaga"),
    experience = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednji nivo", "Advanced" to "Napredni"),
    limitation = mapOf("Knee" to "Koleno", "Shoulder" to "Rame", "LowerBack" to "Donji deo leđa", "Wrist" to "Zglob šake", "Elbow" to "Lakat"),
    recovery = mapOf("Unknown" to "Radije ne bih rekao", "Good" to "Dobro oporavljen", "Partial" to "Delimično oporavljen", "Insufficient" to "Slabo oporavljen"),
    reason = mapOf("Time" to "Nedostatak vremena", "Fatigue" to "Umor", "Equipment" to "Oprema", "Motivation" to "Motivacija", "Illness" to "Bolest", "Other" to "Ostalo"),
    phase = mapOf("accumulation" to "Akumulacija", "intensification" to "Intenzifikacija", "deload" to "Rasterećenje", "preparation" to "Priprema"),
    pattern = mapOf(
            "Squat" to "Čučanj", "Hinge" to "Pregib u kuku (hip hinge)", "HorizontalPush" to "Horizontalno potiskivanje", "VerticalPush" to "Vertikalno potiskivanje",
            "HorizontalPull" to "Horizontalno povlačenje", "VerticalPull" to "Vertikalno povlačenje", "SingleLeg" to "Jednonožno", "Core" to "Core", "Accessory" to "Dodatna vežba"
        ),
    muscle = mapOf(
            "Chest" to "Grudi", "Lats" to "Široki leđni mišić", "UpperBack" to "Gornji deo leđa", "Deltoids" to "Ramena", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Podlaktice", "Abdominals" to "Trbušni mišići", "Obliques" to "Kosi trbušni mišići", "ErectorSpinae" to "Donji deo leđa", "Glutes" to "Gluteusi",
            "Quadriceps" to "Četvoroglavi mišić", "Hamstrings" to "Zadnja loža", "Calves" to "Listovi", "Adductors" to "Aduktori", "Abductors" to "Abduktori",
            "HipFlexors" to "Pregibači kuka", "Serratus" to "Prednji zupčasti mišić", "TibialisAnterior" to "Prednji golenjački mišić", "RotatorCuff" to "Rotatorna manžetna"
        ),
    equipment = mapOf(
            "barbell" to "Šipka", "dumbbell" to "Bučice", "kettlebell" to "Kettlebell", "cable" to "Kablovska stanica",
            "bench" to "Klupa za vežbanje", "squat-rack" to "Stalak za čučnjeve", "pull-up-bar" to "Vratilo za zgibove",
            "low-row-bar" to "Niska šipka za veslanje", "bodyweight" to "Sopstvena težina", "body-only" to "Sopstvena težina",
            "band" to "Elastična traka", "bands" to "Elastične trake", "resistance-band" to "Elastična traka",
            "ez-bar" to "EZ šipka", "trap-bar" to "Šestougaona šipka", "smith-machine" to "Smit mašina",
            "leg-press-machine" to "Presa za noge", "hack-squat-machine" to "Mašina za hak čučanj",
            "leg-curl-machine" to "Mašina za pregib nogu", "leg-extension-machine" to "Mašina za opružanje nogu",
            "dip-station" to "Paralelne šipke za sklekove (dips)", "captains-chair" to "Stolica za podizanje nogu",
            "ab-wheel" to "Točak za trbuh", "step" to "Pliometrijska kutija / step", "stability-ball" to "Lopta za stabilnost",
            "medicine-ball" to "Medicinka", "exercise-ball" to "Fitnes lopta", "rings" to "Gimnastički krugovi",
            "dip-bar" to "Paralelne šipke", "trx" to "TRX", "suspension-trainer" to "Trener sa trakama",
            "rower" to "Veslački trenažer", "glute-ham-developer" to "GHD klupa",
            "landmine" to "Landmine nastavak za šipku", "t-bar-row-handle" to "Ručka za veslanje T-šipkom",
            "preacher-curl-bench" to "Skot klupa", "other" to "Ostalo",
            "assisted-pull-up-machine" to "Mašina za potpomognute zgibove", "chest-press-machine" to "Mašina za potisak grudi",
            "seated-row-machine" to "Mašina za veslanje sedeći", "hip-abduction-machine" to "Mašina za abdukciju",
            "hip-adduction-machine" to "Mašina za adukciju", "back-extension-machine" to "Mašina za opružanje leđa",
            "bicep-curl-machine" to "Mašina za pregib bicepsa", "standing-calf-raise-machine" to "Mašina za listove stojeći",
            "seated-calf-raise-machine" to "Mašina za listove sedeći", "chest-fly-machine" to "Mašina za razvlačenje grudi (fly)",
            "reverse-fly-machine" to "Mašina za obrnuti fly", "ab-crunch-machine" to "Mašina za trbušnjake",
            "shoulder-press-machine" to "Mašina za potisak ramena", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Mašina za hip thrust",
            "plate-loaded-lateral-raise-machine" to "Mašina za bočno podizanje sa pločama",
            "lat-pulldown-machine" to "Mašina za povlačenje nadole"
        ),
    difficulty = mapOf("Beginner" to "Početnik", "Intermediate" to "Srednji nivo", "Advanced" to "Napredni", "Easy" to "Lako", "Hard" to "Teško"),
    split = mapOf("Automatic" to "Automatski", "FullBody" to "Celo telo", "UpperLower" to "Gornji / donji deo tela", "PushPullLegs" to "Potisak / povlačenje / noge", "Hybrid" to "Hibridno", "BroSplit" to "Po mišićnim grupama")
)

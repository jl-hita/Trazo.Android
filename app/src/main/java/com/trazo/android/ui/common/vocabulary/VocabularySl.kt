package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Slovenian. */
internal val VocabularySl = Vocabulary(
    goal = mapOf("GeneralFitness" to "Splošna kondicija", "Hypertrophy" to "Rast mišic", "Strength" to "Moč"),
    experience = mapOf("Beginner" to "Začetnik", "Intermediate" to "Srednje napreden", "Advanced" to "Napreden"),
    limitation = mapOf("Knee" to "Koleno", "Shoulder" to "Rama", "LowerBack" to "Ledveni del hrbta", "Wrist" to "Zapestje", "Elbow" to "Komolec"),
    recovery = mapOf("Unknown" to "Raje ne povem", "Good" to "Dobro spočit", "Partial" to "Delno spočit", "Insufficient" to "Slabo spočit"),
    reason = mapOf("Time" to "Pomanjkanje časa", "Fatigue" to "Utrujenost", "Equipment" to "Oprema", "Motivation" to "Motivacija", "Illness" to "Bolezen", "Other" to "Drugo"),
    phase = mapOf("accumulation" to "Kopičenje", "intensification" to "Intenzifikacija", "deload" to "Razbremenitev", "preparation" to "Priprava"),
    pattern = mapOf(
            "Squat" to "Počep", "Hinge" to "Upogib v kolku (hip hinge)", "HorizontalPush" to "Vodoravni potisk", "VerticalPush" to "Navpični potisk",
            "HorizontalPull" to "Vodoravni poteg", "VerticalPull" to "Navpični poteg", "SingleLeg" to "Enonožno", "Core" to "Core", "Accessory" to "Dopolnilna vaja"
        ),
    muscle = mapOf(
            "Chest" to "Prsi", "Lats" to "Široka hrbtna mišica", "UpperBack" to "Zgornji del hrbta", "Deltoids" to "Ramena", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Podlakti", "Abdominals" to "Trebušne mišice", "Obliques" to "Poševne trebušne mišice", "ErectorSpinae" to "Ledveni del hrbta", "Glutes" to "Zadnjica",
            "Quadriceps" to "Štiriglava stegenska mišica", "Hamstrings" to "Zadnje stegenske mišice", "Calves" to "Meča", "Adductors" to "Primikalke", "Abductors" to "Odmikalke",
            "HipFlexors" to "Upogibalke kolka", "Serratus" to "Sprednja zobata mišica", "TibialisAnterior" to "Sprednja golenska mišica", "RotatorCuff" to "Rotatorna manšeta"
        ),
    equipment = mapOf(
            "barbell" to "Dolga ročka", "dumbbell" to "Kratke ročke", "kettlebell" to "Kettlebell", "cable" to "Škripčni sistem",
            "bench" to "Utežna klop", "squat-rack" to "Stojalo za počepe", "pull-up-bar" to "Drog za zgibe",
            "low-row-bar" to "Nizka palica za veslanje", "bodyweight" to "Lastna teža", "body-only" to "Lastna teža",
            "band" to "Elastika", "bands" to "Elastike", "resistance-band" to "Elastika",
            "ez-bar" to "EZ palica", "trap-bar" to "Šesterokotna palica", "smith-machine" to "Smithov stroj",
            "leg-press-machine" to "Leg press", "hack-squat-machine" to "Stroj za hack počep",
            "leg-curl-machine" to "Stroj za pregib kolen", "leg-extension-machine" to "Stroj za iztegovanje nog",
            "dip-station" to "Bradlja za dips", "captains-chair" to "Stol za dvige nog (captain's chair)",
            "ab-wheel" to "Kolo za trebuh", "step" to "Pliometrična skrinja / step", "stability-ball" to "Žoga za ravnotežje",
            "medicine-ball" to "Medicinka", "exercise-ball" to "Fitnes žoga", "rings" to "Gimnastični obroči",
            "dip-bar" to "Bradlja", "trx" to "TRX", "suspension-trainer" to "Viseči trenažer",
            "rower" to "Veslaški trenažer", "glute-ham-developer" to "Klop GHD",
            "landmine" to "Landmine nastavek za palico", "t-bar-row-handle" to "Ročaj za veslanje s T-palico",
            "preacher-curl-bench" to "Scottova klop", "other" to "Drugo",
            "assisted-pull-up-machine" to "Stroj za podprte zgibe", "chest-press-machine" to "Stroj za potisk s prsi",
            "seated-row-machine" to "Stroj za veslanje sede", "hip-abduction-machine" to "Stroj za odmik nog",
            "hip-adduction-machine" to "Stroj za primik nog", "back-extension-machine" to "Stroj za iztegovanje hrbta",
            "bicep-curl-machine" to "Stroj za pregibe bicepsa", "standing-calf-raise-machine" to "Stroj za meča stoje",
            "seated-calf-raise-machine" to "Stroj za meča sede", "chest-fly-machine" to "Stroj za razpiranja prsi",
            "reverse-fly-machine" to "Stroj za obratna razpiranja", "ab-crunch-machine" to "Stroj za trebušnjake",
            "shoulder-press-machine" to "Stroj za potisk čez glavo", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Stroj za hip thrust",
            "plate-loaded-lateral-raise-machine" to "Stroj za odmike rok s ploščami",
            "lat-pulldown-machine" to "Stroj za poteg navzdol"
        ),
    difficulty = mapOf("Beginner" to "Začetnik", "Intermediate" to "Srednje napreden", "Advanced" to "Napreden", "Easy" to "Lahko", "Hard" to "Težko"),
    split = mapOf("Automatic" to "Samodejno", "FullBody" to "Celotno telo", "UpperLower" to "Zgornji / spodnji del telesa", "PushPullLegs" to "Potisk / poteg / noge", "Hybrid" to "Hibridno", "BroSplit" to "Po mišičnih skupinah")
)

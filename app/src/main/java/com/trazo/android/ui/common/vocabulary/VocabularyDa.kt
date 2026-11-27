package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Danish. */
internal val VocabularyDa = Vocabulary(
    goal = mapOf("GeneralFitness" to "Generel fitness", "Hypertrophy" to "Muskelopbygning", "Strength" to "Styrke"),
    experience = mapOf("Beginner" to "Begynder", "Intermediate" to "Øvet", "Advanced" to "Avanceret"),
    limitation = mapOf("Knee" to "Knæ", "Shoulder" to "Skulder", "LowerBack" to "Lænd", "Wrist" to "Håndled", "Elbow" to "Albue"),
    recovery = mapOf("Unknown" to "Vil helst ikke sige det", "Good" to "Godt restitueret", "Partial" to "Delvist restitueret", "Insufficient" to "Dårligt restitueret"),
    reason = mapOf("Time" to "Tidsmangel", "Fatigue" to "Træthed", "Equipment" to "Udstyr", "Motivation" to "Motivation", "Illness" to "Sygdom", "Other" to "Andet"),
    phase = mapOf("accumulation" to "Opbygning", "intensification" to "Intensivering", "deload" to "Deload", "preparation" to "Forberedelse"),
    pattern = mapOf(
            "Squat" to "Knebøjning", "Hinge" to "Hoftehængsel", "HorizontalPush" to "Vandret skub", "VerticalPush" to "Lodret skub",
            "HorizontalPull" to "Vandret træk", "VerticalPull" to "Lodret træk", "SingleLeg" to "Enbens", "Core" to "Core", "Accessory" to "Hjælpeøvelse"
        ),
    muscle = mapOf(
            "Chest" to "Bryst", "Lats" to "Brede rygmuskler", "UpperBack" to "Øvre ryg", "Deltoids" to "Skuldre", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Underarme", "Abdominals" to "Mave", "Obliques" to "Skrå mavemuskler", "ErectorSpinae" to "Lænd", "Glutes" to "Baller",
            "Quadriceps" to "Lårets forside", "Hamstrings" to "Lårets bagside", "Calves" to "Lægge", "Adductors" to "Adduktorer", "Abductors" to "Abduktorer",
            "HipFlexors" to "Hoftebøjere", "Serratus" to "Savtakmuskel", "TibialisAnterior" to "Forreste skinnebensmuskel", "RotatorCuff" to "Rotatormanchet"
        ),
    equipment = mapOf(
            "barbell" to "Stang", "dumbbell" to "Håndvægte", "kettlebell" to "Kettlebell", "cable" to "Kabelstation",
            "bench" to "Træningsbænk", "squat-rack" to "Squatstativ", "pull-up-bar" to "Pull-up-stang",
            "low-row-bar" to "Lav stang til roning", "bodyweight" to "Kropsvægt", "body-only" to "Kropsvægt",
            "band" to "Elastik", "bands" to "Elastikker", "resistance-band" to "Elastik",
            "ez-bar" to "EZ-stang", "trap-bar" to "Trap bar / hex bar", "smith-machine" to "Smith-maskine",
            "leg-press-machine" to "Benpres", "hack-squat-machine" to "Hack squat-maskine",
            "leg-curl-machine" to "Benbøjningsmaskine", "leg-extension-machine" to "Benstrækmaskine",
            "dip-station" to "Dipsstation / barre", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Mavehjul", "step" to "Plyoboks / step", "stability-ball" to "Træningsbold",
            "medicine-ball" to "Medicinbold", "exercise-ball" to "Gymnastikbold", "rings" to "Gymnastikringe",
            "dip-bar" to "Dipsbarrer", "trx" to "TRX", "suspension-trainer" to "Slyngetræner",
            "rower" to "Romaskine", "glute-ham-developer" to "GHD-bænk",
            "landmine" to "Landmine-holder til stang", "t-bar-row-handle" to "Greb til T-bar-roning",
            "preacher-curl-bench" to "Preacher curl-bænk", "other" to "Andet",
            "assisted-pull-up-machine" to "Assisteret pull-up-maskine", "chest-press-machine" to "Brystpresse-maskine",
            "seated-row-machine" to "Siddende romaskine", "hip-abduction-machine" to "Abduktormaskine",
            "hip-adduction-machine" to "Adduktormaskine", "back-extension-machine" to "Rygstrækmaskine",
            "bicep-curl-machine" to "Bicepscurl-maskine", "standing-calf-raise-machine" to "Stående lægmaskine",
            "seated-calf-raise-machine" to "Siddende lægmaskine", "chest-fly-machine" to "Chest fly-maskine",
            "reverse-fly-machine" to "Omvendt fly-maskine", "ab-crunch-machine" to "Mavemaskine",
            "shoulder-press-machine" to "Skulderpresse-maskine", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust-maskine",
            "plate-loaded-lateral-raise-machine" to "Sidehævningsmaskine med skiver",
            "lat-pulldown-machine" to "Latsmaskine"
        ),
    difficulty = mapOf("Beginner" to "Begynder", "Intermediate" to "Øvet", "Advanced" to "Avanceret", "Easy" to "Let", "Hard" to "Svær"),
    split = mapOf("Automatic" to "Automatisk", "FullBody" to "Hele kroppen", "UpperLower" to "Overkrop / underkrop", "PushPullLegs" to "Skub / træk / ben", "Hybrid" to "Hybrid", "BroSplit" to "Pr. muskelgruppe")
)

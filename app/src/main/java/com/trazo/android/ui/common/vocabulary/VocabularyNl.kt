package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Dutch. */
internal val VocabularyNl = Vocabulary(
    goal = mapOf("GeneralFitness" to "Algemene fitheid", "Hypertrophy" to "Spieropbouw", "Strength" to "Kracht"),
    experience = mapOf("Beginner" to "Beginner", "Intermediate" to "Gemiddeld", "Advanced" to "Gevorderd"),
    limitation = mapOf("Knee" to "Knie", "Shoulder" to "Schouder", "LowerBack" to "Onderrug", "Wrist" to "Pols", "Elbow" to "Elleboog"),
    recovery = mapOf("Unknown" to "Zeg ik liever niet", "Good" to "Goed hersteld", "Partial" to "Gedeeltelijk hersteld", "Insufficient" to "Slecht hersteld"),
    reason = mapOf("Time" to "Tijdgebrek", "Fatigue" to "Vermoeidheid", "Equipment" to "Materiaal", "Motivation" to "Motivatie", "Illness" to "Ziekte", "Other" to "Anders"),
    phase = mapOf("accumulation" to "Opbouw", "intensification" to "Intensivering", "deload" to "Deload", "preparation" to "Voorbereiding"),
    pattern = mapOf(
            "Squat" to "Squat", "Hinge" to "Heupscharnier", "HorizontalPush" to "Horizontaal duwen", "VerticalPush" to "Verticaal duwen",
            "HorizontalPull" to "Horizontaal trekken", "VerticalPull" to "Verticaal trekken", "SingleLeg" to "Eenbenig", "Core" to "Core", "Accessory" to "Hulpoefening"
        ),
    muscle = mapOf(
            "Chest" to "Borst", "Lats" to "Brede rugspier", "UpperBack" to "Bovenrug", "Deltoids" to "Schouders", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Onderarmen", "Abdominals" to "Buikspieren", "Obliques" to "Schuine buikspieren", "ErectorSpinae" to "Onderrug", "Glutes" to "Bilspieren",
            "Quadriceps" to "Quadriceps", "Hamstrings" to "Hamstrings", "Calves" to "Kuiten", "Adductors" to "Adductoren", "Abductors" to "Abductoren",
            "HipFlexors" to "Heupbuigers", "Serratus" to "Gekartelde spier", "TibialisAnterior" to "Voorste scheenbeenspier", "RotatorCuff" to "Rotatorenmanchet"
        ),
    equipment = mapOf(
            "barbell" to "Halterstang", "dumbbell" to "Dumbbells", "kettlebell" to "Kettlebell", "cable" to "Kabelstation",
            "bench" to "Trainingsbank", "squat-rack" to "Squatrek", "pull-up-bar" to "Optrekstang",
            "low-row-bar" to "Lage roeistang", "bodyweight" to "Lichaamsgewicht", "body-only" to "Lichaamsgewicht",
            "band" to "Weerstandsband", "bands" to "Weerstandsbanden", "resistance-band" to "Weerstandsband",
            "ez-bar" to "EZ-curlstang", "trap-bar" to "Trapbar / hexbar", "smith-machine" to "Smithmachine",
            "leg-press-machine" to "Beenpers", "hack-squat-machine" to "Hacksquatmachine",
            "leg-curl-machine" to "Leg curl-machine", "leg-extension-machine" to "Leg extension-machine",
            "dip-station" to "Dipstation / brug", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Buikwiel", "step" to "Plyobox / step", "stability-ball" to "Stabiliteitsbal",
            "medicine-ball" to "Medicijnbal", "exercise-ball" to "Fitnessbal", "rings" to "Gymnastiekringen",
            "dip-bar" to "Dipbeugels", "trx" to "TRX", "suspension-trainer" to "Suspension trainer",
            "rower" to "Roeimachine", "glute-ham-developer" to "GHD-bank",
            "landmine" to "Landmine-opzet voor halterstang", "t-bar-row-handle" to "Handgreep voor T-bar row",
            "preacher-curl-bench" to "Preacher curl-bank", "other" to "Anders",
            "assisted-pull-up-machine" to "Machine voor geassisteerd optrekken", "chest-press-machine" to "Borstpress-machine",
            "seated-row-machine" to "Zittende roeimachine", "hip-abduction-machine" to "Abductormachine",
            "hip-adduction-machine" to "Adductormachine", "back-extension-machine" to "Rugstrekmachine",
            "bicep-curl-machine" to "Bicepscurl-machine", "standing-calf-raise-machine" to "Staande kuitmachine",
            "seated-calf-raise-machine" to "Zittende kuitmachine", "chest-fly-machine" to "Chest fly-machine",
            "reverse-fly-machine" to "Reverse fly-machine", "ab-crunch-machine" to "Buikspiermachine",
            "shoulder-press-machine" to "Schouderpress-machine", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust-machine",
            "plate-loaded-lateral-raise-machine" to "Zijwaartse-opheffingenmachine met platen",
            "lat-pulldown-machine" to "Lat pulldown-machine"
        ),
    difficulty = mapOf("Beginner" to "Beginner", "Intermediate" to "Gemiddeld", "Advanced" to "Gevorderd", "Easy" to "Makkelijk", "Hard" to "Moeilijk"),
    split = mapOf("Automatic" to "Automatisch", "FullBody" to "Full body", "UpperLower" to "Boven- / onderlichaam", "PushPullLegs" to "Duwen / trekken / benen", "Hybrid" to "Hybride", "BroSplit" to "Per spiergroep")
)

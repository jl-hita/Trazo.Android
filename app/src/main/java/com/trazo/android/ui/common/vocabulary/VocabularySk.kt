package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Slovak. */
internal val VocabularySk = Vocabulary(
    goal = mapOf("GeneralFitness" to "Celková kondícia", "Hypertrophy" to "Rast svalov", "Strength" to "Sila"),
    experience = mapOf("Beginner" to "Začiatočník", "Intermediate" to "Mierne pokročilý", "Advanced" to "Pokročilý"),
    limitation = mapOf("Knee" to "Koleno", "Shoulder" to "Rameno", "LowerBack" to "Dolná časť chrbta", "Wrist" to "Zápästie", "Elbow" to "Lakeť"),
    recovery = mapOf("Unknown" to "Radšej nehovoriť", "Good" to "Dobre zregenerovaný", "Partial" to "Čiastočne zregenerovaný", "Insufficient" to "Slabo zregenerovaný"),
    reason = mapOf("Time" to "Nedostatok času", "Fatigue" to "Únava", "Equipment" to "Vybavenie", "Motivation" to "Motivácia", "Illness" to "Choroba", "Other" to "Iné"),
    phase = mapOf("accumulation" to "Akumulácia", "intensification" to "Intenzifikácia", "deload" to "Odľahčenie", "preparation" to "Príprava"),
    pattern = mapOf(
            "Squat" to "Drep", "Hinge" to "Pohyb v bedrách (hip hinge)", "HorizontalPush" to "Horizontálny tlak", "VerticalPush" to "Vertikálny tlak",
            "HorizontalPull" to "Horizontálny ťah", "VerticalPull" to "Vertikálny ťah", "SingleLeg" to "Na jednej nohe", "Core" to "Core", "Accessory" to "Doplnkový cvik"
        ),
    muscle = mapOf(
            "Chest" to "Hrudník", "Lats" to "Široký sval chrbta", "UpperBack" to "Horná časť chrbta", "Deltoids" to "Ramená", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Predlaktia", "Abdominals" to "Brucho", "Obliques" to "Šikmé brušné svaly", "ErectorSpinae" to "Dolná časť chrbta", "Glutes" to "Sedacie svaly",
            "Quadriceps" to "Štvorhlavý sval stehna", "Hamstrings" to "Zadná strana stehien", "Calves" to "Lýtka", "Adductors" to "Priťahovače", "Abductors" to "Odťahovače",
            "HipFlexors" to "Ohýbače bedra", "Serratus" to "Predný zubatý sval", "TibialisAnterior" to "Predný sval holennej kosti", "RotatorCuff" to "Rotátorová manžeta"
        ),
    equipment = mapOf(
            "barbell" to "Činka (tyč)", "dumbbell" to "Jednoručné činky", "kettlebell" to "Kettlebell", "cable" to "Káblová veža",
            "bench" to "Posilňovacia lavica", "squat-rack" to "Stojan na drepy", "pull-up-bar" to "Hrazda",
            "low-row-bar" to "Nízka tyč na veslovanie", "bodyweight" to "Vlastná váha", "body-only" to "Vlastná váha",
            "band" to "Odporová guma", "bands" to "Odporové gumy", "resistance-band" to "Odporová guma",
            "ez-bar" to "EZ tyč", "trap-bar" to "Šesťhranná tyč", "smith-machine" to "Smithov stroj",
            "leg-press-machine" to "Leg press", "hack-squat-machine" to "Stroj na hack drepy",
            "leg-curl-machine" to "Stroj na zadné stehná", "leg-extension-machine" to "Stroj na predné stehná",
            "dip-station" to "Bradlá na kliky", "captains-chair" to "Stolička na dvíhanie nôh (captain's chair)",
            "ab-wheel" to "Koliesko na brucho", "step" to "Plyo box / step", "stability-ball" to "Gymnastická lopta",
            "medicine-ball" to "Medicinbal", "exercise-ball" to "Fitlopta", "rings" to "Gymnastické kruhy",
            "dip-bar" to "Bradlá", "trx" to "TRX", "suspension-trainer" to "Závesný trenažér",
            "rower" to "Veslársky trenažér", "glute-ham-developer" to "Lavica GHD",
            "landmine" to "Landmine na tyč", "t-bar-row-handle" to "Držadlo na T-bar veslovanie",
            "preacher-curl-bench" to "Scottova lavica", "other" to "Iné",
            "assisted-pull-up-machine" to "Stroj na asistované príťahy", "chest-press-machine" to "Stroj na tlaky na prsia",
            "seated-row-machine" to "Stroj na veslovanie v sede", "hip-abduction-machine" to "Stroj na odťahovanie nôh",
            "hip-adduction-machine" to "Stroj na priťahovanie nôh", "back-extension-machine" to "Stroj na hyperextenzie",
            "bicep-curl-machine" to "Stroj na bicepsový zdvih", "standing-calf-raise-machine" to "Stroj na lýtka v stoji",
            "seated-calf-raise-machine" to "Stroj na lýtka v sede", "chest-fly-machine" to "Stroj na rozpažky",
            "reverse-fly-machine" to "Stroj na zadné rozpažky", "ab-crunch-machine" to "Stroj na brucho",
            "shoulder-press-machine" to "Stroj na tlaky na ramená", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Stroj na hip thrust",
            "plate-loaded-lateral-raise-machine" to "Stroj na upažovanie s kotúčmi",
            "lat-pulldown-machine" to "Sťahovanie hornej kladky"
        ),
    difficulty = mapOf("Beginner" to "Začiatočník", "Intermediate" to "Mierne pokročilý", "Advanced" to "Pokročilý", "Easy" to "Ľahký", "Hard" to "Ťažký"),
    split = mapOf("Automatic" to "Automaticky", "FullBody" to "Celé telo", "UpperLower" to "Horná / dolná polovica tela", "PushPullLegs" to "Tlak / ťah / nohy", "Hybrid" to "Hybridné", "BroSplit" to "Podľa svalových partií")
)

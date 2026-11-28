package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Norwegian Bokmål. */
internal val VocabularyNb = Vocabulary(
    goal = mapOf("GeneralFitness" to "Generell form", "Hypertrophy" to "Muskelvekst", "Strength" to "Styrke"),
    experience = mapOf("Beginner" to "Nybegynner", "Intermediate" to "Middels", "Advanced" to "Avansert"),
    limitation = mapOf("Knee" to "Kne", "Shoulder" to "Skulder", "LowerBack" to "Nedre rygg", "Wrist" to "Håndledd", "Elbow" to "Albue"),
    reason = mapOf("Time" to "Tidsmangel", "Fatigue" to "Tretthet", "Equipment" to "Utstyr", "Motivation" to "Motivasjon", "Illness" to "Sykdom", "Other" to "Annet"),
    phase = mapOf("accumulation" to "Oppbygging", "intensification" to "Intensivering", "deload" to "Avlastning", "preparation" to "Forberedelse"),
    pattern = mapOf(
            "Squat" to "Knebøy", "Hinge" to "Hoftehengsel", "HorizontalPush" to "Horisontal press", "VerticalPush" to "Vertikal press",
            "HorizontalPull" to "Horisontalt drag", "VerticalPull" to "Vertikalt drag", "SingleLeg" to "Enbens", "Core" to "Kjerne", "Accessory" to "Tilleggsøvelse"
        ),
    muscle = mapOf(
            "Chest" to "Bryst", "Lats" to "Brede ryggmuskler", "UpperBack" to "Øvre rygg", "Deltoids" to "Skuldre", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Underarmer", "Abdominals" to "Mage", "Obliques" to "Skrå magemuskler", "ErectorSpinae" to "Nedre rygg", "Glutes" to "Setemuskler",
            "Quadriceps" to "Lårets forside", "Hamstrings" to "Lårets bakside", "Calves" to "Legger", "Adductors" to "Adduktorer", "Abductors" to "Abduktorer",
            "HipFlexors" to "Hoftebøyere", "Serratus" to "Sagmuskel", "TibialisAnterior" to "Fremre leggmuskel", "RotatorCuff" to "Rotatormansjett"
        ),
    equipment = mapOf(
            "barbell" to "Stang", "dumbbell" to "Manualer", "kettlebell" to "Kettlebell", "cable" to "Kabelstasjon",
            "bench" to "Treningsbenk", "squat-rack" to "Knebøyrigg", "pull-up-bar" to "Pull-up-stang",
            "low-row-bar" to "Lav roingsstang", "bodyweight" to "Kroppsvekt", "body-only" to "Kroppsvekt",
            "band" to "Strikk", "bands" to "Strikker", "resistance-band" to "Strikk",
            "ez-bar" to "EZ-stang", "trap-bar" to "Trapbar / sekskantstang", "smith-machine" to "Smith-maskin",
            "leg-press-machine" to "Beinpress", "hack-squat-machine" to "Hackknebøy-maskin",
            "leg-curl-machine" to "Lårcurl-maskin", "leg-extension-machine" to "Kneekstensjon-maskin",
            "dip-station" to "Dipsstasjon / barre", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Magehjul", "step" to "Plyoboks / step", "stability-ball" to "Stabilitetsball",
            "medicine-ball" to "Medisinball", "exercise-ball" to "Treningsball", "rings" to "Gymnastikkringer",
            "dip-bar" to "Dipsbarrer", "trx" to "TRX", "suspension-trainer" to "Slyngetrener",
            "rower" to "Romaskin", "glute-ham-developer" to "GHD-benk",
            "landmine" to "Landmine-feste til stang", "t-bar-row-handle" to "Håndtak til T-bar-roing",
            "preacher-curl-bench" to "Preacher curl-benk", "other" to "Annet",
            "assisted-pull-up-machine" to "Assistert pull-up-maskin", "chest-press-machine" to "Brystpress-maskin",
            "seated-row-machine" to "Sittende romaskin", "hip-abduction-machine" to "Abduktormaskin",
            "hip-adduction-machine" to "Adduktormaskin", "back-extension-machine" to "Ryggstrekk-maskin",
            "bicep-curl-machine" to "Bicepscurl-maskin", "standing-calf-raise-machine" to "Stående leggmaskin",
            "seated-calf-raise-machine" to "Sittende leggmaskin", "chest-fly-machine" to "Chest fly-maskin",
            "reverse-fly-machine" to "Omvendt fly-maskin", "ab-crunch-machine" to "Magemaskin",
            "shoulder-press-machine" to "Skulderpress-maskin", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust-maskin",
            "plate-loaded-lateral-raise-machine" to "Sidehev-maskin med skiver",
            "lat-pulldown-machine" to "Nedtrekksmaskin"
        ),
    difficulty = mapOf("Beginner" to "Nybegynner", "Intermediate" to "Middels", "Advanced" to "Avansert", "Easy" to "Lett", "Hard" to "Vanskelig"),
    split = mapOf("Automatic" to "Automatisk", "FullBody" to "Hele kroppen", "UpperLower" to "Overkropp / underkropp", "PushPullLegs" to "Press / drag / ben", "Hybrid" to "Hybrid", "BroSplit" to "Per muskelgruppe")
)

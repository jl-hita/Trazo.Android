package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in German. */
internal val VocabularyDe = Vocabulary(
    goal = mapOf("GeneralFitness" to "Allgemeine Fitness", "Hypertrophy" to "Muskelaufbau", "Strength" to "Kraft"),
    experience = mapOf("Beginner" to "Anfänger", "Intermediate" to "Fortgeschritten", "Advanced" to "Experte"),
    limitation = mapOf("Knee" to "Knie", "Shoulder" to "Schulter", "LowerBack" to "Unterer Rücken", "Wrist" to "Handgelenk", "Elbow" to "Ellbogen"),
    recovery = mapOf("Unknown" to "Möchte ich nicht sagen", "Good" to "Gut erholt", "Partial" to "Teilweise erholt", "Insufficient" to "Schlecht erholt"),
    reason = mapOf("Time" to "Zeitmangel", "Fatigue" to "Erschöpfung", "Equipment" to "Ausrüstung", "Motivation" to "Motivation", "Illness" to "Krankheit", "Other" to "Sonstiges"),
    phase = mapOf("accumulation" to "Aufbau", "intensification" to "Intensivierung", "deload" to "Deload", "preparation" to "Vorbereitung"),
    pattern = mapOf(
            "Squat" to "Kniebeuge", "Hinge" to "Hüftbeuge (Hip Hinge)", "HorizontalPush" to "Horizontales Drücken", "VerticalPush" to "Vertikales Drücken",
            "HorizontalPull" to "Horizontales Ziehen", "VerticalPull" to "Vertikales Ziehen", "SingleLeg" to "Einbeinig", "Core" to "Rumpf", "Accessory" to "Zusatzübung"
        ),
    muscle = mapOf(
            "Chest" to "Brust", "Lats" to "Latissimus", "UpperBack" to "Oberer Rücken", "Deltoids" to "Schultern", "Biceps" to "Bizeps", "Triceps" to "Trizeps",
            "Forearms" to "Unterarme", "Abdominals" to "Bauch", "Obliques" to "Seitliche Bauchmuskeln", "ErectorSpinae" to "Unterer Rücken", "Glutes" to "Gesäß",
            "Quadriceps" to "Quadrizeps", "Hamstrings" to "Beinbeuger", "Calves" to "Waden", "Adductors" to "Adduktoren", "Abductors" to "Abduktoren",
            "HipFlexors" to "Hüftbeuger", "Serratus" to "Sägemuskel", "TibialisAnterior" to "Schienbeinmuskel", "RotatorCuff" to "Rotatorenmanschette"
        ),
    equipment = mapOf(
            "barbell" to "Langhantel", "dumbbell" to "Kurzhanteln", "kettlebell" to "Kettlebell", "cable" to "Kabelzug",
            "bench" to "Hantelbank", "squat-rack" to "Kniebeugenständer", "pull-up-bar" to "Klimmzugstange",
            "low-row-bar" to "Niedrige Stange fürs Rudern", "bodyweight" to "Körpergewicht", "body-only" to "Körpergewicht",
            "band" to "Widerstandsband", "bands" to "Widerstandsbänder", "resistance-band" to "Widerstandsband",
            "ez-bar" to "SZ-Stange", "trap-bar" to "Trap-Bar / Hex-Bar", "smith-machine" to "Smith-Maschine",
            "leg-press-machine" to "Beinpresse", "hack-squat-machine" to "Hackenschmidt-Maschine",
            "leg-curl-machine" to "Beinbeuger-Maschine", "leg-extension-machine" to "Beinstrecker-Maschine",
            "dip-station" to "Dip-Station / Barren", "captains-chair" to "Captain's Chair",
            "ab-wheel" to "Bauchroller", "step" to "Plyo-Box / Step", "stability-ball" to "Gymnastikball",
            "medicine-ball" to "Medizinball", "exercise-ball" to "Fitnessball", "rings" to "Turnringe",
            "dip-bar" to "Dip-Barren", "trx" to "TRX", "suspension-trainer" to "Schlingentrainer",
            "rower" to "Rudergerät", "glute-ham-developer" to "GHD-Gerät (Glute-Ham-Developer)",
            "landmine" to "Landmine-Aufsatz für die Langhantel", "t-bar-row-handle" to "Griff fürs T-Bar-Rudern",
            "preacher-curl-bench" to "Scottbank", "other" to "Sonstiges",
            "assisted-pull-up-machine" to "Klimmzugmaschine mit Unterstützung", "chest-press-machine" to "Brustpresse",
            "seated-row-machine" to "Rudermaschine sitzend", "hip-abduction-machine" to "Abduktoren-Maschine",
            "hip-adduction-machine" to "Adduktoren-Maschine", "back-extension-machine" to "Rückenstrecker-Maschine",
            "bicep-curl-machine" to "Bizepscurl-Maschine", "standing-calf-raise-machine" to "Wadenheber-Maschine stehend",
            "seated-calf-raise-machine" to "Wadenheber-Maschine sitzend", "chest-fly-machine" to "Butterfly-Maschine",
            "reverse-fly-machine" to "Reverse-Butterfly-Maschine", "ab-crunch-machine" to "Bauchmaschine",
            "shoulder-press-machine" to "Schulterpresse", "pec-deck" to "Pec Deck",
            "hip-thrust-machine" to "Hip-Thrust-Maschine",
            "plate-loaded-lateral-raise-machine" to "Seitheben-Maschine mit Gewichtsscheiben",
            "lat-pulldown-machine" to "Latzug-Maschine"
        ),
    difficulty = mapOf("Beginner" to "Anfänger", "Intermediate" to "Fortgeschritten", "Advanced" to "Experte", "Easy" to "Leicht", "Hard" to "Schwer"),
    split = mapOf("Automatic" to "Automatisch", "FullBody" to "Ganzkörper", "UpperLower" to "Oberkörper / Unterkörper", "PushPullLegs" to "Drücken / Ziehen / Beine", "Hybrid" to "Hybrid", "BroSplit" to "Nach Muskelgruppen")
)

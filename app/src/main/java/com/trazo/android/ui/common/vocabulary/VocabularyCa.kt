package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Catalan. */
internal val VocabularyCa = Vocabulary(
    goal = mapOf("GeneralFitness" to "Forma general", "Hypertrophy" to "Hipertròfia", "Strength" to "Força"),
    experience = mapOf("Beginner" to "Principiant", "Intermediate" to "Intermedi", "Advanced" to "Avançat"),
    limitation = mapOf("Knee" to "Genoll", "Shoulder" to "Espatlla", "LowerBack" to "Zona lumbar", "Wrist" to "Canell", "Elbow" to "Colze"),
    recovery = mapOf("Unknown" to "Prefereixo no dir-ho", "Good" to "Ben recuperat", "Partial" to "A mitges", "Insufficient" to "Poc recuperat"),
    reason = mapOf("Time" to "Falta de temps", "Fatigue" to "Fatiga", "Equipment" to "Material", "Motivation" to "Motivació", "Illness" to "Malaltia", "Other" to "Altre"),
    phase = mapOf("accumulation" to "Acumulació", "intensification" to "Intensificació", "deload" to "Descàrrega", "preparation" to "Preparació"),
    pattern = mapOf(
            "Squat" to "Esquat", "Hinge" to "Frontissa de maluc", "HorizontalPush" to "Empenta horitzontal", "VerticalPush" to "Empenta vertical",
            "HorizontalPull" to "Estirada horitzontal", "VerticalPull" to "Estirada vertical", "SingleLeg" to "Unilateral de cama", "Core" to "Core", "Accessory" to "Accessori"
        ),
    muscle = mapOf(
            "Chest" to "Pit", "Lats" to "Dorsals", "UpperBack" to "Esquena alta", "Deltoids" to "Espatlles", "Biceps" to "Bíceps", "Triceps" to "Tríceps",
            "Forearms" to "Avantbraços", "Abdominals" to "Abdominals", "Obliques" to "Oblics", "ErectorSpinae" to "Zona lumbar", "Glutes" to "Glutis",
            "Quadriceps" to "Quàdriceps", "Hamstrings" to "Isquiotibials", "Calves" to "Bessons", "Adductors" to "Adductors", "Abductors" to "Abductors",
            "HipFlexors" to "Flexors del maluc", "Serratus" to "Serrat", "TibialisAnterior" to "Tibial anterior", "RotatorCuff" to "Maniguet rotador"
        ),
    equipment = mapOf(
            "barbell" to "Barra", "dumbbell" to "Manubris", "kettlebell" to "Pesa russa", "cable" to "Politja / estació de cables",
            "bench" to "Banc de musculació", "squat-rack" to "Gàbia o rack d'esquats", "pull-up-bar" to "Barra de dominades",
            "low-row-bar" to "Barra baixa per a rem", "bodyweight" to "Pes corporal", "body-only" to "Pes corporal",
            "band" to "Banda elàstica", "bands" to "Bandes elàstiques", "resistance-band" to "Banda elàstica",
            "ez-bar" to "Barra Z", "trap-bar" to "Barra hexagonal", "smith-machine" to "Màquina Smith",
            "leg-press-machine" to "Premsa de cames", "hack-squat-machine" to "Màquina d'esquat hack",
            "leg-curl-machine" to "Màquina de curl femoral", "leg-extension-machine" to "Màquina d'extensió de cames",
            "dip-station" to "Estació de fons / paral·leles", "captains-chair" to "Cadira romana",
            "ab-wheel" to "Roda abdominal", "step" to "Calaix o plataforma", "stability-ball" to "Fitball / pilota d'estabilitat",
            "medicine-ball" to "Pilota medicinal", "exercise-ball" to "Fitball", "rings" to "Anelles",
            "dip-bar" to "Barres paral·leles", "trx" to "TRX", "suspension-trainer" to "Entrenador de suspensió",
            "rower" to "Màquina de rem", "glute-ham-developer" to "Banc GHD (glute-ham developer)",
            "landmine" to "Ancoratge landmine per a barra", "t-bar-row-handle" to "Nansa per a rem en T",
            "preacher-curl-bench" to "Banc Scott", "other" to "Altre",
            "assisted-pull-up-machine" to "Màquina de dominades assistides", "chest-press-machine" to "Màquina de press de pit",
            "seated-row-machine" to "Màquina de rem assegut", "hip-abduction-machine" to "Màquina d'abductors",
            "hip-adduction-machine" to "Màquina d'adductors", "back-extension-machine" to "Màquina d'extensió lumbar",
            "bicep-curl-machine" to "Màquina de curl de bíceps", "standing-calf-raise-machine" to "Màquina de bessons dret",
            "seated-calf-raise-machine" to "Màquina de bessons assegut", "chest-fly-machine" to "Màquina d'obertures de pit",
            "reverse-fly-machine" to "Màquina d'obertures inverses", "ab-crunch-machine" to "Màquina d'abdominals",
            "shoulder-press-machine" to "Màquina de press d'espatlles", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Màquina de hip thrust",
            "plate-loaded-lateral-raise-machine" to "Màquina d'elevacions laterals",
            "lat-pulldown-machine" to "Màquina de tracció al pit"
        ),
    difficulty = mapOf("Beginner" to "Principiant", "Intermediate" to "Intermedi", "Advanced" to "Avançat", "Easy" to "Fàcil", "Hard" to "Difícil"),
    split = mapOf("Automatic" to "Automàtic", "FullBody" to "Cos complet", "UpperLower" to "Tren superior / inferior", "PushPullLegs" to "Empenta / estirada / cames", "Hybrid" to "Híbrid", "BroSplit" to "Per grups musculars")
)

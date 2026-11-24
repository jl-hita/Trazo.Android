package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Galician. */
internal val VocabularyGl = Vocabulary(
    goal = mapOf("GeneralFitness" to "Forma xeral", "Hypertrophy" to "Hipertrofia", "Strength" to "Forza"),
    experience = mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado"),
    limitation = mapOf("Knee" to "Xeonllo", "Shoulder" to "Ombro", "LowerBack" to "Zona lumbar", "Wrist" to "Pulso", "Elbow" to "Cóbado"),
    recovery = mapOf("Unknown" to "Prefiro non dicilo", "Good" to "Ben recuperado", "Partial" to "A medias", "Insufficient" to "Pouco recuperado"),
    reason = mapOf("Time" to "Falta de tempo", "Fatigue" to "Fatiga", "Equipment" to "Material", "Motivation" to "Motivación", "Illness" to "Enfermidade", "Other" to "Outro"),
    phase = mapOf("accumulation" to "Acumulación", "intensification" to "Intensificación", "deload" to "Descarga", "preparation" to "Preparación"),
    pattern = mapOf(
            "Squat" to "Agachamento", "Hinge" to "Bisagra de cadeira", "HorizontalPush" to "Empuxe horizontal", "VerticalPush" to "Empuxe vertical",
            "HorizontalPull" to "Tirón horizontal", "VerticalPull" to "Tirón vertical", "SingleLeg" to "Unilateral de perna", "Core" to "Core", "Accessory" to "Accesorio"
        ),
    muscle = mapOf(
            "Chest" to "Peito", "Lats" to "Dorsais", "UpperBack" to "Costas altas", "Deltoids" to "Ombros", "Biceps" to "Bíceps", "Triceps" to "Tríceps",
            "Forearms" to "Antebrazos", "Abdominals" to "Abdominais", "Obliques" to "Oblicuos", "ErectorSpinae" to "Zona lumbar", "Glutes" to "Glúteos",
            "Quadriceps" to "Cuádriceps", "Hamstrings" to "Isquiotibiais", "Calves" to "Xemelgos", "Adductors" to "Adutores", "Abductors" to "Abdutores",
            "HipFlexors" to "Flexores da cadeira", "Serratus" to "Serrato", "TibialisAnterior" to "Tibial anterior", "RotatorCuff" to "Manguito rotador"
        ),
    equipment = mapOf(
            "barbell" to "Barra", "dumbbell" to "Mancuernas", "kettlebell" to "Pesa rusa", "cable" to "Polea / estación de cabos",
            "bench" to "Banco de musculación", "squat-rack" to "Gaiola ou rack de agachamentos", "pull-up-bar" to "Barra de dominadas",
            "low-row-bar" to "Barra baixa para remo", "bodyweight" to "Peso corporal", "body-only" to "Peso corporal",
            "band" to "Banda elástica", "bands" to "Bandas elásticas", "resistance-band" to "Banda elástica",
            "ez-bar" to "Barra Z", "trap-bar" to "Barra hexagonal", "smith-machine" to "Máquina Smith",
            "leg-press-machine" to "Prensa de pernas", "hack-squat-machine" to "Máquina de agachamento hack",
            "leg-curl-machine" to "Máquina de curl femoral", "leg-extension-machine" to "Máquina de extensión de pernas",
            "dip-station" to "Estación de fondos / paralelas", "captains-chair" to "Cadeira romana",
            "ab-wheel" to "Roda abdominal", "step" to "Caixón ou plataforma", "stability-ball" to "Fitball / balón de estabilidade",
            "medicine-ball" to "Balón medicinal", "exercise-ball" to "Fitball", "rings" to "Aneis",
            "dip-bar" to "Barras paralelas", "trx" to "TRX", "suspension-trainer" to "Adestrador de suspensión",
            "rower" to "Máquina de remo", "glute-ham-developer" to "Banco GHD",
            "landmine" to "Ancoraxe landmine para barra", "t-bar-row-handle" to "Asa para remo en T",
            "preacher-curl-bench" to "Banco Scott", "other" to "Outro",
            "assisted-pull-up-machine" to "Máquina de dominadas asistidas", "chest-press-machine" to "Máquina de press de peito",
            "seated-row-machine" to "Máquina de remo sentado", "hip-abduction-machine" to "Máquina de abdutores",
            "hip-adduction-machine" to "Máquina de adutores", "back-extension-machine" to "Máquina de extensión lumbar",
            "bicep-curl-machine" to "Máquina de curl de bíceps", "standing-calf-raise-machine" to "Máquina de xemelgos de pé",
            "seated-calf-raise-machine" to "Máquina de xemelgos sentado", "chest-fly-machine" to "Máquina de aperturas de peito",
            "reverse-fly-machine" to "Máquina de aperturas inversas", "ab-crunch-machine" to "Máquina de abdominais",
            "shoulder-press-machine" to "Máquina de press de ombros", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Máquina de hip thrust",
            "plate-loaded-lateral-raise-machine" to "Máquina de elevacións laterais",
            "lat-pulldown-machine" to "Máquina de tracción ao peito"
        ),
    difficulty = mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado", "Easy" to "Doado", "Hard" to "Difícil"),
    split = mapOf("Automatic" to "Automático", "FullBody" to "Corpo completo", "UpperLower" to "Tren superior / inferior", "PushPullLegs" to "Empuxe / tirón / pernas", "BroSplit" to "Por grupos musculares")
)

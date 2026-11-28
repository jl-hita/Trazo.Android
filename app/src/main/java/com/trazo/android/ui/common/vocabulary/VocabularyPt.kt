package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Brazilian Portuguese. */
internal val VocabularyPt = Vocabulary(
    goal = mapOf("GeneralFitness" to "Condicionamento geral", "Hypertrophy" to "Hipertrofia", "Strength" to "Força"),
    experience = mapOf("Beginner" to "Iniciante", "Intermediate" to "Intermediário", "Advanced" to "Avançado"),
    limitation = mapOf("Knee" to "Joelho", "Shoulder" to "Ombro", "LowerBack" to "Lombar", "Wrist" to "Punho", "Elbow" to "Cotovelo"),
    reason = mapOf("Time" to "Falta de tempo", "Fatigue" to "Fadiga", "Equipment" to "Equipamento", "Motivation" to "Motivação", "Illness" to "Doença", "Other" to "Outro"),
    phase = mapOf("accumulation" to "Acúmulo", "intensification" to "Intensificação", "deload" to "Descarga", "preparation" to "Preparação"),
    pattern = mapOf(
            "Squat" to "Agachamento", "Hinge" to "Dobradiça de quadril", "HorizontalPush" to "Empurrar horizontal", "VerticalPush" to "Empurrar vertical",
            "HorizontalPull" to "Puxar horizontal", "VerticalPull" to "Puxar vertical", "SingleLeg" to "Unilateral de perna", "Core" to "Core", "Accessory" to "Acessório"
        ),
    muscle = mapOf(
            "Chest" to "Peitoral", "Lats" to "Dorsais", "UpperBack" to "Costas superiores", "Deltoids" to "Ombros", "Biceps" to "Bíceps", "Triceps" to "Tríceps",
            "Forearms" to "Antebraços", "Abdominals" to "Abdômen", "Obliques" to "Oblíquos", "ErectorSpinae" to "Lombar", "Glutes" to "Glúteos",
            "Quadriceps" to "Quadríceps", "Hamstrings" to "Posteriores de coxa", "Calves" to "Panturrilhas", "Adductors" to "Adutores", "Abductors" to "Abdutores",
            "HipFlexors" to "Flexores do quadril", "Serratus" to "Serrátil", "TibialisAnterior" to "Tibial anterior", "RotatorCuff" to "Manguito rotador"
        ),
    equipment = mapOf(
            "barbell" to "Barra", "dumbbell" to "Halteres", "kettlebell" to "Kettlebell", "cable" to "Polia / estação de cabos",
            "bench" to "Banco de musculação", "squat-rack" to "Rack de agachamento", "pull-up-bar" to "Barra fixa",
            "low-row-bar" to "Barra baixa para remada", "bodyweight" to "Peso corporal", "body-only" to "Peso corporal",
            "band" to "Faixa elástica", "bands" to "Faixas elásticas", "resistance-band" to "Faixa elástica",
            "ez-bar" to "Barra W", "trap-bar" to "Barra hexagonal", "smith-machine" to "Smith (barra guiada)",
            "leg-press-machine" to "Leg press", "hack-squat-machine" to "Máquina de agachamento hack",
            "leg-curl-machine" to "Mesa flexora", "leg-extension-machine" to "Cadeira extensora",
            "dip-station" to "Estação de paralelas / mergulho", "captains-chair" to "Cadeira de elevação de pernas",
            "ab-wheel" to "Roda abdominal", "step" to "Caixa pliométrica / step", "stability-ball" to "Bola de estabilidade",
            "medicine-ball" to "Medicine ball", "exercise-ball" to "Bola suíça", "rings" to "Argolas",
            "dip-bar" to "Barras paralelas", "trx" to "TRX", "suspension-trainer" to "Treinador de suspensão",
            "rower" to "Máquina de remo", "glute-ham-developer" to "Banco GHD",
            "landmine" to "Acessório landmine para barra", "t-bar-row-handle" to "Pegada para remada em T",
            "preacher-curl-bench" to "Banco Scott", "other" to "Outro",
            "assisted-pull-up-machine" to "Máquina de barra assistida", "chest-press-machine" to "Supino na máquina",
            "seated-row-machine" to "Remada sentada na máquina", "hip-abduction-machine" to "Cadeira abdutora",
            "hip-adduction-machine" to "Cadeira adutora", "back-extension-machine" to "Máquina de extensão lombar",
            "bicep-curl-machine" to "Máquina de rosca bíceps", "standing-calf-raise-machine" to "Panturrilha em pé na máquina",
            "seated-calf-raise-machine" to "Panturrilha sentado na máquina", "chest-fly-machine" to "Voador (crucifixo na máquina)",
            "reverse-fly-machine" to "Voador inverso", "ab-crunch-machine" to "Máquina de abdominal",
            "shoulder-press-machine" to "Desenvolvimento na máquina", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Máquina de hip thrust",
            "plate-loaded-lateral-raise-machine" to "Máquina de elevação lateral",
            "lat-pulldown-machine" to "Puxada alta na máquina"
        ),
    difficulty = mapOf("Beginner" to "Iniciante", "Intermediate" to "Intermediário", "Advanced" to "Avançado", "Easy" to "Fácil", "Hard" to "Difícil"),
    split = mapOf("Automatic" to "Automático", "FullBody" to "Corpo inteiro", "UpperLower" to "Superior / inferior", "PushPullLegs" to "Empurrar / puxar / pernas", "Hybrid" to "Híbrido", "BroSplit" to "Por grupos musculares")
)

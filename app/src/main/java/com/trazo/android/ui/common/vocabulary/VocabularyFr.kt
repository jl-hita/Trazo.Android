package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in French. */
internal val VocabularyFr = Vocabulary(
    goal = mapOf("GeneralFitness" to "Forme générale", "Hypertrophy" to "Hypertrophie", "Strength" to "Force"),
    experience = mapOf("Beginner" to "Débutant", "Intermediate" to "Intermédiaire", "Advanced" to "Avancé"),
    limitation = mapOf("Knee" to "Genou", "Shoulder" to "Épaule", "LowerBack" to "Bas du dos", "Wrist" to "Poignet", "Elbow" to "Coude"),
    recovery = mapOf("Unknown" to "Je préfère ne pas le dire", "Good" to "Bien récupéré", "Partial" to "À moitié récupéré", "Insufficient" to "Peu récupéré"),
    reason = mapOf("Time" to "Manque de temps", "Fatigue" to "Fatigue", "Equipment" to "Matériel", "Motivation" to "Motivation", "Illness" to "Maladie", "Other" to "Autre"),
    phase = mapOf("accumulation" to "Accumulation", "intensification" to "Intensification", "deload" to "Décharge", "preparation" to "Préparation"),
    pattern = mapOf(
            "Squat" to "Squat", "Hinge" to "Charnière de hanche", "HorizontalPush" to "Poussée horizontale", "VerticalPush" to "Poussée verticale",
            "HorizontalPull" to "Tirage horizontal", "VerticalPull" to "Tirage vertical", "SingleLeg" to "Unilatéral de jambe", "Core" to "Gainage", "Accessory" to "Accessoire"
        ),
    muscle = mapOf(
            "Chest" to "Pectoraux", "Lats" to "Grands dorsaux", "UpperBack" to "Haut du dos", "Deltoids" to "Épaules", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Avant-bras", "Abdominals" to "Abdominaux", "Obliques" to "Obliques", "ErectorSpinae" to "Bas du dos", "Glutes" to "Fessiers",
            "Quadriceps" to "Quadriceps", "Hamstrings" to "Ischio-jambiers", "Calves" to "Mollets", "Adductors" to "Adducteurs", "Abductors" to "Abducteurs",
            "HipFlexors" to "Fléchisseurs de hanche", "Serratus" to "Dentelé", "TibialisAnterior" to "Tibial antérieur", "RotatorCuff" to "Coiffe des rotateurs"
        ),
    equipment = mapOf(
            "barbell" to "Barre", "dumbbell" to "Haltères", "kettlebell" to "Kettlebell", "cable" to "Poulie / station de câbles",
            "bench" to "Banc de musculation", "squat-rack" to "Cage ou rack à squat", "pull-up-bar" to "Barre de traction",
            "low-row-bar" to "Barre basse pour le rowing", "bodyweight" to "Poids du corps", "body-only" to "Poids du corps",
            "band" to "Bande élastique", "bands" to "Bandes élastiques", "resistance-band" to "Bande élastique",
            "ez-bar" to "Barre EZ", "trap-bar" to "Barre hexagonale", "smith-machine" to "Machine Smith",
            "leg-press-machine" to "Presse à cuisses", "hack-squat-machine" to "Machine de hack squat",
            "leg-curl-machine" to "Machine à leg curl", "leg-extension-machine" to "Machine à leg extension",
            "dip-station" to "Station de dips / barres parallèles", "captains-chair" to "Chaise romaine",
            "ab-wheel" to "Roue abdominale", "step" to "Box pliométrique / step", "stability-ball" to "Ballon de stabilité",
            "medicine-ball" to "Médecine-ball", "exercise-ball" to "Swiss ball", "rings" to "Anneaux de gymnastique",
            "dip-bar" to "Barres de dips", "trx" to "TRX", "suspension-trainer" to "Sangles de suspension",
            "rower" to "Rameur", "glute-ham-developer" to "Banc GHD (glute-ham developer)",
            "landmine" to "Support landmine pour barre", "t-bar-row-handle" to "Poignée de rowing en T",
            "preacher-curl-bench" to "Pupitre à biceps (banc Scott)", "other" to "Autre",
            "assisted-pull-up-machine" to "Machine à tractions assistées", "chest-press-machine" to "Machine de développé couché",
            "seated-row-machine" to "Machine de tirage horizontal assis", "hip-abduction-machine" to "Machine à abducteurs",
            "hip-adduction-machine" to "Machine à adducteurs", "back-extension-machine" to "Banc à lombaires",
            "bicep-curl-machine" to "Machine à curl biceps", "standing-calf-raise-machine" to "Machine à mollets debout",
            "seated-calf-raise-machine" to "Machine à mollets assis", "chest-fly-machine" to "Machine à écartés (pectoraux)",
            "reverse-fly-machine" to "Machine à oiseau", "ab-crunch-machine" to "Machine à abdominaux",
            "shoulder-press-machine" to "Machine de développé épaules", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Machine de hip thrust",
            "plate-loaded-lateral-raise-machine" to "Machine à élévations latérales",
            "lat-pulldown-machine" to "Machine de tirage vertical"
        ),
    difficulty = mapOf("Beginner" to "Débutant", "Intermediate" to "Intermédiaire", "Advanced" to "Avancé", "Easy" to "Facile", "Hard" to "Difficile"),
    split = mapOf("Automatic" to "Automatique", "FullBody" to "Full body", "UpperLower" to "Haut / bas du corps", "PushPullLegs" to "Poussée / tirage / jambes", "Hybrid" to "Hybride", "BroSplit" to "Par groupes musculaires")
)

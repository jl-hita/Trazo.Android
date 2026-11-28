package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in English. */
internal val VocabularyEn = Vocabulary(
    goal = mapOf("GeneralFitness" to "General fitness", "Hypertrophy" to "Muscle growth", "Strength" to "Strength"),
    experience = mapOf("Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced"),
    limitation = mapOf("Knee" to "Knee", "Shoulder" to "Shoulder", "LowerBack" to "Lower back", "Wrist" to "Wrist", "Elbow" to "Elbow"),
    reason = mapOf("Time" to "Lack of time", "Fatigue" to "Fatigue", "Equipment" to "Equipment", "Motivation" to "Motivation", "Illness" to "Illness", "Other" to "Other"),
    phase = mapOf("accumulation" to "Accumulation", "intensification" to "Intensification", "deload" to "Deload", "preparation" to "Preparation"),
    pattern = mapOf(
            "Squat" to "Squat", "Hinge" to "Hip hinge", "HorizontalPush" to "Horizontal push", "VerticalPush" to "Vertical push",
            "HorizontalPull" to "Horizontal pull", "VerticalPull" to "Vertical pull", "SingleLeg" to "Single leg", "Core" to "Core", "Accessory" to "Accessory"
        ),
    muscle = mapOf(
            "Chest" to "Chest", "Lats" to "Lats", "UpperBack" to "Upper back", "Deltoids" to "Shoulders", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Forearms", "Abdominals" to "Abs", "Obliques" to "Obliques", "ErectorSpinae" to "Lower back", "Glutes" to "Glutes",
            "Quadriceps" to "Quadriceps", "Hamstrings" to "Hamstrings", "Calves" to "Calves", "Adductors" to "Adductors", "Abductors" to "Abductors",
            "HipFlexors" to "Hip flexors", "Serratus" to "Serratus", "TibialisAnterior" to "Tibialis anterior", "RotatorCuff" to "Rotator cuff"
        ),
    equipment = mapOf(
            "barbell" to "Barbell", "dumbbell" to "Dumbbells", "kettlebell" to "Kettlebell", "cable" to "Cable station",
            "bench" to "Weight bench", "squat-rack" to "Squat rack", "pull-up-bar" to "Pull-up bar",
            "low-row-bar" to "Low horizontal row bar", "bodyweight" to "Bodyweight", "body-only" to "Bodyweight",
            "band" to "Resistance band", "bands" to "Resistance bands", "resistance-band" to "Resistance band",
            "ez-bar" to "EZ bar", "trap-bar" to "Trap bar / hex bar", "smith-machine" to "Smith machine",
            "leg-press-machine" to "Leg press machine", "hack-squat-machine" to "Hack squat machine",
            "leg-curl-machine" to "Leg curl machine", "leg-extension-machine" to "Leg extension machine",
            "dip-station" to "Dip station / parallel bars", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Ab wheel", "step" to "Plyometric box / step", "stability-ball" to "Stability ball",
            "medicine-ball" to "Medicine ball", "exercise-ball" to "Exercise ball", "rings" to "Gymnastics rings",
            "dip-bar" to "Dip bars", "trx" to "TRX", "suspension-trainer" to "Suspension trainer",
            "rower" to "Rowing machine", "glute-ham-developer" to "Glute-ham developer (GHD)",
            "landmine" to "Barbell landmine attachment", "t-bar-row-handle" to "T-bar row handle",
            "preacher-curl-bench" to "Preacher curl bench", "other" to "Other",
            "assisted-pull-up-machine" to "Assisted pull-up machine", "chest-press-machine" to "Chest press machine",
            "seated-row-machine" to "Seated row machine", "hip-abduction-machine" to "Hip abduction machine",
            "hip-adduction-machine" to "Hip adduction machine", "back-extension-machine" to "Back extension machine",
            "bicep-curl-machine" to "Biceps curl machine", "standing-calf-raise-machine" to "Standing calf raise machine",
            "seated-calf-raise-machine" to "Seated calf raise machine", "chest-fly-machine" to "Chest fly machine",
            "reverse-fly-machine" to "Reverse fly machine", "ab-crunch-machine" to "Ab crunch machine",
            "shoulder-press-machine" to "Shoulder press machine", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust machine",
            "plate-loaded-lateral-raise-machine" to "Plate-loaded lateral raise machine",
            "lat-pulldown-machine" to "Lat pulldown machine"
        ),
    difficulty = mapOf("Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced", "Easy" to "Easy", "Hard" to "Hard"),
    split = mapOf("Automatic" to "Automatic", "FullBody" to "Full body", "UpperLower" to "Upper / lower", "PushPullLegs" to "Push / pull / legs", "Hybrid" to "Hybrid", "BroSplit" to "Body-part split")
)

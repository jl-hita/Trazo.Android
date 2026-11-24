package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Swedish. */
internal val VocabularySv = Vocabulary(
    goal = mapOf("GeneralFitness" to "Allmän fitness", "Hypertrophy" to "Muskeltillväxt", "Strength" to "Styrka"),
    experience = mapOf("Beginner" to "Nybörjare", "Intermediate" to "Medel", "Advanced" to "Avancerad"),
    limitation = mapOf("Knee" to "Knä", "Shoulder" to "Axel", "LowerBack" to "Nedre rygg", "Wrist" to "Handled", "Elbow" to "Armbåge"),
    recovery = mapOf("Unknown" to "Vill helst inte säga", "Good" to "Väl återhämtad", "Partial" to "Delvis återhämtad", "Insufficient" to "Dåligt återhämtad"),
    reason = mapOf("Time" to "Tidsbrist", "Fatigue" to "Trötthet", "Equipment" to "Utrustning", "Motivation" to "Motivation", "Illness" to "Sjukdom", "Other" to "Annat"),
    phase = mapOf("accumulation" to "Uppbyggnad", "intensification" to "Intensifiering", "deload" to "Avlastning", "preparation" to "Förberedelse"),
    pattern = mapOf(
            "Squat" to "Knäböj", "Hinge" to "Höftböj (hip hinge)", "HorizontalPush" to "Horisontell press", "VerticalPush" to "Vertikal press",
            "HorizontalPull" to "Horisontellt drag", "VerticalPull" to "Vertikalt drag", "SingleLeg" to "Enbensövning", "Core" to "Bål", "Accessory" to "Tillbehörsövning"
        ),
    muscle = mapOf(
            "Chest" to "Bröst", "Lats" to "Breda ryggmuskeln", "UpperBack" to "Övre rygg", "Deltoids" to "Axlar", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Underarmar", "Abdominals" to "Mage", "Obliques" to "Sneda magmuskler", "ErectorSpinae" to "Nedre rygg", "Glutes" to "Skinkor",
            "Quadriceps" to "Framsida lår", "Hamstrings" to "Baksida lår", "Calves" to "Vader", "Adductors" to "Adduktorer", "Abductors" to "Abduktorer",
            "HipFlexors" to "Höftböjare", "Serratus" to "Sågtandade muskeln", "TibialisAnterior" to "Främre skenbensmuskeln", "RotatorCuff" to "Rotatorcuffen"
        ),
    equipment = mapOf(
            "barbell" to "Skivstång", "dumbbell" to "Hantlar", "kettlebell" to "Kettlebell", "cable" to "Kabelstation",
            "bench" to "Träningsbänk", "squat-rack" to "Knäböjsställning", "pull-up-bar" to "Chinsstång",
            "low-row-bar" to "Låg stång för rodd", "bodyweight" to "Kroppsvikt", "body-only" to "Kroppsvikt",
            "band" to "Gummiband", "bands" to "Gummiband", "resistance-band" to "Gummiband",
            "ez-bar" to "EZ-stång", "trap-bar" to "Trap bar / hex bar", "smith-machine" to "Smithmaskin",
            "leg-press-machine" to "Benpress", "hack-squat-machine" to "Hack squat-maskin",
            "leg-curl-machine" to "Lårcurlmaskin", "leg-extension-machine" to "Benextensionsmaskin",
            "dip-station" to "Dipsstation / barr", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Magrulle", "step" to "Plyobox / stegbräda", "stability-ball" to "Pilatesboll",
            "medicine-ball" to "Medicinboll", "exercise-ball" to "Träningsboll", "rings" to "Gymnastikringar",
            "dip-bar" to "Dipsbarr", "trx" to "TRX", "suspension-trainer" to "Slingträning",
            "rower" to "Roddmaskin", "glute-ham-developer" to "GHD-bänk",
            "landmine" to "Landmine-fäste för skivstång", "t-bar-row-handle" to "Handtag för T-bar-rodd",
            "preacher-curl-bench" to "Predikstol (preacher bench)", "other" to "Annat",
            "assisted-pull-up-machine" to "Assisterad chinsmaskin", "chest-press-machine" to "Bröstpressmaskin",
            "seated-row-machine" to "Sittande roddmaskin", "hip-abduction-machine" to "Abduktormaskin",
            "hip-adduction-machine" to "Adduktormaskin", "back-extension-machine" to "Ryggresningsmaskin",
            "bicep-curl-machine" to "Bicepscurlmaskin", "standing-calf-raise-machine" to "Stående vadmaskin",
            "seated-calf-raise-machine" to "Sittande vadmaskin", "chest-fly-machine" to "Flyesmaskin för bröst",
            "reverse-fly-machine" to "Omvänd flyesmaskin", "ab-crunch-machine" to "Magmaskin",
            "shoulder-press-machine" to "Axelpressmaskin", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust-maskin",
            "plate-loaded-lateral-raise-machine" to "Sidolyftsmaskin med skivor",
            "lat-pulldown-machine" to "Latsdragsmaskin"
        ),
    difficulty = mapOf("Beginner" to "Nybörjare", "Intermediate" to "Medel", "Advanced" to "Avancerad", "Easy" to "Lätt", "Hard" to "Svår"),
    split = mapOf("Automatic" to "Automatisk", "FullBody" to "Helkropp", "UpperLower" to "Överkropp / underkropp", "PushPullLegs" to "Press / drag / ben", "BroSplit" to "Per muskelgrupp")
)

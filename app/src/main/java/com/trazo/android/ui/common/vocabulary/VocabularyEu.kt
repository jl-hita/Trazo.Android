package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Basque. */
internal val VocabularyEu = Vocabulary(
    goal = mapOf("GeneralFitness" to "Forma fisiko orokorra", "Hypertrophy" to "Hipertrofia", "Strength" to "Indarra"),
    experience = mapOf("Beginner" to "Hasiberria", "Intermediate" to "Tartekoa", "Advanced" to "Aurreratua"),
    limitation = mapOf("Knee" to "Belauna", "Shoulder" to "Sorbalda", "LowerBack" to "Gerri aldea", "Wrist" to "Eskumuturra", "Elbow" to "Ukondoa"),
    reason = mapOf("Time" to "Denbora faltan", "Fatigue" to "Nekea", "Equipment" to "Materiala", "Motivation" to "Motibazioa", "Illness" to "Gaixotasuna", "Other" to "Beste bat"),
    phase = mapOf("accumulation" to "Metaketa", "intensification" to "Intentsifikazioa", "deload" to "Arintze", "preparation" to "Prestakuntza"),
    pattern = mapOf(
            "Squat" to "Squat-a", "Hinge" to "Aldaka-bisagra", "HorizontalPush" to "Bultzada horizontala", "VerticalPush" to "Bultzada bertikala",
            "HorizontalPull" to "Tira horizontala", "VerticalPull" to "Tira bertikala", "SingleLeg" to "Hanka bakarrekoa", "Core" to "Core-a", "Accessory" to "Osagarria"
        ),
    muscle = mapOf(
            "Chest" to "Bularra", "Lats" to "Bizkarreko zabalak", "UpperBack" to "Goiko bizkarra", "Deltoids" to "Sorbaldak", "Biceps" to "Bizepsa", "Triceps" to "Trizepsa",
            "Forearms" to "Besaurreak", "Abdominals" to "Sabelekoak", "Obliques" to "Zeiharrak", "ErectorSpinae" to "Gerri aldea", "Glutes" to "Gluteoak",
            "Quadriceps" to "Koadrizepsa", "Hamstrings" to "Izterraren atzealdea", "Calves" to "Txahalak", "Adductors" to "Aduktoreak", "Abductors" to "Abduktoreak",
            "HipFlexors" to "Aldaka-flexoreak", "Serratus" to "Serratua", "TibialisAnterior" to "Tibial aurrekoa", "RotatorCuff" to "Biraketa-mahuka"
        ),
    equipment = mapOf(
            "barbell" to "Barra", "dumbbell" to "Mankuernak", "kettlebell" to "Pisu errusiarra", "cable" to "Polea / kable-estazioa",
            "bench" to "Gihartze-banku", "squat-rack" to "Squat-errack-a", "pull-up-bar" to "Barra-altxaketetarako barra",
            "low-row-bar" to "Arraun-barra baxua", "bodyweight" to "Gorputz-pisua", "body-only" to "Gorputz-pisua",
            "band" to "Banda elastikoa", "bands" to "Banda elastikoak", "resistance-band" to "Banda elastikoa",
            "ez-bar" to "EZ barra", "trap-bar" to "Barra hexagonala", "smith-machine" to "Smith makina",
            "leg-press-machine" to "Hanka-prentsa", "hack-squat-machine" to "Hack squat makina",
            "leg-curl-machine" to "Hanken flexio-makina", "leg-extension-machine" to "Hanken luzapen-makina",
            "dip-station" to "Hondoratze-estazioa / paraleloak", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Sabel-gurpila", "step" to "Kutxa pliometrikoa / step", "stability-ball" to "Egonkortasun-pilota",
            "medicine-ball" to "Pilota medikoa", "exercise-ball" to "Fitball-a", "rings" to "Gimnasia-eraztunak",
            "dip-bar" to "Barra paraleloak", "trx" to "TRX", "suspension-trainer" to "Esekidura-entrenagailua",
            "rower" to "Arraun-makina", "glute-ham-developer" to "GHD banku",
            "landmine" to "Landmine ainguraketa barrarentzat", "t-bar-row-handle" to "T arraunerako heldulekua",
            "preacher-curl-bench" to "Scott banku", "other" to "Beste bat",
            "assisted-pull-up-machine" to "Barra-altxaketa lagundutako makina", "chest-press-machine" to "Bular-prentsa makina",
            "seated-row-machine" to "Eserita arraunerako makina", "hip-abduction-machine" to "Abduktore-makina",
            "hip-adduction-machine" to "Aduktore-makina", "back-extension-machine" to "Gerri-luzapen makina",
            "bicep-curl-machine" to "Bizeps-curl makina", "standing-calf-raise-machine" to "Zutik txahal-makina",
            "seated-calf-raise-machine" to "Eserita txahal-makina", "chest-fly-machine" to "Bular-irekiera makina",
            "reverse-fly-machine" to "Irekiera inbertsoko makina", "ab-crunch-machine" to "Sabel-makina",
            "shoulder-press-machine" to "Sorbalda-prentsa makina", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust makina",
            "plate-loaded-lateral-raise-machine" to "Alboko altxaketa-makina (diskoekin)",
            "lat-pulldown-machine" to "Tira-makina bularrerantz"
        ),
    difficulty = mapOf("Beginner" to "Hasiberria", "Intermediate" to "Tartekoa", "Advanced" to "Aurreratua", "Easy" to "Erraza", "Hard" to "Zaila"),
    split = mapOf("Automatic" to "Automatikoa", "FullBody" to "Gorputz osoa", "UpperLower" to "Goiko / beheko gorputza", "PushPullLegs" to "Bultzatu / tiratu / hankak", "Hybrid" to "Hibridoa", "BroSplit" to "Giharte-taldeka")
)

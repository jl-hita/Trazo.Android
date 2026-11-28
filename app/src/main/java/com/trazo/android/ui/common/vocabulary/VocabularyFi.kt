package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Finnish. */
internal val VocabularyFi = Vocabulary(
    goal = mapOf("GeneralFitness" to "Yleiskunto", "Hypertrophy" to "Lihasmassan kasvatus", "Strength" to "Voima"),
    experience = mapOf("Beginner" to "Aloittelija", "Intermediate" to "Keskitaso", "Advanced" to "Edistynyt"),
    limitation = mapOf("Knee" to "Polvi", "Shoulder" to "Olkapää", "LowerBack" to "Alaselkä", "Wrist" to "Ranne", "Elbow" to "Kyynärpää"),
    reason = mapOf("Time" to "Ajanpuute", "Fatigue" to "Väsymys", "Equipment" to "Välineet", "Motivation" to "Motivaatio", "Illness" to "Sairaus", "Other" to "Muu"),
    phase = mapOf("accumulation" to "Kasvatusvaihe", "intensification" to "Tehostusvaihe", "deload" to "Kevennys", "preparation" to "Valmistelu"),
    pattern = mapOf(
            "Squat" to "Kyykky", "Hinge" to "Lonkkavipu", "HorizontalPush" to "Vaakatyöntö", "VerticalPush" to "Pystytyöntö",
            "HorizontalPull" to "Vaakaveto", "VerticalPull" to "Pystyveto", "SingleLeg" to "Yhden jalan liike", "Core" to "Keskivartalo", "Accessory" to "Lisäliike"
        ),
    muscle = mapOf(
            "Chest" to "Rinta", "Lats" to "Leveä selkälihas", "UpperBack" to "Yläselkä", "Deltoids" to "Hartiat", "Biceps" to "Hauis", "Triceps" to "Ojentajalihas",
            "Forearms" to "Kyynärvarret", "Abdominals" to "Vatsalihakset", "Obliques" to "Vinot vatsalihakset", "ErectorSpinae" to "Alaselkä", "Glutes" to "Pakarat",
            "Quadriceps" to "Nelipäinen reisilihas", "Hamstrings" to "Reiden takaosa", "Calves" to "Pohkeet", "Adductors" to "Lähentäjät", "Abductors" to "Loitontajat",
            "HipFlexors" to "Lonkankoukistajat", "Serratus" to "Etuhammaslihas", "TibialisAnterior" to "Sääriluun etulihas", "RotatorCuff" to "Pyörittäjäkalvosin"
        ),
    equipment = mapOf(
            "barbell" to "Tanko", "dumbbell" to "Käsipainot", "kettlebell" to "Kahvakuula", "cable" to "Kaapelilaite",
            "bench" to "Treenipenkki", "squat-rack" to "Kyykkyteline", "pull-up-bar" to "Leuanvetotanko",
            "low-row-bar" to "Matala soututanko", "bodyweight" to "Kehonpaino", "body-only" to "Kehonpaino",
            "band" to "Vastuskuminauha", "bands" to "Vastuskuminauhat", "resistance-band" to "Vastuskuminauha",
            "ez-bar" to "EZ-tanko", "trap-bar" to "Kuusikulmatanko", "smith-machine" to "Smith-laite",
            "leg-press-machine" to "Jalkaprässi", "hack-squat-machine" to "Hack-kyykkylaite",
            "leg-curl-machine" to "Reiden koukistuslaite", "leg-extension-machine" to "Reiden ojennuslaite",
            "dip-station" to "Dippiteline / nojapuut", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Vatsarulla", "step" to "Pliolaatikko / step-lauta", "stability-ball" to "Jumppapallo",
            "medicine-ball" to "Painopallo", "exercise-ball" to "Jumppapallo", "rings" to "Voimistelurenkaat",
            "dip-bar" to "Dippitangot", "trx" to "TRX", "suspension-trainer" to "Ripustettava treenilaite",
            "rower" to "Soutulaite", "glute-ham-developer" to "GHD-penkki",
            "landmine" to "Landmine-kiinnitys tangolle", "t-bar-row-handle" to "T-tankosoudun kahva",
            "preacher-curl-bench" to "Scott-penkki", "other" to "Muu",
            "assisted-pull-up-machine" to "Avustettu leuanvetolaite", "chest-press-machine" to "Rintaprässi",
            "seated-row-machine" to "Istumasoutulaite", "hip-abduction-machine" to "Loitonnuslaite",
            "hip-adduction-machine" to "Lähennyslaite", "back-extension-machine" to "Selänojennuslaite",
            "bicep-curl-machine" to "Hauiskääntölaite", "standing-calf-raise-machine" to "Pohjenousulaite seisten",
            "seated-calf-raise-machine" to "Pohjenousulaite istuen", "chest-fly-machine" to "Rintalihaksen perhoslaite",
            "reverse-fly-machine" to "Käänteinen perhoslaite", "ab-crunch-machine" to "Vatsarutistuslaite",
            "shoulder-press-machine" to "Olkaprässi", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust -laite",
            "plate-loaded-lateral-raise-machine" to "Sivunnostolaite levypainoilla",
            "lat-pulldown-machine" to "Ylätalja"
        ),
    difficulty = mapOf("Beginner" to "Aloittelija", "Intermediate" to "Keskitaso", "Advanced" to "Edistynyt", "Easy" to "Helppo", "Hard" to "Vaikea"),
    split = mapOf("Automatic" to "Automaattinen", "FullBody" to "Koko keho", "UpperLower" to "Ylä- ja alavartalo", "PushPullLegs" to "Työntö / veto / jalat", "Hybrid" to "Hybridi", "BroSplit" to "Lihasryhmittäin")
)

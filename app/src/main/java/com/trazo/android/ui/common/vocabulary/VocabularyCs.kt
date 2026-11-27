package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Czech. */
internal val VocabularyCs = Vocabulary(
    goal = mapOf("GeneralFitness" to "Celková kondice", "Hypertrophy" to "Nárůst svalů", "Strength" to "Síla"),
    experience = mapOf("Beginner" to "Začátečník", "Intermediate" to "Středně pokročilý", "Advanced" to "Pokročilý"),
    limitation = mapOf("Knee" to "Koleno", "Shoulder" to "Rameno", "LowerBack" to "Dolní část zad", "Wrist" to "Zápěstí", "Elbow" to "Loket"),
    recovery = mapOf("Unknown" to "Raději neříkat", "Good" to "Dobře zregenerovaný", "Partial" to "Částečně zregenerovaný", "Insufficient" to "Špatně zregenerovaný"),
    reason = mapOf("Time" to "Nedostatek času", "Fatigue" to "Únava", "Equipment" to "Vybavení", "Motivation" to "Motivace", "Illness" to "Nemoc", "Other" to "Jiné"),
    phase = mapOf("accumulation" to "Akumulace", "intensification" to "Intenzifikace", "deload" to "Odlehčení", "preparation" to "Příprava"),
    pattern = mapOf(
            "Squat" to "Dřep", "Hinge" to "Pohyb v kyčli (hip hinge)", "HorizontalPush" to "Horizontální tlak", "VerticalPush" to "Vertikální tlak",
            "HorizontalPull" to "Horizontální tah", "VerticalPull" to "Vertikální tah", "SingleLeg" to "Jednonožní", "Core" to "Core", "Accessory" to "Doplňkový cvik"
        ),
    muscle = mapOf(
            "Chest" to "Hrudník", "Lats" to "Široký sval zádový", "UpperBack" to "Horní záda", "Deltoids" to "Ramena", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Předloktí", "Abdominals" to "Břicho", "Obliques" to "Šikmé břišní svaly", "ErectorSpinae" to "Dolní část zad", "Glutes" to "Hýždě",
            "Quadriceps" to "Čtyřhlavý sval stehenní", "Hamstrings" to "Zadní strana stehen", "Calves" to "Lýtka", "Adductors" to "Přitahovače", "Abductors" to "Odtahovače",
            "HipFlexors" to "Ohýbače kyčle", "Serratus" to "Sval pilovitý", "TibialisAnterior" to "Přední sval holenní", "RotatorCuff" to "Rotátorová manžeta"
        ),
    equipment = mapOf(
            "barbell" to "Velká činka", "dumbbell" to "Jednoruční činky", "kettlebell" to "Kettlebell", "cable" to "Kabelová věž",
            "bench" to "Posilovací lavice", "squat-rack" to "Stojan na dřepy", "pull-up-bar" to "Hrazda",
            "low-row-bar" to "Nízká tyč na přítahy", "bodyweight" to "Vlastní váha", "body-only" to "Vlastní váha",
            "band" to "Odporová guma", "bands" to "Odporové gumy", "resistance-band" to "Odporová guma",
            "ez-bar" to "EZ tyč", "trap-bar" to "Šestihranná tyč", "smith-machine" to "Smithův stroj",
            "leg-press-machine" to "Leg press", "hack-squat-machine" to "Stroj na hack dřepy",
            "leg-curl-machine" to "Stroj na zadní stehna", "leg-extension-machine" to "Stroj na přední stehna",
            "dip-station" to "Bradla na kliky", "captains-chair" to "Stolice na zvedání nohou (captain's chair)",
            "ab-wheel" to "Kolečko na břicho", "step" to "Plyo box / step", "stability-ball" to "Gymnastický míč",
            "medicine-ball" to "Medicinbal", "exercise-ball" to "Fitball", "rings" to "Gymnastické kruhy",
            "dip-bar" to "Bradla", "trx" to "TRX", "suspension-trainer" to "Závěsný trenažér",
            "rower" to "Veslovací trenažér", "glute-ham-developer" to "Lavice GHD",
            "landmine" to "Landmine na tyč", "t-bar-row-handle" to "Držadlo na T-bar veslování",
            "preacher-curl-bench" to "Scottova lavice", "other" to "Jiné",
            "assisted-pull-up-machine" to "Stroj na asistované přítahy", "chest-press-machine" to "Stroj na tlaky na prsa",
            "seated-row-machine" to "Stroj na veslování vsedě", "hip-abduction-machine" to "Stroj na odtahování nohou",
            "hip-adduction-machine" to "Stroj na přitahování nohou", "back-extension-machine" to "Stroj na hyperextenze",
            "bicep-curl-machine" to "Stroj na bicepsový zdvih", "standing-calf-raise-machine" to "Stroj na lýtka ve stoje",
            "seated-calf-raise-machine" to "Stroj na lýtka vsedě", "chest-fly-machine" to "Stroj na rozpažky",
            "reverse-fly-machine" to "Stroj na zadní rozpažky", "ab-crunch-machine" to "Stroj na břicho",
            "shoulder-press-machine" to "Stroj na tlaky na ramena", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Stroj na hip thrust",
            "plate-loaded-lateral-raise-machine" to "Stroj na upažování s kotouči",
            "lat-pulldown-machine" to "Stahování horní kladky"
        ),
    difficulty = mapOf("Beginner" to "Začátečník", "Intermediate" to "Středně pokročilý", "Advanced" to "Pokročilý", "Easy" to "Snadný", "Hard" to "Těžký"),
    split = mapOf("Automatic" to "Automaticky", "FullBody" to "Celé tělo", "UpperLower" to "Horní / dolní polovina těla", "PushPullLegs" to "Tlak / tah / nohy", "Hybrid" to "Hybridní", "BroSplit" to "Po svalových partiích")
)

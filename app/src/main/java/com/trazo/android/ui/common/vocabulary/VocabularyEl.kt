package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Greek. */
internal val VocabularyEl = Vocabulary(
    goal = mapOf("GeneralFitness" to "Γενική φυσική κατάσταση", "Hypertrophy" to "Μυϊκή ανάπτυξη", "Strength" to "Δύναμη"),
    experience = mapOf("Beginner" to "Αρχάριος", "Intermediate" to "Μέσος", "Advanced" to "Προχωρημένος"),
    limitation = mapOf("Knee" to "Γόνατο", "Shoulder" to "Ώμος", "LowerBack" to "Μέση", "Wrist" to "Καρπός", "Elbow" to "Αγκώνας"),
    reason = mapOf("Time" to "Έλλειψη χρόνου", "Fatigue" to "Κούραση", "Equipment" to "Εξοπλισμός", "Motivation" to "Κίνητρο", "Illness" to "Ασθένεια", "Other" to "Άλλο"),
    phase = mapOf("accumulation" to "Συσσώρευση", "intensification" to "Εντατικοποίηση", "deload" to "Αποφόρτιση", "preparation" to "Προετοιμασία"),
    pattern = mapOf(
            "Squat" to "Squat", "Hinge" to "Μεντεσές ισχίου (hip hinge)", "HorizontalPush" to "Οριζόντια ώθηση", "VerticalPush" to "Κάθετη ώθηση",
            "HorizontalPull" to "Οριζόντιο τράβηγμα", "VerticalPull" to "Κάθετο τράβηγμα", "SingleLeg" to "Με το ένα πόδι", "Core" to "Κορμός (core)", "Accessory" to "Βοηθητική άσκηση"
        ),
    muscle = mapOf(
            "Chest" to "Στήθος", "Lats" to "Πλατύς ραχιαίος", "UpperBack" to "Άνω πλάτη", "Deltoids" to "Ώμοι", "Biceps" to "Δικέφαλοι", "Triceps" to "Τρικέφαλοι",
            "Forearms" to "Πήχεις", "Abdominals" to "Κοιλιακοί", "Obliques" to "Λοξοί κοιλιακοί", "ErectorSpinae" to "Μέση", "Glutes" to "Γλουτοί",
            "Quadriceps" to "Τετρακέφαλος", "Hamstrings" to "Οπίσθιοι μηριαίοι", "Calves" to "Γάμπες", "Adductors" to "Προσαγωγοί", "Abductors" to "Απαγωγοί",
            "HipFlexors" to "Καμπτήρες ισχίου", "Serratus" to "Πρόσθιος οδοντωτός", "TibialisAnterior" to "Πρόσθιος κνημιαίος", "RotatorCuff" to "Στροφικός πετάλος"
        ),
    equipment = mapOf(
            "barbell" to "Μπάρα", "dumbbell" to "Αλτήρες", "kettlebell" to "Kettlebell", "cable" to "Σταθμός με καλώδια",
            "bench" to "Παγκάκι γυμναστικής", "squat-rack" to "Σταντ για squat", "pull-up-bar" to "Μονόζυγο",
            "low-row-bar" to "Χαμηλή μπάρα κωπηλασίας", "bodyweight" to "Βάρος σώματος", "body-only" to "Βάρος σώματος",
            "band" to "Ελαστική ταινία", "bands" to "Ελαστικές ταινίες", "resistance-band" to "Ελαστική ταινία",
            "ez-bar" to "Μπάρα EZ", "trap-bar" to "Εξαγωνική μπάρα", "smith-machine" to "Μηχάνημα Smith",
            "leg-press-machine" to "Πρέσα ποδιών", "hack-squat-machine" to "Μηχάνημα hack squat",
            "leg-curl-machine" to "Μηχάνημα κάμψης ποδιών", "leg-extension-machine" to "Μηχάνημα έκτασης ποδιών",
            "dip-station" to "Δίζυγο", "captains-chair" to "Καρέκλα captain's chair",
            "ab-wheel" to "Τροχός κοιλιακών", "step" to "Πλειομετρικό κουτί / step", "stability-ball" to "Μπάλα ισορροπίας",
            "medicine-ball" to "Μπάλα medicine ball", "exercise-ball" to "Μπάλα γυμναστικής", "rings" to "Κρίκοι γυμναστικής",
            "dip-bar" to "Δίζυγο (barres)", "trx" to "TRX", "suspension-trainer" to "Όργανο αναρτημένης προπόνησης",
            "rower" to "Μηχάνημα κωπηλασίας", "glute-ham-developer" to "Παγκάκι GHD",
            "landmine" to "Προσάρτημα landmine για μπάρα", "t-bar-row-handle" to "Λαβή για κωπηλασία T-bar",
            "preacher-curl-bench" to "Παγκάκι Scott", "other" to "Άλλο",
            "assisted-pull-up-machine" to "Μηχάνημα υποβοηθούμενου μονόζυγου", "chest-press-machine" to "Μηχάνημα πιέσεων στήθους",
            "seated-row-machine" to "Μηχάνημα καθιστής κωπηλασίας", "hip-abduction-machine" to "Μηχάνημα απαγωγών",
            "hip-adduction-machine" to "Μηχάνημα προσαγωγών", "back-extension-machine" to "Μηχάνημα έκτασης πλάτης",
            "bicep-curl-machine" to "Μηχάνημα για δικέφαλους", "standing-calf-raise-machine" to "Μηχάνημα γαμπών όρθιος",
            "seated-calf-raise-machine" to "Μηχάνημα γαμπών καθιστός", "chest-fly-machine" to "Μηχάνημα πεταλούδα",
            "reverse-fly-machine" to "Μηχάνημα αντίστροφη πεταλούδα", "ab-crunch-machine" to "Μηχάνημα κοιλιακών",
            "shoulder-press-machine" to "Μηχάνημα πιέσεων ώμων", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Μηχάνημα hip thrust",
            "plate-loaded-lateral-raise-machine" to "Μηχάνημα πλαϊνών ανυψώσεων με δίσκους",
            "lat-pulldown-machine" to "Μηχάνημα κάθετου τραβήγματος"
        ),
    difficulty = mapOf("Beginner" to "Αρχάριος", "Intermediate" to "Μέσος", "Advanced" to "Προχωρημένος", "Easy" to "Εύκολο", "Hard" to "Δύσκολο"),
    split = mapOf("Automatic" to "Αυτόματο", "FullBody" to "Ολόκληρο σώμα", "UpperLower" to "Άνω / κάτω μέρος σώματος", "PushPullLegs" to "Ώθηση / τράβηγμα / πόδια", "Hybrid" to "Υβριδικό", "BroSplit" to "Ανά μυϊκή ομάδα")
)

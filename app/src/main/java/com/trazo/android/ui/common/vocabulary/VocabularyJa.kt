package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Japanese. */
internal val VocabularyJa = Vocabulary(
    goal = mapOf("GeneralFitness" to "総合的な体力づくり", "Hypertrophy" to "筋肥大", "Strength" to "筋力"),
    experience = mapOf("Beginner" to "初級", "Intermediate" to "中級", "Advanced" to "上級"),
    limitation = mapOf("Knee" to "膝", "Shoulder" to "肩", "LowerBack" to "腰", "Wrist" to "手首", "Elbow" to "肘"),
    reason = mapOf("Time" to "時間がない", "Fatigue" to "疲労", "Equipment" to "器具", "Motivation" to "やる気", "Illness" to "体調不良", "Other" to "その他"),
    phase = mapOf("accumulation" to "蓄積期", "intensification" to "強化期", "deload" to "ディロード", "preparation" to "準備期"),
    pattern = mapOf(
            "Squat" to "スクワット", "Hinge" to "ヒップヒンジ", "HorizontalPush" to "水平プッシュ", "VerticalPush" to "垂直プッシュ",
            "HorizontalPull" to "水平プル", "VerticalPull" to "垂直プル", "SingleLeg" to "片脚", "Core" to "体幹", "Accessory" to "補助種目"
        ),
    muscle = mapOf(
            "Chest" to "胸", "Lats" to "広背筋", "UpperBack" to "上背部", "Deltoids" to "肩", "Biceps" to "上腕二頭筋", "Triceps" to "上腕三頭筋",
            "Forearms" to "前腕", "Abdominals" to "腹筋", "Obliques" to "腹斜筋", "ErectorSpinae" to "腰", "Glutes" to "臀筋",
            "Quadriceps" to "大腿四頭筋", "Hamstrings" to "ハムストリングス", "Calves" to "ふくらはぎ", "Adductors" to "内転筋", "Abductors" to "外転筋",
            "HipFlexors" to "腸腰筋", "Serratus" to "前鋸筋", "TibialisAnterior" to "前脛骨筋", "RotatorCuff" to "ローテーターカフ"
        ),
    equipment = mapOf(
            "barbell" to "バーベル", "dumbbell" to "ダンベル", "kettlebell" to "ケトルベル", "cable" to "ケーブルマシン",
            "bench" to "ウェイトベンチ", "squat-rack" to "スクワットラック", "pull-up-bar" to "懸垂バー",
            "low-row-bar" to "ローロウ用バー", "bodyweight" to "自重", "body-only" to "自重",
            "band" to "トレーニングバンド", "bands" to "トレーニングバンド（複数）", "resistance-band" to "トレーニングバンド",
            "ez-bar" to "EZバー", "trap-bar" to "トラップバー（ヘックスバー）", "smith-machine" to "スミスマシン",
            "leg-press-machine" to "レッグプレスマシン", "hack-squat-machine" to "ハックスクワットマシン",
            "leg-curl-machine" to "レッグカールマシン", "leg-extension-machine" to "レッグエクステンションマシン",
            "dip-station" to "ディップスステーション／平行棒", "captains-chair" to "キャプテンズチェア",
            "ab-wheel" to "アブローラー", "step" to "プライオボックス／ステップ", "stability-ball" to "バランスボール",
            "medicine-ball" to "メディシンボール", "exercise-ball" to "エクササイズボール", "rings" to "体操リング",
            "dip-bar" to "ディップバー", "trx" to "TRX", "suspension-trainer" to "サスペンショントレーナー",
            "rower" to "ローイングマシン", "glute-ham-developer" to "GHDベンチ",
            "landmine" to "バーベル用ランドマイン", "t-bar-row-handle" to "Tバーロウ用ハンドル",
            "preacher-curl-bench" to "プリーチャーカールベンチ", "other" to "その他",
            "assisted-pull-up-machine" to "アシスト懸垂マシン", "chest-press-machine" to "チェストプレスマシン",
            "seated-row-machine" to "シーテッドロウマシン", "hip-abduction-machine" to "ヒップアブダクションマシン",
            "hip-adduction-machine" to "ヒップアダクションマシン", "back-extension-machine" to "バックエクステンションマシン",
            "bicep-curl-machine" to "バイセップスカールマシン", "standing-calf-raise-machine" to "スタンディングカーフレイズマシン",
            "seated-calf-raise-machine" to "シーテッドカーフレイズマシン", "chest-fly-machine" to "チェストフライマシン",
            "reverse-fly-machine" to "リバースフライマシン", "ab-crunch-machine" to "アブクランチマシン",
            "shoulder-press-machine" to "ショルダープレスマシン", "pec-deck" to "ペックデック",
            "hip-thrust-machine" to "ヒップスラストマシン",
            "plate-loaded-lateral-raise-machine" to "プレート式サイドレイズマシン",
            "lat-pulldown-machine" to "ラットプルダウンマシン"
        ),
    difficulty = mapOf("Beginner" to "初級", "Intermediate" to "中級", "Advanced" to "上級", "Easy" to "易しい", "Hard" to "難しい"),
    split = mapOf("Automatic" to "自動", "FullBody" to "全身", "UpperLower" to "上半身／下半身", "PushPullLegs" to "プッシュ／プル／レッグ", "Hybrid" to "ハイブリッド", "BroSplit" to "部位別")
)

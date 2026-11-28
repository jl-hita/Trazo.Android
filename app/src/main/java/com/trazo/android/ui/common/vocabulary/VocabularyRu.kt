package com.trazo.android.ui.common.vocabulary

/** The vocabulary of the API in Russian. */
internal val VocabularyRu = Vocabulary(
    goal = mapOf("GeneralFitness" to "Общая физическая форма", "Hypertrophy" to "Рост мышц", "Strength" to "Сила"),
    experience = mapOf("Beginner" to "Новичок", "Intermediate" to "Средний уровень", "Advanced" to "Продвинутый"),
    limitation = mapOf("Knee" to "Колено", "Shoulder" to "Плечо", "LowerBack" to "Поясница", "Wrist" to "Запястье", "Elbow" to "Локоть"),
    reason = mapOf("Time" to "Нехватка времени", "Fatigue" to "Усталость", "Equipment" to "Оборудование", "Motivation" to "Мотивация", "Illness" to "Болезнь", "Other" to "Другое"),
    phase = mapOf("accumulation" to "Накопление", "intensification" to "Интенсификация", "deload" to "Разгрузка", "preparation" to "Подготовка"),
    pattern = mapOf(
            "Squat" to "Приседание", "Hinge" to "Наклон с отведением таза (hip hinge)", "HorizontalPush" to "Горизонтальный жим", "VerticalPush" to "Вертикальный жим",
            "HorizontalPull" to "Горизонтальная тяга", "VerticalPull" to "Вертикальная тяга", "SingleLeg" to "На одной ноге", "Core" to "Кор", "Accessory" to "Вспомогательное"
        ),
    muscle = mapOf(
            "Chest" to "Грудь", "Lats" to "Широчайшие", "UpperBack" to "Верх спины", "Deltoids" to "Плечи", "Biceps" to "Бицепс", "Triceps" to "Трицепс",
            "Forearms" to "Предплечья", "Abdominals" to "Пресс", "Obliques" to "Косые мышцы живота", "ErectorSpinae" to "Поясница", "Glutes" to "Ягодицы",
            "Quadriceps" to "Квадрицепс", "Hamstrings" to "Бицепс бедра", "Calves" to "Икры", "Adductors" to "Приводящие мышцы", "Abductors" to "Отводящие мышцы",
            "HipFlexors" to "Сгибатели бедра", "Serratus" to "Зубчатая мышца", "TibialisAnterior" to "Передняя большеберцовая мышца", "RotatorCuff" to "Вращательная манжета"
        ),
    equipment = mapOf(
            "barbell" to "Штанга", "dumbbell" to "Гантели", "kettlebell" to "Гиря", "cable" to "Блочная станция",
            "bench" to "Скамья", "squat-rack" to "Стойка для приседаний", "pull-up-bar" to "Турник",
            "low-row-bar" to "Низкий гриф для тяги", "bodyweight" to "Собственный вес", "body-only" to "Собственный вес",
            "band" to "Эспандер-лента", "bands" to "Эспандеры-ленты", "resistance-band" to "Эспандер-лента",
            "ez-bar" to "EZ-гриф", "trap-bar" to "Шестигранный гриф", "smith-machine" to "Машина Смита",
            "leg-press-machine" to "Жим ногами (платформа)", "hack-squat-machine" to "Гакк-машина",
            "leg-curl-machine" to "Сгибание ног (тренажёр)", "leg-extension-machine" to "Разгибание ног (тренажёр)",
            "dip-station" to "Брусья", "captains-chair" to "Тренажёр для подъёма ног",
            "ab-wheel" to "Ролик для пресса", "step" to "Плиометрический бокс / степ", "stability-ball" to "Фитбол",
            "medicine-ball" to "Медбол", "exercise-ball" to "Гимнастический мяч", "rings" to "Гимнастические кольца",
            "dip-bar" to "Брусья", "trx" to "TRX", "suspension-trainer" to "Подвесной тренажёр",
            "rower" to "Гребной тренажёр", "glute-ham-developer" to "Скамья GHD",
            "landmine" to "Крепление landmine для штанги", "t-bar-row-handle" to "Рукоятка для Т-тяги",
            "preacher-curl-bench" to "Скамья Скотта", "other" to "Другое",
            "assisted-pull-up-machine" to "Гравитрон", "chest-press-machine" to "Жим от груди (тренажёр)",
            "seated-row-machine" to "Тяга сидя (тренажёр)", "hip-abduction-machine" to "Тренажёр для отведения бедра",
            "hip-adduction-machine" to "Тренажёр для приведения бедра", "back-extension-machine" to "Гиперэкстензия (тренажёр)",
            "bicep-curl-machine" to "Тренажёр для сгибания на бицепс", "standing-calf-raise-machine" to "Голень стоя (тренажёр)",
            "seated-calf-raise-machine" to "Голень сидя (тренажёр)", "chest-fly-machine" to "Бабочка (тренажёр)",
            "reverse-fly-machine" to "Обратная бабочка", "ab-crunch-machine" to "Тренажёр для пресса",
            "shoulder-press-machine" to "Жим на плечи (тренажёр)", "pec-deck" to "Пек-дек",
            "hip-thrust-machine" to "Тренажёр для ягодичного мостика",
            "plate-loaded-lateral-raise-machine" to "Тренажёр для махов в стороны (с блинами)",
            "lat-pulldown-machine" to "Верхняя тяга (тренажёр)"
        ),
    difficulty = mapOf("Beginner" to "Новичок", "Intermediate" to "Средний уровень", "Advanced" to "Продвинутый", "Easy" to "Лёгкий", "Hard" to "Сложный"),
    split = mapOf("Automatic" to "Автоматически", "FullBody" to "Всё тело", "UpperLower" to "Верх / низ", "PushPullLegs" to "Жим / тяга / ноги", "Hybrid" to "Гибридный", "BroSplit" to "По группам мышц")
)

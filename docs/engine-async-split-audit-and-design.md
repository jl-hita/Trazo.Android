# Motor de planificación: auditoría y diseño del Split asíncrono

Estado: **Implementado** (backend: jl-hita/Trazo #77, #79, #80; Android: este PR). Texto original de las fases 1 y 2: Pensado para vivir en `jl-hita/Trazo/docs/` (backend); está aquí
porque esta sesión solo puede escribir en `Trazo.Android`. Auditado sobre `jl-hita/Trazo` @ `7301a11` (main).


> **Actualización (motor 0.11.0, jl-hita/Trazo `docs/planning-engine.md`, sección "One engine, two scheduling policies"):** el punto 3.2 de este diseño (generar con días sintéticos y reiniciar el split en cada periodo) quedó sustituido. El modo secuencia sigue una rotación continua de sesiones (el periodo solo agrupa sesiones y nunca reinicia el split), admite más combinaciones (cuerpo completo y torso/pierna de 2 a 6, push/pull/legs de 3 a 6, automático de 2 a 6), y el volumen real se mide en una ventana de siete días reales (`rolling` en `GET /plans/current/load`), no por periodo. La app solo cambió en la gráfica de carga, que dice "ciclo" en vez de "semana" cuando `periodKind` es `Rotation`.

---

## 1. Auditoría: qué hace hoy el sistema

### 1.1 Mapa real de los conceptos

| Concepto del encargo | Implementación real |
|---|---|
| `TrainingPlanService` | `Trazo.Infrastructure/Planning/TrainingPlanService.cs` (932 líneas): crea, sirve, registra, adapta y genera bloques |
| `TrainingPlan`, `TrainingMesocycle`, `PlannedSession`, `WorkoutLog` | `Trazo.Domain/Planning/TrainingPlan.cs` (+ `TrainingPlanPhase`, `WorkoutSetLog`, `PlanAdaptation`) |
| `PlanSchedule` | `Trazo.Application/Planning/PlanSchedule.cs`: reglas de calendario puras (semanas de 7 días desde `StartDate`, bloques de ≤ 4 semanas) |
| `AdaptationPlanner` | `Trazo.Application/Planning/AdaptationPlanner.cs`: puro, decide qué cambiar; no persiste |
| `TrainingAutoAdaptationPolicy` | `Trazo.Training/Engine/` (motor compartido con DietoExpress): cooldowns y ventanas (`MaximumFutureSessions = 3`, `DeloadWindowDays = 7`) |
| Generación | `MesocycleGenerator` (adaptador) → `TrainingProgramGenerator` (motor, 1368 líneas). Genera **un mesociclo cada vez**, al llegar su primera semana |
| Descargas | `DeloadPlacement` + `MacrocyclePlanner`: semanas naturales concretas |

El sistema ya separa bien: motor puro (`Trazo.Training`), adaptadores puros (`Trazo.Application`) y orquestación con BBDD
(`TrainingPlanService`). Eso es una buena base: **la solución no necesita tocar el motor compartido** (ver §3.6).

### 1.2 Las diez preguntas

**1. Qué depende de la fecha *prevista* (`PlannedSession.Date` / `WeekNumber`)** — casi todo:
- Qué semana es «la actual»: `PlanSchedule.Position(StartDate, TotalWeeks, Today)`; `GET current/week` y `current/block` salen de ahí.
- **Cuándo se genera el siguiente mesociclo**: `BuildWeekAsync` lo genera cuando `Today` entra en su primera semana, aunque el usuario no haya hecho las sesiones del anterior.
- **Qué sesión se puede registrar**: `WindowCheck` solo admite la semana en curso y la anterior (`session_out_of_window`, 409).
- **Qué sesiones futuras adapta una adaptación**: `Status == Planned && Date > logged.Date`, ordenadas por fecha, `Take(40)`; luego `Take(3)` por acción; la descarga usa `Date <= Today + 7`.
- Arrastre de carga a «la siguiente sesión con ese ejercicio», cambio de ejercicio «en el resto del bloque» (`Date > session.Date`), carga semanal (`GetLoadAsync`), fin del plan (`Today > TotalWeeks` ⇒ `Completed`, **con efecto secundario dentro de un GET**), fases (`Phases[].StartDate/EndDate`) y realimentación muscular (`records` usan `session.Date`).

**2. Qué usa la fecha *real*:** no existe. Solo hay `WorkoutLog.LoggedAt = clock.GetUtcNow()` en el servidor: **el instante del registro**, no el del entrenamiento. Se usa como si fuera la fecha de ejecución para: orden de exposiciones (`OrderBy(LoggedAt)`), cooldowns y ventana de «mantener progresión» (`now`), la carga de referencia al generar bloques, el historial, `Progress` y «última vez» (`LastPerformance`).

**3. Registro con días de retraso** (dentro de la ventana de 2 semanas): el log se guarda con `LoggedAt = hoy`. Consecuencias:
- La sesión «ocurre» hoy para todo el motor: si entrenó el lunes y registra el jueves, y ya registró el martes, el orden cronológico queda **invertido** (el martes pasa a ser anterior).
- Los cooldowns y la retención de progresión empiezan en el registro, no en el entrenamiento.
- El adaptador considera «futuras» las sesiones con `Date > logged.Date` que sigan en `Planned`, **incluidas las pasadas que ya nunca se harán** (el martes y el miércoles olvidados), y `Take(3)` se gasta en ellas: **el ajuste se pierde en silencio** o cae en sesiones inexistentes en la práctica.
- Pasadas 2 semanas, **no hay forma de registrar** (409) ni de cerrar la sesión: se queda `Planned` para siempre.
- Bug de zona horaria colateral: `LastPerformance` formatea `LoggedAt` en UTC (`DateOnly.FromDateTime(...UtcDateTime)`) mientras `Progress` sí usa la zona del usuario; cerca de medianoche difieren.

**4. Registro adelantado:** dentro de la semana en curso funciona y no hay nada que lo impida; fuera (una sesión de la semana siguiente) es 409. La sesión adelantada sigue ocupando su hueco y **nada considera la recuperación** (no existe el concepto).

**5. Fuera de orden:** se permite cualquier sesión `Planned` de la ventana. El orden de las exposiciones es el de registro. Nada garantiza el orden lógico del programa (p. ej. piernas B antes de piernas A) ni lo avisa. El filtro `Date > logged.Date` trata distinto el mismo caso según el día previsto.

**6. Cuál es «la siguiente sesión»:** el backend **no lo determina**. Entrega las sesiones de la semana ordenadas por fecha con su `Status`; la «siguiente» la deduce el cliente. Esto contradice el principio «Android no reconstruye reglas del backend».

**7. Qué sesiones futuras puede tocar una adaptación:** solo `Planned` (las ejecutadas **no** se reescriben: buena propiedad, hay que conservarla), de **mesociclos ya generados**; los futuros aún no existen y nacen con `LoadReferences`. Ver el fallo de las sesiones pasadas olvidadas en el punto 3.

**8. Límites entre semanas y mesociclos:** semana = 7 días contados desde `StartDate` (no desde el lunes); fase = rango de fechas; mesociclo = trozo de ≤ 4 semanas **dentro de una fase**; deload = semanas concretas fijadas al crear el plan (`DeloadPlacement`). Todo se mueve con el reloj: **un usuario que apenas ha entrenado entra igualmente en la semana de descarga y el plan termina por calendario.**

**9. Consecuencias de una secuencia independiente del calendario:** `PlannedSession.Date` es obligatoria; `TrainingDays` son `DayOfWeek` y el generador reparte sesiones por día de la semana; `Phases` y los límites de bloque son fechas; `Position`, `WindowCheck`, `GetLoad`, `ToView` y el arrastre de carga asumen «semana = tiempo». Hay que introducir un **ordinal de programa** como eje y dejar el calendario como proyección opcional.

**10. Qué hay que adaptar:** modelo (WorkoutLog, PlannedSession, fases, plan), `PlanSchedule`/posición, `LogSessionAsync`, `AdaptAsync`, `GenerateMesocycleAsync`, `GetLoad`, vistas y endpoints, Android (`Models.kt`, `Apis.kt`, `PlanScreens.kt`, `BlockScreen.kt`) y el admin web si lista planes. El motor `Trazo.Training` **no**.

### 1.3 Pruebas existentes (`PlansApiTests`, 978 líneas, y suites del motor)
Cubren registro normal, replay, premium/free, descargas en semanas 5 y 9, arrastre de carga y feedback muscular. **No hay ninguna prueba** de: registro tardío o adelantado, fuera de orden, `session_out_of_window`, orden por fecha real, zona horaria/medianoche, sesión omitida, ni de que una adaptación no toque sesiones pasadas. El comportamiento actual en esos casos **no está especificado ni protegido**.

### 1.4 Lo que ya está bien y se conserva
Índice único `WorkoutLog.PlannedSessionId` + `SameLog` (idempotencia por sesión); adaptaciones solo sobre `Planned`; generación por bloques con índice único `(PlanId, Sequence)`; `PrescriptionJson` como instantánea de lo prescrito; auditoría en `PlanAdaptation`; cooldowns y límites del motor.

### 1.5 Riesgos detectados (por prioridad)
1. Pérdida silenciosa de adaptaciones por sesiones pasadas olvidadas (§1.2-3).
2. Orden de exposiciones corrupto con registros tardíos ⇒ progresión de carga calculada sobre el historial equivocado.
3. Imposible registrar tras 2 semanas ⇒ el usuario pierde entrenamientos reales.
4. Descargas y fin de plan por reloj, no por progreso.
5. GET con efectos secundarios (generar bloque, cerrar plan).
6. Fecha de «última vez» en UTC.

---

## 2. Principio: planificado ≠ ejecutado ≠ qué viene después

| Capa | Dato | Dónde vive |
|---|---|---|
| Planificación | identidad (`PlannedSession.Id`), **`SequenceIndex`** (posición en el programa), ordinal de periodo (`WeekNumber`, ahora «periodo»), mesociclo/fase, `ScheduledDate?` (solo modo calendario), `PrescriptionJson` original | `PlannedSession`, `TrainingMesocycle`, `TrainingPlanPhase` |
| Ejecución | **`PerformedAt`** (instante real), **`PerformedOn`** + zona (día local), **`RegisteredAt`** (= el actual `LoggedAt`), sets, RIR, resultado, recuperación | `WorkoutLog` |
| Progresión | posición real (qué sesiones están resueltas), historial **ordenado por `PerformedAt`**, fatiga/recuperación, qué viene | calculado, no almacenado (salvo `PlanAdaptation`) |

Regla temporal única, en ambos modos: **la cronología de la evidencia es `PerformedAt`; la identidad y el orden lógico del programa son `SequenceIndex`; la fecha prevista es solo una sugerencia del modo calendario.** Nada compara fechas previstas para decidir qué se modifica.

---

## 3. Diseño

### 3.1 Modelo de datos (aditivo, con defaults)

`TrainingPlan`
- `SchedulingMode` (`Calendar` por defecto | `Sequence`).
- `SessionsPerPeriod` (nº de sesiones de un periodo; en `Calendar` = `TrainingDays.Length`).

`TrainingPlanPhase`: `StartPeriod`, `EndPeriod` (int). Las fechas pasan a ser una proyección (en `Sequence` quedan nulas).

`PlannedSession`
- `SequenceIndex` (int, único por plan): orden lógico global. Backfill: orden por `(Date, Title)` dentro del plan.
- `Date` pasa a **nullable** (`ScheduledDate` semántico; columna intacta, sin renombrar, para no romper nada).
- `WeekNumber` se conserva con significado «periodo» (= semana en calendario, = ciclo/rotación en secuencia).

`WorkoutLog`
- `PerformedAt` (instante real), `PerformedOn` (fecha local), `TimeZoneId`. Backfill: `PerformedAt = LoggedAt`, `PerformedOn` = fecha de `LoggedAt` en la zona del usuario (o `Europe/Madrid` por defecto).
- `LoggedAt` se conserva y pasa a ser **`RegisteredAt`** a nivel de significado (sin renombrar columna).
- `ClientRequestId` (guid opcional, único por usuario cuando existe) para idempotencia entre dispositivos; la unicidad por `PlannedSessionId` sigue siendo la red de seguridad.

`PlanAdaptation`: `EvidenceAt` (el `PerformedAt` del log que la causó), para que los cooldowns sean cronológicos.

**Planes y registros existentes:** todos quedan en `Calendar`; `SequenceIndex` y `PerformedAt` se rellenan en la migración; ninguna sesión pierde su `Id`; el historial no se toca. Índices nuevos: `(PlanId, SequenceIndex)` único, `(PlanId, PerformedAt)`. Hay que actualizar el snapshot (`ModelSnapshotTests`).

### 3.2 Una abstracción nueva: la línea temporal del plan

Pura, en `Trazo.Application/Planning` (no en el motor):

```
IPlanTimeline
  Position(plan, resolvedSessions, today)      -> PlanPosition(periodo actual, sesión en el periodo, estado)
  NextSession(plan, sessions, history, today)  -> sesión pendiente de menor SequenceIndex (+ disponibilidad)
  AdaptationWindow(plan, sessions, anchor)     -> sesiones pendientes modificables
  ShouldGenerate(plan, position)               -> qué mesociclo hace falta ya
  IsPeriodElapsed(...)                         -> ¿puede avanzar el periodo?
CalendarTimeline : el periodo avanza con el reloj (comportamiento actual)
SequenceTimeline : el periodo avanza al resolver las SessionsPerPeriod sesiones de él
```

`PlanSchedule` se conserva (el calendario es una implementación), generador, selección, prescripción, progresión, `AdaptationPlanner` y `TrainingAutoAdaptationPolicy` **no se duplican**: solo cambia quién responde a «dónde estoy» y «qué sesión toca».

Generación en `Sequence`: se llama al mismo `TrainingProgramGenerator` con días sintéticos (los primeros `SessionsPerPeriod` días) y se descartan las fechas: sale la misma rotación de sesiones, con la misma selección y volumen. `WeekNumber` = periodo.

### 3.3 Política de registro (igual en ambos modos)

Petición: `performedAt?` (por defecto, ahora) y `clientRequestId?`. Validaciones: `performedAt ≤ ahora + 5 min` (sin cap atrás, pero ≥ inicio del plan); sesión de un plan activo y no registrada.

| Caso | Resultado |
|---|---|
| Día previsto / un día después / adelantado | Identidad intacta; `PerformedAt` real; la sesión no cambia de hueco |
| Varios días tarde, o registro tardío | Admitido sin límite de ventana (se elimina `session_out_of_window` en calendario); `RegisteredAt` ≠ `PerformedAt` |
| Fuera de orden | Admitido; se guarda tal cual; el orden de la evidencia es `PerformedAt`; si rompe el orden lógico, la respuesta lleva un **aviso** (no bloquea) |
| Parcial / omitida | Estados actuales; omitir = resolver sin ejecución (cuenta para avanzar el periodo, no como exposición) |
| Reintento / doble envío | Mismo `ClientRequestId` o mismo log ⇒ misma respuesta (`Replayed`) |
| Dos dispositivos a la vez | Gana el índice único; el segundo recibe el replay o 409 si difiere |

**Adaptación cronológicamente correcta:**
1. Historial = logs del plan ordenados por `(PerformedAt, RegisteredAt)`; `AdaptationInput.Now` = `PerformedAt` más reciente (cabeza cronológica), no el reloj.
2. Un log **no cabeza** (hay ejecuciones posteriores) entra en el historial y puede producir consejos, pero **no aplica cambios automáticos** que contradigan decisiones ya tomadas con datos más nuevos; los cooldowns se miden por `EvidenceAt`.
3. Sesiones modificables = pendientes con `SequenceIndex` mayor que el máximo ya resuelto, **no** `Date > logged.Date`. Las pendientes «anteriores» (olvidadas) nunca consumen una acción.
4. Las sesiones resueltas no se reescriben jamás (ya ocurre; se protege con prueba).
5. Los GET dejan de tener efectos secundarios: generar un bloque y cerrar un plan pasan a la primera operación de escritura o a un job idempotente.

### 3.4 Split asíncrono

- Es un modo de primer nivel (`SchedulingMode.Sequence`), con **secuencia lógica propia** (`SequenceIndex`), no un calendario sin días.
- Entrada: nº de sesiones por periodo (2–6) en lugar de días de la semana. El periodo es **una vuelta completa a la rotación**.
- «Siguiente sesión» = pendiente de menor `SequenceIndex`; la fecha no interviene. Entrenar lun-mié-jue-sáb-mar avanza el programa igual.
- Mesociclos y fases son rangos de periodos (`StartPeriod/EndPeriod`); el bloque actual lo marca el progreso, no el reloj. Pocas sesiones hechas ⇒ no hay descarga.
- Descarga: (a) estructural, cuando el progreso alcanza el periodo marcado como `deload` (mismo `DeloadPlacement`, ahora en periodos); (b) reactiva, la actual (`Deload` del `AutoAdaptationPolicy`), con ventana de «próximas N sesiones» en vez de 7 días.
- Inactividad larga: el programa **no avanza**; ver decisión D4.
- El plan termina cuando se resuelve la última sesión, no por fecha (fecha límite opcional: D6).

### 3.5 Recuperación (asesora, no bloqueante)

`RecoveryAdvisor` puro, en `Trazo.Application`, con entradas: exposiciones recientes por ejercicio (series efectivas por músculo primario/secundario, cercanía al fallo vía RIR, carga relativa), `SessionRpe`, `Recovery`, dolor, y las respuestas post-ejercicio ya recogidas (`preFatigue`, `stimulus`), nivel de experiencia y frecuencia de exposición del músculo en el programa.

Salida por sesión candidata: `Ready | Caution | Wait`, `AvailableAt` estimado, y motivos legibles (p. ej. «Pecho y tríceps cargados hace 20 h con 14 series cerca del fallo»).

Modelo: cada músculo acumula «coste de recuperación» = series efectivas ponderadas por proximidad al fallo y experiencia; la ventana crece con el coste y con señales de fatiga y se acorta con buena recuperación declarada y con la frecuencia habitual del programa. El resultado de una sesión = el músculo más cargado entre los que solapan con ella. Se propone alternativa: **otra sesión pendiente del periodo con menor solapamiento**, sin saltar el orden salvo que el usuario lo elija. Los coeficientes son valores de producto, no una guía; quedan en un único fichero con pruebas por escenario (D3).

Aplica a los dos modos (en calendario es solo informativo).

### 3.6 Motor compartido con DietoExpress
`TrainingProgramGenerator`, `…VolumeAllocator`, `…ExerciseSelection` y la política **no cambian**, así que no hay desfase que portar. `IPlanTimeline`, `RecoveryAdvisor` y `DeloadPlacement` van en `Trazo.Application` (adaptador) por el mismo motivo que ya documenta `docs/shared-engine-package.md`. Si en el futuro se quiere compartir `RecoveryAdvisor`, se mueve al paquete compartido.

### 3.7 API (solo aditiva)

- `LogSessionRequest`: `performedAt?`, `clientRequestId?`.
- `PlanView`: `schedulingMode`, `sessionsPerPeriod`.
- `SessionView`: `date` pasa a ser opcional; nuevos `sequenceIndex`, `periodNumber`, `performedAt`, `registeredAt` (en `LogView`).
- **Nuevo** `GET /api/v1/plans/current/next` → `NextSessionView { session, position{periodo, sesión, bloque, fase}, availability{state, availableAt, reasons[], alternatives[]}, overdue[] }`. Android lo muestra tal cual; no calcula nada.
- `week`/`block` se mantienen (en `Sequence` devuelven el periodo actual en orden de programa).
- `PlanProfileInput`: `schedulingMode?` (por defecto `Calendar`) y `sessionsPerPeriod?`.
- Un cliente antiguo seguiría funcionando: campos nuevos ignorados, `date` presente en planes calendario. Los planes `Sequence` solo se ofrecen desde la versión de app que los entiende (`PlanOptions` anuncia los modos soportados).

**Android** (revisado a nivel de archivos, pendiente de lectura detallada en la fase de implementación): `Models.kt` (campos opcionales y `date` nullable), `Apis.kt` (endpoint `/next`, `performedAt`), `PlanScreens.kt`/`BlockScreen.kt` (tarjeta «siguiente sesión», selector de fecha real al registrar, estado de recuperación, creación del plan asíncrono), cola de registros pendientes locales (debe conservar `performedAt` y `clientRequestId` al reintentar: **crítico** para que un registro offline no cambie de fecha al sincronizar), `strings.xml` (es/en).

### 3.8 Plan de implementación (PRs independientes, orden de prioridad del encargo)

1. **Modelo temporal y registro flexible** (backend): `PerformedAt/RegisteredAt/ClientRequestId`, `SequenceIndex`, fases por periodo, migración con backfill, orden por `PerformedAt`, fin de la ventana de 2 semanas, adaptación por orden de programa, GET sin efectos. Pruebas Split tradicional (§4). Cambia el comportamiento solo donde hoy hay errores.
2. **`IPlanTimeline` + `/next` + `RecoveryAdvisor`** (backend) con `CalendarTimeline`.
3. **Split asíncrono** (backend): `SequenceTimeline`, creación, generación por periodos, descargas por progreso, inactividad.
4. **Android**: fecha real al registrar + cola offline, siguiente sesión, recuperación, alta de plan asíncrono.
5. **Documentación** (`planning-engine.md`), runbook de migración y validación.

---

## 4. Pruebas previstas
Todas las del encargo, en tres niveles: unitarias de `CalendarTimeline`, `SequenceTimeline`, `RecoveryAdvisor` y orden cronológico; integración sobre `PlansApiTests` con reloj controlado (ya hay `TimeProvider`) para registro puntual/adelantado/atrasado/varios días/consecutivo/fuera de orden/parcial/omitido/tardío; adaptación tras registro tardío; sesiones resueltas intocables; descarga y avance de mesociclo por progreso; reanudación tras semanas sin entrenar; duplicados, reintentos, concurrencia (dos `LogSessionAsync` simultáneos), zona horaria y medianoche (`PerformedOn` ≠ fecha UTC); migración desde esquema antiguo con datos reales de ejemplo. Una prueba de propiedades: **mismo historial en distinto orden de registro ⇒ misma adaptación**.

---

## 5. Decisiones funcionales (recomendación entre paréntesis)

> **Aprobadas (2026-11-17), todas según la recomendación:** D1, D2, D4 y D5. D3, D6 y D7 se adoptan como criterio razonable y se revisan con datos reales.

- **D1 Alcance Free/Premium.** (Secuencia y siguiente sesión gratis en planes manuales y generados; el `RecoveryAdvisor` y la adaptación automática, Premium, igual que hoy.)
- **D2 Sesiones atrasadas en calendario.** (Nunca se marcan omitidas solas; se pueden registrar sin límite; no consumen adaptaciones; se ofrece «omitir pendientes» al cambiar de semana.)
- **D3 Recuperación.** (Solo aconseja, jamás impide registrar; coeficientes iniciales conservadores, ajustables.)
- **D4 Inactividad.** (`Sequence`: no avanza; tras ≥ 21 días sin entrenar propone sesión de reentrada con −1 serie y +1 RIR. `Calendar`: ofrece «recolocar plan desde hoy» tras una semana sin registros.)
- **D5 Cambiar de modo con un plan activo.** (No: se elige al crear; para cambiar se archiva y se crea otro. Evita reordenar historial.)
- **D6 Fin del plan en `Sequence`.** (Por progreso, sin fecha límite.)
- **D7 Descarga reactiva.** (Se mantiene tal cual; solo cambia la ventana de 7 días a «próximas N sesiones» en `Sequence`.)

## 6. Limitaciones y riesgos conocidos
Los coeficientes de recuperación necesitan validación con datos reales. La migración de `SequenceIndex` en planes ya empezados se ordena por fecha y desempata por título: es determinista pero hay que revisarla con una copia de producción. Un cliente Android antiguo no debe ver planes `Sequence`. Una adaptación de un log no cabeza no actúa: es una decisión de seguridad, no un olvido.

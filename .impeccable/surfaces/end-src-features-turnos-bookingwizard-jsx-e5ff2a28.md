---
version: 1
slug: "end-src-features-turnos-bookingwizard-jsx-e5ff2a28"
primary_target: "fkt-turnos-frontend/src/features/turnos/BookingWizard.jsx"
related_targets: ["fkt-turnos-frontend/src/features/auth/LoginPage.jsx","fkt-turnos-frontend/src/index.css"]
---

# Surface brief: patient booking flow + login (pilot of the app-wide redesign)

Scope: `fkt-turnos-frontend` login (`features/auth/LoginPage.jsx`) and booking wizard (`features/turnos/BookingWizard.jsx`), plus the global tokens and base components in `src/index.css` that every screen inherits. Mode: Operate. Stage 1 of a staged redesign; the user approves the pilot before the other areas and the marketing site follow.

Audience and task: patients of mixed ages, many older, booking from a phone (desktop also supported); the secretaría uses the same base components all day at a PC. Task: pick a specialty, professional, day and time, then confirm. Constraints: Spanish (voseo), WCAG 2.2 AA, 44px targets, 16px+ body text, reduced motion, dark mode kept, brand teal and logo kept, no invented content.

Unresolved: whether the FluidOrb stays on the 404 (kept untouched for now); the marketing site's adoption happens in a later stage.

## Direction contract

THESIS: "Cada grado de movimiento". The app measures progress and time the way a kinesiologist reads a goniometer: graduated arcs and scales with one teal needle. It refuses the category default of a card holding stacked selects and a loose grid of time chips.

OWN-WORLD: clinical white ground with a faint cool tint, deep blue-gray ink, the brand teal as the only measuring color, 1px graduations and tick marks, tabular numerals for every time and count. Type: Atkinson Hyperlegible Next, chosen for older patients' legibility, from regular body weight to bold display. Sheets with hairline borders and soft two-layer shadows; 12px radius; teal focus rings.

STORY: the patient sees exactly where they are in four steps, chooses with big unambiguous targets, sees the time they picked read out large, and confirms with total certainty about what was booked.

FIRST VIEWPORT: booking: a semicircular goniometer arc at the top with four graduated stops (Especialidad, Profesional, Día, Horario) whose needle springs to the current step, the step title large below it, then that step's choices as large rows; the time step is a horizontal graduated day scale with slots as marks and a monumental readout of the selected time beside the confirm button. Login: a centered sheet under a small arc mark whose needle sweeps once on load.

FORM: Goniómetro, position 1 on the ordered list (IMPECCABLE'S PICK, chosen by the user), seed key 54f4ea26. Signature interaction: the needle's spring sweep between steps. Motion grammar: springs for the needle and selections, short exponential ease-out elsewhere, instant under reduced motion.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

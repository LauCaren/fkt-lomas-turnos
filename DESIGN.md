---
name: FKT Lomas
description: "Goniómetro: one visual system for the FKT Lomas appointment app and marketing site. Progress and time read like a kinesiologist's goniometer, with one teal needle."
colors:
  primary: "#186F6C"
  primary-hover: "#125855"
  on-primary: "#FFFFFF"
  teal: "#239A96"
  teal-dark: "#186F6C"
  teal-tint: "#E3F3F2"
  ink: "#1C3240"
  ink-soft: "#3D5566"
  muted: "#5A6E78"
  line: "#DCE7E8"
  line-strong: "#93A9AE"
  bg: "#FFFFFF"
  bg-soft: "#F2F7F7"
  white: "#FFFFFF"
  black: "#13232C"
  red: "#BE3E12"
  red-tint: "#FBEAE4"
  green: "#2E7D32"
  green-tint: "#E8F5E9"
  amber: "#B4740E"
  amber-tint: "#FBF1DF"
  amber-dark: "#97610C"
  header-bg: "rgba(255, 255, 255, 0.86)"
  dark-primary: "#3CC4BE"
  dark-primary-hover: "#63D3CE"
  dark-on-primary: "#06221F"
  dark-teal: "#3CC4BE"
  dark-teal-dark: "#8ADFDA"
  dark-teal-tint: "#133230"
  dark-ink: "#E8F1F1"
  dark-ink-soft: "#B9CACC"
  dark-muted: "#93A7AB"
  dark-line: "#22363D"
  dark-line-strong: "#4F6A70"
  dark-bg: "#13222A"
  dark-bg-soft: "#0C171D"
  dark-white: "#13222A"
  dark-black: "#0B141A"
  dark-red: "#FF8A6B"
  dark-red-tint: "#3A1F16"
  dark-green: "#6FCF73"
  dark-green-tint: "#1C3320"
  dark-amber: "#E4A93A"
  dark-amber-tint: "#3A2E14"
  dark-amber-dark: "#F5C563"
  dark-header-bg: "rgba(12, 23, 29, 0.84)"
typography:
  display:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "clamp(38px, 4.8vw, 64px)"
    fontWeight: 800
    lineHeight: 1.04
    letterSpacing: "-0.035em"
  readout:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "clamp(46px, 9vw, 64px)"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.035em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "clamp(28px, 4vw, 34px)"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  headline-site:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "clamp(34px, 4.6vw, 54px)"
    fontWeight: 800
    lineHeight: 1.12
    letterSpacing: "-0.022em"
  section-site:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "clamp(28px, 3.4vw, 42px)"
    fontWeight: 800
    lineHeight: 1.12
    letterSpacing: "-0.022em"
  title:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "21px"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  option-title:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "17.5px"
    fontWeight: 700
    lineHeight: 1.3
  numeric:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "30px"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.025em"
    fontFeature: "tnum"
  body:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.6
  body-site:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: 1.6
  button:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "16px"
    fontWeight: 700
    lineHeight: 1.2
  label:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "14.5px"
    fontWeight: 700
  badge:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "13px"
    fontWeight: 700
    lineHeight: 1.4
  group-label:
    fontFamily: "Atkinson Hyperlegible Next, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "12px"
    fontWeight: 700
    letterSpacing: "0.06em"
  brand-script:
    fontFamily: "Caveat, cursive"
    fontSize: "15px"
    fontWeight: 600
rounded:
  s: "10px"
  control: "12px"
  control-lg: "14px"
  m: "18px"
  l: "26px"
  pill: "100px"
spacing:
  gutter: "24px"
  gutter-mobile: "16px"
  gutter-site: "28px"
  gutter-site-mobile: "18px"
  field-gap: "18px"
  sheet: "28px"
  section-site: "96px"
  cta-band: "84px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.s}"
    padding: "12px 22px"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-outline:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.s}"
    padding: "12px 22px"
    height: "48px"
  button-outline-hover:
    backgroundColor: "{colors.bg-soft}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.teal-dark}"
    typography: "{typography.button}"
    rounded: "{rounded.s}"
    padding: "12px 12px"
    height: "48px"
  button-ghost-hover:
    backgroundColor: "{colors.teal-tint}"
  button-dark:
    backgroundColor: "{colors.black}"
    textColor: "#FFFFFF"
    rounded: "{rounded.s}"
    height: "48px"
  button-lg:
    rounded: "{rounded.control-lg}"
    padding: "14px 28px"
    height: "56px"
  button-sm:
    rounded: "{rounded.s}"
    padding: "6px 14px"
    height: "38px"
  field:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.s}"
    padding: "11px 14px"
    height: "48px"
  field-label:
    textColor: "{colors.ink-soft}"
    typography: "{typography.label}"
  sheet:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.m}"
    padding: "28px"
  sheet-large:
    backgroundColor: "{colors.white}"
    rounded: "{rounded.l}"
    padding: "20px"
  modal:
    backgroundColor: "{colors.white}"
    rounded: "{rounded.l}"
    padding: "28px"
    width: "460px"
  badge-disponible:
    backgroundColor: "{colors.green-tint}"
    textColor: "{colors.green}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  badge-reservado:
    backgroundColor: "color-mix(in srgb, #1C3240 8%, #FFFFFF)"
    textColor: "{colors.ink-soft}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  badge-confirmado:
    backgroundColor: "{colors.teal-tint}"
    textColor: "{colors.teal-dark}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  badge-pendiente:
    backgroundColor: "{colors.amber-tint}"
    textColor: "{colors.amber-dark}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  badge-cancelado:
    backgroundColor: "{colors.line}"
    textColor: "{colors.ink-soft}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  badge-ausente:
    backgroundColor: "{colors.red-tint}"
    textColor: "{colors.red}"
    typography: "{typography.badge}"
    rounded: "{rounded.pill}"
    padding: "3px 10px 3px 8px"
  filter-chip:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.pill}"
    padding: "6px 14px 6px 12px"
    height: "38px"
  option-row:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.option-title}"
    rounded: "{rounded.m}"
    padding: "14px 18px"
    height: "64px"
  option-row-selected:
    backgroundColor: "{colors.teal-tint}"
  day-cell:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.m}"
    padding: "10px 4px 12px"
    height: "92px"
  day-cell-selected:
    backgroundColor: "{colors.teal-tint}"
  time-slot:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.s}"
    height: "48px"
  time-slot-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
  readout:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.readout}"
    rounded: "{rounded.l}"
    padding: "20px 22px"
  tab:
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.s}"
    padding: "8px 16px"
    height: "40px"
  tab-active:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
  sidebar-item:
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.control}"
    padding: "9px 12px"
    height: "42px"
  sidebar-item-active:
    backgroundColor: "{colors.white}"
    textColor: "{colors.teal-dark}"
  nav-link:
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.pill}"
    padding: "10px 14px"
  nav-link-active:
    backgroundColor: "{colors.teal-tint}"
    textColor: "{colors.teal-dark}"
  alert-error:
    backgroundColor: "{colors.red-tint}"
    textColor: "{colors.red}"
    rounded: "{rounded.s}"
    padding: "13px 16px"
  alert-success:
    backgroundColor: "{colors.green-tint}"
    textColor: "{colors.green}"
    rounded: "{rounded.s}"
    padding: "13px 16px"
  alert-warning:
    backgroundColor: "{colors.amber-tint}"
    textColor: "{colors.amber-dark}"
    rounded: "{rounded.s}"
    padding: "13px 16px"
  camino:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.l}"
    padding: "32px 34px"
  camino-strong:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.l}"
    padding: "32px 34px"
  cta-band:
    backgroundColor: "{colors.black}"
    textColor: "#FFFFFF"
    padding: "84px 0"
---

# Design System: FKT Lomas

## Overview

**Creative North Star: "El Goniómetro: cada grado de movimiento"**

The whole system reads like the instrument a kinesiologist uses to measure a joint angle. Progress, time and occupancy are shown as graduated semicircular arcs and scales with a single teal needle. They are never shown as generic progress bars or loose grids of chips. The React appointment app (`fkt-turnos-frontend/src/index.css`) and the static marketing site (`FKT Pagina/assets/css/site.css`) share the same tokens, the same typeface and the same needle spring, so the clinic reads as one center from the first Google visit to the agenda the secretaría keeps open all day.

The ground is clinical white over a faintly cool grey-green (`bg-soft`), with deep blue-grey ink. Surfaces are paper sheets: a 1px hairline border plus a soft two-layer shadow. Every time, date, count and amount uses tabular figures and is set large when it is the answer the person came for (the booked hour, the day's total). The user chose the type and contrast decisions for older patients, often booking from a phone and sometimes in pain. They mean 16px minimum body text, 48px controls and AA contrast even where the brand teal would fall short. The secretaría's screens use the same pieces in a denser mode, not a separate look.

Motion follows one grammar. A near-critical spring moves the needle and confirms selections. Everything else uses short exponential ease-out. Nothing moves under the cursor on hover, and everything is instant under `prefers-reduced-motion`. The user removed some site effects in the redesign because they worked against older visitors (see Do's and Don'ts).

**Key Characteristics:**
- One measuring color: the brand teal draws arcs, needles, reached ticks and small accents. Fills that carry white text use the deeper teal.
- Graduations as ornament: 1px tick marks, ruler lines and empty arcs appear where other systems would use icons or dividers.
- Paper sheets on a cool ground: a hairline border plus a two-layer ink-tinted shadow, with radii of 10/18/26px.
- One family, Atkinson Hyperlegible Next, from 400 body to 800 readouts, with tabular figures for every number.
- State shown by shape and color together: a filled dot vs a hollow ring, and an icon plus color for attendance.
- Spring for the needle, ease-out for everything else, nothing under reduced motion.

## Colors

The palette is cool and restrained: deep blue-grey ink on white and a pale teal-grey ground, with the brand teal as the only chromatic voice and three muted state hues kept for meaning.

### Primary
- **Deep Clinic Teal** (`primary`, also `teal-dark`): the fill of every primary button, the selected time slot, the selected day in the agenda strip and the strong "Reservar" path on the site. It also serves as the emphasis color for text and links on white, as the focus outline and as the active nav and sidebar label. It reaches 5.9:1 with white text, where the brand teal only reaches 3.3:1. Its hover is the darker **Pressed Teal** (`primary-hover`).
- **Brand Teal / Needle Teal** (`teal`): the FKT brand color from the logo. It draws the measured part of the goniometer arc, the needle's center dot, reached step stops, ruler marks, the evolution-timeline dot, the toast status dot and the reading-progress bar on the site. It is a graphic color that needs only 3:1. It is never used for body text on white and never as a fill behind white text.
- **Teal Mist** (`teal-tint`): the tint for selected rows and day cells, hover washes on ghost buttons and slots, the active nav pill, confirmed and arrived badges, and the booking notice box. Text selection also uses it, with teal-dark text.

### Neutral
- **Deep Blue-Grey Ink** (`ink`): primary text, headings, the needle line and its pivot.
- **Soft Ink** (`ink-soft`): secondary text, field labels, inactive nav and tab labels, and the text of the neutral "Reservado" badge.
- **Muted Slate** (`muted`): hints, captions and empty-state subtitles. It was moved from #6D8089 to this value so it holds 5.0:1 on `bg-soft` and 5.4:1 on white.
- **Hairline** (`line`): sheet borders, row dividers, the base of every goniometer arc and the agenda grid lines.
- **Graduation Grey** (`line-strong`): tick marks, control borders (fields, outline buttons, time slots) and dashed empty-state borders. It holds 3:1 against white so controls read as UI components (WCAG 1.4.11).
- **Clinical White** (`bg`) and **Cool Ground** (`bg-soft`): the app body sits on `bg-soft` and the site body on `bg`. Site sections alternate with `section.soft`.
- **Sheet** (`white`): the surface token for sheets, fields and options. It flips to the dark surface in dark mode, so always use it for surfaces and never a literal white.
- **Night Ink** (`black`): the dark CTA band and footer on the site, tooltips and the dark button.

### State
- **Rust Red** (`red`, `red-tint`): errors, absences (ausente), holidays in the agenda grid, the batch-cancel bar and destructive ghost buttons. It was tuned from #C23F12 to reach 4.63:1 on its tint.
- **Clinic Green** (`green`, `green-tint`): available (disponible), the success alert, the "Confirmado" pill on the booking confirmation and free counts.
- **Amber** (`amber`, `amber-tint`, `amber-dark`): pending review, warnings, sidebar counters and lit review stars. Text on the amber tint must use `amber-dark`, because `amber` reaches only 3.44:1 there.

### Dark mode
Every token has a dark counterpart (the `dark-*` keys). It is applied by `@media (prefers-color-scheme: dark)` on `:root:not([data-theme="light"])` and by `:root[data-theme="dark"]`, with an explicit `data-theme` always winning. Token names keep their role while their values invert: `ink` and `teal-dark` become the lightest tones so they stay legible on `#13222A`. In dark mode `primary` becomes the bright teal with near-black `on-primary` text.

### Named Rules
**The One Needle Rule.** The brand teal (#239A96) is the only measuring color. It draws arcs, needles, reached ticks and small marks, and nothing else gets a second accent hue.

**The Deep Teal Fill Rule.** Any fill that carries white text uses `primary` (#186F6C), never the brand teal. The brand teal with white text fails AA at 16px.

**The Reservado Is Never Red Rule.** For staff a booked slot means occupied, not a problem, so "Reservado" uses the neutral ink-tinted badge with a filled dot. Red is kept for errors, absences and holidays.

**The Shape Plus Color Rule.** No state is shown by color alone. Badges carry a filled dot, and cancelled or absent states use a hollow ring. Attendance buttons pair an icon with their color. The agenda grid marks confirmed slots with an inset green ring and absent slots with an inset red ring.

## Typography

**Display Font:** Atkinson Hyperlegible Next (with system-ui, -apple-system, Segoe UI, sans-serif)
**Body Font:** Atkinson Hyperlegible Next (same family)
**Script (brand only):** Caveat 600, only for the handwritten "Fisio Kinesio Terapia" tagline under the logo

**Character:** a single humanist grotesque designed by the Braille Institute for low vision. Its letterforms never collide (Il1, 0O, 5S), and its zero is slashed so a time like 10:00 can never be misread. It was chosen because many patients are older adults. One family covers body text, headings and monumental numerals, loaded at weights 300–800 plus italic 400 from Google Fonts.

### Hierarchy
- **Display** (800, clamp(38px, 4.8vw, 64px), 1.04, -0.035em): the site home hero headline only.
- **Readout** (800, clamp(46px, 9vw, 64px), 1, -0.035em, tabular): the booked time in the booking readout. Its siblings share the same idea: the confirmation hour (clamp(52px, 11vw, 72px)), the "Mis turnos" hour (40px), the agenda detail hour (40px) and the day-summary total (36px). The minutes suffix drops to about 0.42em in muted.
- **Headline** (700, clamp(28px, 4vw, 34px), 1.15, -0.015em): app page h1. Booking step titles go to clamp(28px, 4.4vw, 36px) and login to clamp(30px, 5vw, 36px).
- **Headline (site)** (800, clamp(34px, 4.6vw, 54px), 1.12, -0.022em): interior page heroes. Section h2 uses clamp(28px, 3.4vw, 42px) and the values list header uses clamp(30px, 3.6vw, 46px).
- **Title** (800, 21px): section titles inside the patient area. Modal titles are 22px. Option row titles are 17.5px/700/1.3.
- **Numeric** (800, 30px, -0.025em, tabular): stat values and day numbers (30px/700) in the day picker.
- **Body** (400, 16px, 1.6): the app body and the minimum anywhere. Site body is 17px. Lead paragraphs under titles are 17–18px in muted. Long text is held to about 46–56ch.
- **Label** (700, 14.5px): field labels, operator-density buttons and chips. Badges are 13px/700. Sidebar group labels are 12px/700, 0.06em, uppercase: a nav group name, not a title kicker.

### Named Rules
**The One Family Rule.** Atkinson Hyperlegible Next sets everything. Caveat appears only in the logo tagline. Playfair Display, Inter and JetBrains Mono were retired in the redesign.

**The Tabular Readout Rule.** Every time, date number, count and amount uses `font-variant-numeric: tabular-nums`, so a value changing from 9 to 10 never shifts the row.

**The Title Stands Alone Rule.** Headings carry no eyebrow or kicker above them and no 01/02 section numbering. Context goes below the title as a subtitle (for example the panel greeting: "Hola, {nombre}" with "panel · fecha" beneath).

## Layout

**App.** The content column is 960px (`--maxw`) with a 24px gutter (16px under 640px). The sticky header always spans max(960px, 1180px) as a three-column grid (logo | nav | actions). Under 860px the nav drops to a second row that scrolls sideways and never wraps. Patient flows are narrow single columns: booking 720px, login 440px (560px for wide forms), confirmation 560px, result screens 460px, "Mis turnos" 720px and kinesiology 760px. Internal panels use a 236px sticky sidebar (top 96px) beside the content with a 36px gap. At 880px the sidebar becomes a horizontal scrolling row of pills under a hairline. "Mi cuenta" sections flow in an auto-fit grid (minmax(360px, 1fr), 24px gap) with no media queries.

**Booking.** The goniometer sits centered at 340px max (280px on mobile), with the four named step stops in a 4-column grid below it. The step title follows 34px later, then the step's choices. Specialty and professional choices are full-width rows in a 10px-gap stack. Days form a 7-column grid of whole cells, 4 columns under 640px. Times form a vertical graduated scale: one row per hour, a 64px hour label (46px on mobile), then four fixed quarter-hour columns (:00 :15 :30 :45), so empty quarters show as a short graduation mark rather than collapsing. On mobile the readout with the confirm button is sticky at the bottom (10px plus the safe area).

**Site.** The container is 1180px with a 28px gutter (18px under 560px). Sections have 96px vertical padding, and section heads are held to 640px with 52px below. The header nav collapses to a burger at 1180px. Recurring compositions: the body map hero (260px figure + 1fr panel, stacked under 760px); "Dos caminos" (two equal panels, 18px gap, one column under 760px); "Lo que encontrás" (5fr/7fr with the header sticky at top 120px, stacked under 860px); service cards (4 → 2 at 960px → 1 at 600px, 18px gaps); numbered process (3 columns on a hairline rail, stacked with a left rule under 800px); contact (1fr 1fr, stacked under 860px).

**Density.** Patient surfaces breathe: 64px option rows and 92px day cells. Operator surfaces tighten with 38px small buttons and chips, 14px-padded agenda rows and a compact grid mode with 24px rows. They keep the same tokens.

## Elevation & Depth

This is a hybrid of hairline and soft shadow. Every sheet has a 1px `line` border and the small two-layer shadow (a contact shadow plus a wide, negative-spread ambient one, both tinted with ink rather than black). Elevation rises one step on hover for clickable sheets and two steps for overlays. Dark mode swaps in deeper black-based shadows. Depth on the site's dark band comes from a faint noise grain and a large ghosted goniometer, not from shadow.

### Shadow Vocabulary
- **Sheet** (`box-shadow: 0 1px 2px rgba(19, 35, 44, .05), 0 6px 18px -6px rgba(19, 35, 44, .08)`): resting sheets, option rows, stat cards, the active tab and the sliding sidebar indicator.
- **Lifted** (`box-shadow: 0 2px 6px rgba(19, 35, 44, .05), 0 18px 40px -12px rgba(19, 35, 44, .16)`): hover on clickable sheets and options, the booking readout, the selected time slot and site photo teasers.
- **Overlay** (`box-shadow: 0 10px 24px rgba(19, 35, 44, .08), 0 36px 80px -20px rgba(19, 35, 44, .28)`): modals, toasts and lightbox images.
- **Focus halo** (`box-shadow: 0 0 0 4px rgba(35, 154, 150, .18)`): fields on focus, always paired with a 2px solid teal-dark outline for keyboard focus.

### Named Rules
**The Paper Sheet Rule.** A surface is a sheet: a 1px hairline border, a two-layer ink-tinted shadow and a 10/18/26px radius. Short lists inside a sheet are hairline-ruled rows, not a card per row. Groupings inside a sheet use a tint block (cool ground or Teal Mist) rather than another bordered, shadowed sheet.

**The Shadow Responds, The Button Stays Rule.** On hover the shadow deepens and the border darkens. Buttons never translate or jump under the cursor, and press feedback is a slight scale (0.975).

## Shapes

Corners are gently curved on a three-step scale: 10px for controls (buttons, fields, time slots, alerts, toasts), 18px for sheets, options and day cells, and 26px for large sheets (the booking readout, "Mis turnos" cards, kinesiology plans, modals, site caminos, service and review cards). In-between values exist for specific controls: 12px for sidebar items, site buttons and body-map links, and 14px for large buttons and the tab track. Nav links, filter chips, badges, the command trigger and the theme toggle are full pills (100px).

The signature silhouette is the semicircle. The goniometer arc uses a round 10px stroke (14px in the compact version) with 1px–2px graduations every 5° and longer ones every 30°. Empty states draw an unmeasured arc, a 56×30px half-ring in `line`, instead of an icon. On the site the same geometry repeats: a graduated ruler under interior page titles, short ruler ticks on the values list, a mini goniometer beside each process step, and an arc that sweeps over the chosen joint on the body map. Borders are 1px hairlines for surfaces, 1.5px for controls, and 1.5px dashed `line-strong` for empty or "nothing yet" containers and the file drop zone. Strokes always use round linecaps.

## Components

### Buttons
Calm and certain: a solid deep-teal fill with no motion under the cursor.
- **Shape:** gently curved (10px in the app; the site uses 12px). Minimum height is 48px. Text is 16px/700 with an 8px icon gap.
- **Primary:** deep teal fill with white text, a subtle 1px contact shadow and an inset top highlight. Hover darkens to pressed teal.
- **Outline:** sheet background, 1.5px graduation-grey border and ink text. Hover darkens the border and washes in the cool ground.
- **Ghost:** transparent with teal-dark text and a Teal Mist wash on hover. The danger variant uses red text with a red-tint wash.
- **Dark:** Night Ink fill with white text (site CTA contexts). On the dark band, outline-light uses a 45% white border.
- **Sizes:** large is 56px, 17px text, 14px radius, used for the final confirm. Small is 38px, 14.5px text, 10px radius, and only on operator screens (agenda, lists).
- **States:** press scales to 0.975 over 0.18s ease-out. Disabled drops to 50% opacity. A 16px spinner in `currentColor` sits inside the button while loading. Site arrow icons nudge 3px right on the spring when hovered.

### Chips and badges
- **Status badge:** a pill with 13px/700 text, a 7px leading dot in `currentColor` and a tinted background. The pairs are: disponible (green), reservado (neutral ink at 8%), confirmado and llegó (teal), pendiente (amber), cancelado (line/ink-soft, hollow ring) and ausente (red, hollow ring).
- **Filter chip:** a 38px pill on the sheet surface with an inset hairline and an 8px dot (graduation grey when neutral, or the state color when it carries a badge class), plus a tabular count in ink. Pressed state shows a 1.5px `currentColor` border, or an ink border for neutral chips.
- **Attendance toggle:** two 38px buttons with an icon and a label. "Llegó" presses to teal-tint/teal-dark and "Ausente" presses to red-tint/red.
- **Site tag pill:** a 44px pill with a 1.5px hairline, 16px/700 text, on the sheet surface.

### Cards / Containers
- **Corner Style:** 18px for standard sheets and 26px for large sheets and modals.
- **Background:** `white` (the sheet token) on `bg-soft` in the app, or on `bg`/`bg-soft` sections on the site.
- **Shadow Strategy:** Sheet at rest and Lifted on hover (see Elevation & Depth). Clickable sheets press to 0.985.
- **Border:** a 1px `line` hairline. Clickable hover moves it to `line-strong`, or to brand teal on options and stat cards.
- **Internal Padding:** 28px for standard sheets (22px/18px on mobile login), 18–22px for large data sheets, and 32px 34px for site caminos.
- **Modal:** 460px wide, 26px radius, Overlay shadow, over a 48% Night Ink veil with a 3px blur. It enters with fade plus scale from 0.95 over 0.24s ease-out from the center, not from the trigger.

### Inputs / Fields
- **Style:** 48px minimum height, 11px 14px padding, 1.5px graduation-grey border, 10px radius, sheet background, 16px text (17px on the site). Labels are 14.5px/700 in soft ink, stacked 7px above the field. Fields sit 18px apart.
- **Hover:** the border darkens to soft ink.
- **Focus:** the border goes teal-dark with the 4px teal focus halo. Keyboard focus adds a 2px solid teal-dark outline at a 2px offset.
- **Error / Disabled:** error text is 14px red below the field. Disabled fields use the cool ground with muted text. Checkboxes are 22px with `accent-color: primary`, and the whole row is the target.
- **File drop:** a 64px dashed zone on the cool ground. When filled it turns solid teal-dark on Teal Mist.

### Navigation
- **App header:** sticky, 72px, translucent sheet with a 10px backdrop blur and a hairline bottom. Nav links are 15.5px/700 soft-ink pills. Hover gives a cool ground wash. Active is teal-dark on Teal Mist. The theme toggle is a 44px round hairline button whose icon tilts −20° on the spring when hovered.
- **Tabs:** a 4px-padded track tinted from `line`, 14px radius, holding 40px tabs with a 10px radius. The active tab becomes a white sheet with the Sheet shadow.
- **Sidebar (internal panels):** 42px items with an 18px icon in soft ink. One white sheet indicator with a hairline and Sheet shadow slides to the active item (0.35s ease-out), and the active label turns teal-dark. Amber count badges sit on the right. On mobile it becomes a pill row with a Teal Mist active state.
- **Site header:** sticky glass (18px blur, saturate 180%), more opaque and shadowed after scrolling, with a 2px brand-teal reading-progress line on its bottom edge. Links follow the same pill states as the app. The burger opens a full-width panel with 17px rows.

### Goniómetro (signature component)
A 320×180 semicircular instrument (`components/ui/Goniometro.jsx` in the app; inline SVG on the site). It has a 10px round `line` base arc, a brand-teal measured arc drawn by `stroke-dashoffset`, graduation ticks every 5° (longer every 30°) that turn teal-dark once measured, optional step stops (white with a graduation-grey ring, filling teal once reached), an ink needle with a round tip, and an ink pivot with a teal center. The needle and arc travel on the spring over 0.9s, and on mount they sweep in from 0 over 1.2s after a 0.1s delay. The compact variant (14px arc, ticks every 15°) marks login, result screens, kinesiology plan progress and the agenda's day occupancy (a 46px inline gauge).

On result screens the arc color carries the outcome: teal for ok (full sweep), amber for warning (0.55) and red for error (0.12). While loading, the needle sweeps 40°↔140° on a loop. The goniometer is always `aria-hidden`, so the same value must exist in text beside it.

### Booking pieces
- **Step stops:** four named stops (Especialidad, Profesional, Día, Horario) under the arc as 56px buttons. The current one has a teal-dark name. Completed ones show a teal dot and their chosen value in 13px tabular soft ink, shown as a teal underline on mobile. Each step enters from the direction the needle moved (28px, 0.45s ease-out).
- **Option row:** a full-width 64px row-button. The title is 17.5px/700, the detail is 14.5px muted, and a trailing chevron nudges 3px on the spring. Selected state is a teal-dark border on Teal Mist.
- **Day cell:** a 92px cell with the weekday (14px/700 muted, teal-dark for today), a 30px tabular day number and the month. Selected state is a teal-dark border on Teal Mist.
- **Time scale and slots:** hairline-ruled hour rows. Slots are 48px with a 1.5px graduation-grey border and 17px/700 tabular text. Hover gives teal-dark on Teal Mist. Selected fills deep teal with white text, scales to 1.04 on the spring and takes the Lifted shadow.
- **Readout:** a large sheet holding the monumental time and the summary, with the confirm button spanning both columns. It rises in over 0.45s.
- **Confirmation:** the full arc, a green "Confirmado" pill, then the day and hour at readout scale.

### Agenda pieces (operator density)
- **Day summary:** a large sheet with the compact goniometer, the total as a 36px tabular figure, and the next appointments in a hairline-separated list.
- **Agenda row:** an 18px-radius hairline sheet with a 22px/800 tabular hour, the professional in muted, the patient, badges and small actions. Free rows take a faint green wash. Selection for batch cancel gives a red border and an inset ring, under a sticky red-tint batch bar.
- **Week/day grid:** a hairline grid on the sheet. Free cells are white with green text, booked cells are Teal Mist with ink, and confirmed or absent cells get an inset 2px green or red ring. Holidays get diagonal 6px hatching. Today's column header has a 3px teal underline. Hover shows an inset teal-dark ring. Keyboard shortcuts appear as `kbd` keycaps (24px, graduation-grey border with a 1px bottom shadow).

### Feedback
- **Alerts:** a 10px-radius tint panel with a 40%-mix border. Error, success and warning use the state pairs. Alerts drop in by 6px over 0.3s.
- **Toasts:** a white sheet with the Overlay shadow and a 10px status dot with a 4px halo, stacked from the bottom right. They enter from below and leave toward below.
- **Empty state:** a white panel with a 1.5px dashed graduation-grey border, the unmeasured half-ring, a 17px/700 title and a 46ch muted subtitle.
- **Skeleton:** a line/ground mix with a 1.5s highlight sweep.

### Site pieces
- **Caminos:** two large 26px sheets, at least 220px tall. The booking path is filled deep teal with white text (78% white for its paragraph). The other is a white sheet with a teal-dark action line.
- **Valores list:** hairline-ruled items, each led by a ruler mark: a 2×23px graduation-grey tick crossed by an 18×3px teal bar.
- **Graduated ruler:** under interior hero titles and photo banners there is a 14px ruler, built from a 32% teal 3px bar over 1.5px ticks every 12px and 2px ticks every 60px. On photo banners it runs in white and bright teal.
- **CTA band:** Night Ink with a faint grain, a large ghosted goniometer at the right, a white h2 at clamp(28px, 3.4vw, 40px), 72% white body text, and primary plus outline-light buttons.
- **Step gauges:** a 54px goniometer before each process step label. It measures the step's own degrees where the text carries them (0°, 90°, 180°), or its fraction of the journey otherwise, springing in over 1.1s with a 160ms stagger as the process scrolls into view.
- **Body map:** a stroked silhouette with pulsing teal joint dots. Choosing a joint draws a 24px-radius goniometer over it whose needle sweeps to 126° on the spring. It shows no number, because no real measurement backs it. The info panel fills with 48px treatment links.

## Do's and Don'ts

### Do:
- **Do** use `primary` (#186F6C) for any fill carrying white text, and keep the brand teal (#239A96) for arcs, needles, ticks and small marks.
- **Do** express progress, occupancy and outcome with the goniometer (arc plus needle plus graduations) and give the same value in text beside it, because the gauge is `aria-hidden`.
- **Do** set every time, date number, count and amount in tabular figures, and set the answer (the booked hour, the day total) large.
- **Do** keep patient controls at 48px or more (56px for the final confirm) and touch targets at 44px or more. Use 38px small buttons and chips only on secretaría/admin work screens.
- **Do** pair every state color with a shape: a filled dot or hollow ring on badges, an icon on attendance, inset rings in the grid.
- **Do** use the `--spring` curve only for the needle, arc, selections and small arrow nudges, and `--ease-out` (0.12–0.45s) for everything else. Let `prefers-reduced-motion` reduce all of it to 0.01ms.
- **Do** build surfaces as sheets: `white` token, 1px `line` border, Sheet shadow, 18px or 26px radius. Rule short lists with hairlines inside one sheet.
- **Do** keep focus visible: a 2px solid teal-dark outline (2px offset in the app), plus the 4px teal halo on fields.
- **Do** read every color through the tokens so dark mode (`data-theme` over `prefers-color-scheme`) follows automatically.

### Don't:
- **Don't** put white text on the brand teal #239A96 (3.3:1, below AA).
- **Don't** color "Reservado" red. It is neutral for staff and teal for patients. Red means error, absence or holiday.
- **Don't** place eyebrows, kickers or 01/02 numbering above headings. The title stands alone, and context goes in a subtitle below it.
- **Don't** move buttons or cards on hover (no translateY lift). Change tone, border and shadow instead, and keep press feedback to a slight scale.
- **Don't** re-add the effects removed in the redesign: a custom cursor, magnetic buttons, 3D tilt on cards, inertia or hijacked wheel scrolling, a loading intro with the isotype, ripple on buttons, a spotlight following the cursor on dark bands, or handwritten promo notes.
- **Don't** introduce Playfair Display, Inter, JetBrains Mono or any second text family. Caveat stays confined to the logo tagline.
- **Don't** print a degree or score on a goniometer unless a real measurement or real step value backs it.
- **Don't** use a small dropdown where a patient chooses specialty, professional, day or time. Use full-width rows, whole day cells and the graduated time scale.
- **Don't** add a second accent hue, or a gradient on buttons or text, to carry emphasis. The one teal needle is the emphasis.

# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

(The React app is also shipped wrapped as a desktop app with Electron and as iOS/Android apps with Capacitor, but its design language is the web one, so it stays `web`.)

## Users

- **Secretaría (primary operator):** spends the working day in the agenda from a PC (browser or the Electron desktop app): books, reschedules and cancels appointments, checks in patients, handles cash (caja), reminders and the waiting list. Speed, density and zero ambiguity matter most here.
- **Pacientes (primary public user):** book and manage their own appointments, almost always from a phone. Mixed ages, with many older adults (rehabilitation, traumatology). Many also book by phone through the secretaría.
- **Médicos:** check their own agenda, attend patients and write clinical history; less frequent use.
- **Admin and Contador:** manage staff, configuration and reports, or review the clinic's money; occasional use.
- **Website visitors:** people looking for treatment for a specific pain or condition in Lomas de Zamora, deciding whether to book.

## Product Purpose

FKT Lomas is a real kinesiology and medical-specialties center in Lomas de Zamora (Buenos Aires, Argentina). This repo holds its marketing website (`FKT Pagina/`, static HTML/CSS/JS) and its appointment system (`fkt-turnos-frontend/` React app on the `fkt-turnos-backend/` Spring Boot API). Success means patients can book on their own, without friction or doubt, and the secretaría runs the whole day from one tool.

## Positioning

The website is organized around "¿qué te pasa?" (what hurts, which body part) instead of a medical directory, with an interactive body map pointing to the real treatments for each area. The appointment system brings the agenda, reminders, cash, kinesiology plans and clinical history together in one tool, with five roles.

## Operating Context

- The secretaría works at a desk PC during opening hours, often while on the phone with a patient.
- Patients book from a phone, often older adults and sometimes with pain; they get reminders by email or WhatsApp.
- Spanish (Argentina, voseo) is the only interface language.
- Roles: PACIENTE, ADMIN, SECRETARIA, MEDICO, CONTADOR. Staff use mandatory TOTP two-step verification (except médicos, for whom it is optional); patients may sign in with Google.

## Capabilities and Constraints

- Frontend: React 19 + Redux Toolkit + React Router, Vite, plain CSS with custom properties (`src/index.css`), lucide-react icons, PWA. Existing dark mode and in-app notifications.
- Website: static HTML + shared `assets/css/site.css` / `assets/js/site.js`, no build step; already has dark mode, reduced-motion gating, a site search (Ctrl/Cmd+K), focus traps and SEO structured data.
- Everything must work with `prefers-reduced-motion` and on touch devices; motion on the website is gated to `pointer: fine`.
- The production deploy (domains, public backend) is not done yet.

## Brand Commitments

- Name: FKT Lomas. Existing logo and isotype (`FKT Pagina/assets/img/logo/`).
- Brand color: teal (`#239A96`, dark `#186F6C`) on deep blue-gray ink (`#1C3240`), shared by the site and the app. The user asked to keep the brand colors and logo and refine them, not replace them.
- Voice: Spanish (Argentina), close and clear, never alarmist.

## Evidence on Hand

- Real photos of the center, offices, specialties and services in `FKT Pagina/assets/img/`; logos of the obras sociales (health insurers) accepted.
- Real reviews: only the 3 in `opiniones.html`, plus the aggregate Google rating. Team: 26 professionals, loaded from the backend (photos only when one is actually uploaded).
- **Must not be fabricated:** credentials, years of experience, number of patients, cases, statistics, testimonials, WhatsApp response times, transit directions, or any other local fact not provided by the client. The site README lists the pending items that need real client data.

## Product Principles

1. **Calm and trust first:** booking a health appointment should feel easy and reassuring; nothing gets in the way of the task.
2. **Legible for everyone:** large text, high contrast and big targets for older patients; motion never hides information or confuses.
3. **Speed for the operator:** the secretaría's daily screens favor density, keyboard use and the state visible at a glance over decoration.
4. **Honesty:** only real content; when data is missing, say so explicitly instead of filling the gap.
5. **One system:** the site and the app read as the same center.

## Accessibility & Inclusion

WCAG 2.2 AA as the minimum floor, with special care for older adults: comfortable body text (16px minimum, larger in patient flows), touch targets of at least 44px, `prefers-reduced-motion` respected everywhere, visible focus, and states that never depend on color alone.

# CampusLab — Product Overview

CampusLab is a campus laboratory reservation management system. It serves two user roles:

- **ALUNO** (student): read-only access — can view laboratories and their reservations
- **PROFESSOR** (teacher): full access — can create, update, and delete laboratories and reservations

## Core Features

- **Authentication**: JWT-based login with email + password; stateless sessions
- **Laboratory management**: list, view detail (with current `reserved` status), create, update, delete (blocked if future reservations exist)
- **Reservation management**: list per laboratory (sorted chronologically), view, create, update, delete — with conflict detection (no overlapping time slots per lab) and minimum 1-minute duration enforcement
- **Error handling**: all error paths return a standardized JSON `ErrorResponse` — no stack traces or internal details are ever exposed

## Language

The application domain, error messages, and user-facing text are written in **Brazilian Portuguese** (pt-BR). Code, comments, and identifiers use English.

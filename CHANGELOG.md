## [2026-09-24] - Frontend UI Polish & Dockerization

### Added
- **Multi-Account Support**: Added an account dropdown selector to the Dashboard and Transfer pages, allowing dynamic switching of active ledgers.
- **Create Account Flow**: Built `CreateAccountPage` (`/accounts/new`) using the premium Split Studio layout.
- **UUID Copy Feature**: Added a clickable, styling input-box to the Transfer page that copies the user's UUID to the clipboard with a 2-second success state.
- **Frontend Dockerization**: Created a multi-stage `Dockerfile` (`node:20-alpine` build stage -> `nginx:alpine` serve stage) for the React frontend.
- **Nginx Config**: Added `nginx.conf` customized for SPA routing (`try_files` fallback), Gzip compression, and 1-year caching on Vite assets.

### Changed
- **UI Architecture**: Extracted a reusable `SideNav` component for all protected routes to enforce DRY principles.
- **Theme & Tone**: Transitioned the app to the "Energetic Atelier" theme. Removed all specific Oracle Cloud / 24-core jokes across all pages (`DashboardPage`, `SettingsPage`, `RegisterPage`, `LoginPage`, `TransferPage`) in favor of a cleaner, universally prestigious "fictitious wealth" tone.
- **Dashboard Layout**: Relocated the account management controls (Dropdown and New Account button) from the Bento Grid into the main page header for a cleaner aesthetic.

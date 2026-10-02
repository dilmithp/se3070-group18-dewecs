# DEWECS Admin

React (Vite) officer console for the DEWECS Spring Boot backend (`../se3070-group18-dewecs/backend`).
It uses the backend's existing officer routes in JSON mode (`Accept: application/json`, flat JSON bodies), so the backend is unchanged.

Screens: dashboard, warnings (draft/edit/publish/retract), shelters (check-in/out, close/reopen), rescue requests (assign/complete/cancel),
relief supplies (restock), relief distributions (deliver/cancel), ground-report review, post-event reports.

## Run

1. Start the backend (demo data, no database needed):
   `cd ../se3070-group18-dewecs/backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`
2. `npm install && npm run dev` → http://localhost:5173

Dev requests go to `/backend/*`, which Vite proxies to `http://localhost:8080` (override with `BACKEND_URL`).
The officer routes send no CORS headers, so for a production build either serve the app and backend behind one reverse proxy
(map `/backend/` to the backend) or set `VITE_API_BASE` and add CORS on the backend.

The backend has no login yet: actions that need a user (issuing a warning, reviewing a report) use an officer dropdown, remembered in the browser.

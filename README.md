# Birthday RSVP

A small full-stack invitation site built with React, Spring Boot, and PostgreSQL.

## Personalize the invitation

Edit `frontend/src/partyConfig.js`. This is where the child's name, age, party time,
venue, address, RSVP deadline, and short biography live.

## Run locally

1. Start PostgreSQL:

   ```bash
   docker compose up -d db
   ```

2. Start the API:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. Start the website in another terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

Open `http://localhost:5173`. The API runs at `http://localhost:8080`.

## Environment variables

Backend:

- `DATABASE_URL` (default `jdbc:postgresql://localhost:5432/birthday`)
- `DATABASE_USERNAME` (default `birthday`)
- `DATABASE_PASSWORD` (default `birthday`)
- `ALLOWED_ORIGINS` (comma-separated; default local frontend)
- `PORT` (default `8080`)
- `WHATSAPP_ENABLED` (`true` only after setup; otherwise messages are disabled)
- `WHATSAPP_ACCESS_TOKEN` (Meta permanent system-user access token; keep secret)
- `WHATSAPP_PHONE_NUMBER_ID` (the Phone Number ID from Meta WhatsApp Manager)
- `WHATSAPP_TEMPLATE_NAME` (default `birthday_invitation`)
- `WHATSAPP_TEMPLATE_LANGUAGE` (default `en_US`)
- `WHATSAPP_TEMPLATE_INCLUDES_NAME` (default `true`; use a `{{1}}` guest-name variable in the template body)
- `WHATSAPP_ADMIN_TOKEN` (a long, private token required to call the send endpoint)

Frontend build:

- `VITE_API_URL` (default `http://localhost:8080`)

## WhatsApp Business invitations

The website includes a free WhatsApp share link for manually sending the invitation.
The API is also prepared for the official WhatsApp Business Cloud API, but is disabled
by default so it cannot send messages accidentally.

Before enabling it in Render, create and obtain approval for a Meta WhatsApp template
named `birthday_invitation`. Put the full invitation and RSVP URL in that template.
Then set the WhatsApp environment variables above. The private `/naveen` page lets you
maintain the invitation list and send pending invitations. Each send is recorded as
`ACCEPTED` (accepted by Meta) or `FAILED`; accepted does not guarantee that the guest has
read the message. The guest name is sent as the `{{1}}` value in the approved template.
Only send to guests who have expressly agreed to receive WhatsApp messages.

## Production on Northflank

Northflank's free Sandbox tier fits this project with two services and one database:

1. Create a Northflank project and a free PostgreSQL addon.
2. Create the **backend** service from this repository. Select Dockerfile build,
   use `backend` as the build context, and expose public port `8080`.
3. Map the PostgreSQL addon's connection values to `DATABASE_URL`,
   `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The URL must use the JDBC form
   `jdbc:postgresql://HOST:PORT/DATABASE`.
4. Create the **frontend** service from the same repository. Select Dockerfile build,
   use `frontend` as the build context, expose public port `80`, and provide the
   backend's public URL as the build argument `VITE_API_URL`.
5. After Northflank gives the frontend its public URL, set that exact URL as the
   backend runtime variable `ALLOWED_ORIGINS`, then restart the backend.
6. Add your purchased custom domain to the frontend service.

The invitation is at the domain root. The unlisted response dashboard is at
`/naveen`; it is hidden by its URL but is not password-protected.

import { neon } from '@neondatabase/serverless'

export const config = { api: { bodyParser: false } }

const reply = (response, status, body) => response.status(status).json(body)

function getDatabase() {
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL is not configured')
  return neon(process.env.DATABASE_URL)
}

async function readBody(request) {
  const chunks = []
  for await (const chunk of request) chunks.push(Buffer.from(chunk))
  return Buffer.concat(chunks)
}

async function readJson(request) {
  const body = await readBody(request)
  return body.length ? JSON.parse(body.toString('utf8')) : {}
}

function requestedPath(request) {
  const value = Array.isArray(request.query.path) ? request.query.path.join('/') : request.query.path
  return `/${value || ''}`.replace(/\/+$/, '') || '/'
}

function nameWords(value = '') {
  return new Set(value.toLowerCase().replace(/[^a-z0-9 ]/g, ' ').split(/\s+/).filter((word) => word.length >= 2))
}

function matchingName(submittedName, existingName) {
  const submitted = nameWords(submittedName)
  return [...nameWords(existingName)].some((word) => submitted.has(word))
}

function requireAdmin(request, response) {
  const configured = process.env.WHATSAPP_ADMIN_TOKEN || ''
  const supplied = request.headers['x-admin-token'] || ''
  if (!configured || supplied !== configured) {
    reply(response, 401, { message: 'Not authorized to manage invitations' })
    return false
  }
  return true
}

function mapRsvp(row) {
  return {
    id: Number(row.id),
    guestName: row.guest_name,
    attending: row.attending,
    adults: row.adults,
    toddlers: row.toddlers,
    partySize: row.party_size,
    vegetarianCount: row.vegetarian_count,
    nonVegetarianCount: row.non_vegetarian_count,
    message: row.message || '',
    createdAt: row.created_at,
  }
}

function mapWhatsAppGuest(row) {
  return {
    id: Number(row.id),
    guestName: row.guest_name,
    phoneNumber: row.phone_number,
    invitationStatus: row.invitation_status,
    lastError: row.last_error,
    sentAt: row.sent_at,
    createdAt: row.created_at,
  }
}

function photoBytes(value) {
  if (Buffer.isBuffer(value)) return value
  if (value instanceof Uint8Array) return Buffer.from(value)
  if (typeof value === 'string' && value.startsWith('\\x')) return Buffer.from(value.slice(2), 'hex')
  return Buffer.from(value || '')
}

async function sendWhatsApp(phoneNumber, guestName) {
  if (process.env.WHATSAPP_ENABLED !== 'true' || !process.env.WHATSAPP_ACCESS_TOKEN || !process.env.WHATSAPP_PHONE_NUMBER_ID) {
    throw new Error('WhatsApp invitations are not configured yet')
  }
  const template = {
    name: process.env.WHATSAPP_TEMPLATE_NAME || 'birthday_invitation',
    language: { code: process.env.WHATSAPP_TEMPLATE_LANGUAGE || 'en_US' },
  }
  if (process.env.WHATSAPP_TEMPLATE_INCLUDES_NAME !== 'false') {
    template.components = [{ type: 'body', parameters: [{ type: 'text', text: guestName }] }]
  }
  const result = await fetch(`https://graph.facebook.com/${process.env.WHATSAPP_API_VERSION || 'v23.0'}/${process.env.WHATSAPP_PHONE_NUMBER_ID}/messages`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${process.env.WHATSAPP_ACCESS_TOKEN}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ messaging_product: 'whatsapp', to: phoneNumber, type: 'template', template }),
  })
  if (!result.ok) throw new Error('WhatsApp could not send this invitation')
}

export default async function handler(request, response) {
  const path = requestedPath(request)
  try {
    if (path === '/health' && request.method === 'GET') return reply(response, 200, { status: 'ok' })
    const sql = getDatabase()

    if (path === '/rsvps' && request.method === 'GET') {
      const rows = await sql`SELECT * FROM rsvps ORDER BY created_at DESC`
      return reply(response, 200, rows.map(mapRsvp))
    }
    if (path === '/rsvps/names' && request.method === 'GET') {
      const rows = await sql`SELECT DISTINCT guest_name FROM rsvps ORDER BY guest_name`
      return reply(response, 200, rows.map((row) => row.guest_name))
    }
    if (path === '/rsvps' && request.method === 'POST') {
      const body = await readJson(request)
      if (!body.guestName?.trim()) return reply(response, 400, { message: 'Guest name is required' })
      const existing = await sql`SELECT DISTINCT guest_name FROM rsvps`
      const matches = existing.map((row) => row.guest_name).filter((name) => matchingName(body.guestName, name))
      if (!body.confirmDuplicate && matches.length) return reply(response, 409, { code: 'POSSIBLE_DUPLICATE', matches })
      const attending = Boolean(body.attending)
      const adults = attending ? Number(body.adults || 0) : 0
      const toddlers = attending ? Number(body.toddlers || 0) : 0
      const vegetarian = attending ? Number(body.vegetarianCount || 0) : 0
      const nonVegetarian = attending ? Number(body.nonVegetarianCount || 0) : 0
      if (adults < 0 || toddlers < 0 || adults + toddlers > 20 || vegetarian + nonVegetarian !== adults + toddlers) {
        return reply(response, 400, { message: 'Guest or meal counts are invalid' })
      }
      const rows = await sql`INSERT INTO rsvps
        (guest_name, email, attending, party_size, adults, toddlers, vegetarian_count, non_vegetarian_count, message, created_at)
        VALUES (${body.guestName.trim()}, '', ${attending}, ${adults + toddlers}, ${adults}, ${toddlers}, ${vegetarian}, ${nonVegetarian}, ${(body.message || '').trim()}, NOW())
        RETURNING id, attending`
      return reply(response, 201, { id: Number(rows[0].id), attending: rows[0].attending })
    }
    const rsvpDelete = path.match(/^\/rsvps\/(\d+)$/)
    if (rsvpDelete && request.method === 'DELETE') {
      const rows = await sql`DELETE FROM rsvps WHERE id = ${Number(rsvpDelete[1])} RETURNING id`
      return rows.length ? response.status(204).end() : reply(response, 404, { message: 'RSVP not found' })
    }

    if (path === '/photos' && request.method === 'GET') {
      const rows = await sql`SELECT id, file_name, content_type, file_size, created_at FROM gallery_photos ORDER BY created_at DESC`
      return reply(response, 200, rows.map((row) => ({
        id: Number(row.id), fileName: row.file_name, contentType: row.content_type,
        fileSize: Number(row.file_size), createdAt: row.created_at,
      })))
    }
    if (path === '/photos' && request.method === 'POST') {
      const contentType = String(request.headers['content-type'] || '').split(';')[0].toLowerCase()
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(contentType)) return reply(response, 400, { message: 'JPEG, PNG, or WebP required' })
      const data = await readBody(request)
      if (!data.length || data.length > 4 * 1024 * 1024) return reply(response, 400, { message: 'Photo must be no larger than 4 MB' })
      const fileName = decodeURIComponent(String(request.headers['x-file-name'] || 'photo')).replace(/[\r\n"/\\]/g, '_')
      const rows = await sql`INSERT INTO gallery_photos (file_name, content_type, file_size, photo_data, created_at)
        VALUES (${fileName}, ${contentType}, ${data.length}, decode(${data.toString('hex')}, 'hex'), NOW()) RETURNING id, created_at`
      return reply(response, 201, { id: Number(rows[0].id), fileName, contentType, fileSize: data.length, createdAt: rows[0].created_at })
    }
    const photoContent = path.match(/^\/photos\/(\d+)\/content$/)
    if (photoContent && request.method === 'GET') {
      const rows = await sql`SELECT file_name, content_type, photo_data FROM gallery_photos WHERE id = ${Number(photoContent[1])}`
      if (!rows.length) return reply(response, 404, { message: 'Photo not found' })
      response.setHeader('Content-Type', rows[0].content_type)
      response.setHeader('Cache-Control', 'public, max-age=3600')
      return response.status(200).send(photoBytes(rows[0].photo_data))
    }

    if (path === '/whatsapp/guests' && request.method === 'GET') {
      if (!requireAdmin(request, response)) return
      const rows = await sql`SELECT * FROM whatsapp_guests ORDER BY created_at ASC`
      return reply(response, 200, rows.map(mapWhatsAppGuest))
    }
    if (path === '/whatsapp/guests' && request.method === 'POST') {
      if (!requireAdmin(request, response)) return
      const body = await readJson(request)
      const phone = String(body.phoneNumber || '').replace(/[^0-9]/g, '')
      if (!body.guestName?.trim() || phone.length < 8 || phone.length > 15) return reply(response, 400, { message: 'Guest name or mobile number is invalid' })
      try {
        const rows = await sql`INSERT INTO whatsapp_guests (guest_name, phone_number, invitation_status, created_at)
          VALUES (${body.guestName.trim()}, ${phone}, 'PENDING', NOW()) RETURNING *`
        return reply(response, 201, mapWhatsAppGuest(rows[0]))
      } catch (error) {
        if (error.code === '23505') return reply(response, 409, { message: 'This mobile number is already in the invitation list' })
        throw error
      }
    }
    if (path === '/whatsapp/invitations/send-pending' && request.method === 'POST') {
      if (!requireAdmin(request, response)) return
      const guests = await sql`SELECT * FROM whatsapp_guests WHERE invitation_status = 'PENDING' ORDER BY created_at ASC`
      let accepted = 0
      let failed = 0
      for (const guest of guests) {
        try {
          await sendWhatsApp(guest.phone_number, guest.guest_name)
          await sql`UPDATE whatsapp_guests SET invitation_status = 'ACCEPTED', last_error = NULL, sent_at = NOW() WHERE id = ${guest.id}`
          accepted += 1
        } catch (error) {
          await sql`UPDATE whatsapp_guests SET invitation_status = 'FAILED', last_error = ${error.message.slice(0, 500)} WHERE id = ${guest.id}`
          failed += 1
        }
      }
      return reply(response, 200, { accepted, failed })
    }

    return reply(response, 404, { message: 'API route not found' })
  } catch (error) {
    console.error(error)
    return reply(response, 500, { message: 'The API could not complete this request' })
  }
}

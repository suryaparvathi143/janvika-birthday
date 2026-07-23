import { useEffect, useState } from 'react'
import { party } from './partyConfig'

const API_URL =
  import.meta.env.VITE_API_URL ||
  (import.meta.env.DEV
    ? 'http://localhost:8080'
    : 'https://p01--birthday-api--qz475gfh2z9z.code.run')

function Detail({ icon, label, children }) {
  return <div className="detail"><span className="detail-icon" aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{children}</strong></div></div>
}

function RsvpDetails() {
  const [responses, setResponses] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [deletingId, setDeletingId] = useState(null)

  useEffect(() => {
    fetch(`${API_URL}/api/rsvps`)
      .then((response) => {
        if (!response.ok) throw new Error('Could not load responses')
        return response.json()
      })
      .then(setResponses)
      .catch((error) => setLoadError(error.message))
      .finally(() => setLoading(false))
  }, [])

  const deleteResponse = async (response) => {
    if (!window.confirm(`Delete ${response.guestName}'s RSVP? This cannot be undone.`)) return

    setDeletingId(response.id)
    setLoadError('')
    try {
      const result = await fetch(`${API_URL}/api/rsvps/${response.id}`, { method: 'DELETE' })
      if (!result.ok) throw new Error('Could not delete this response')
      setResponses((current) => current.filter((item) => item.id !== response.id))
    } catch (error) {
      setLoadError(error.message)
    } finally {
      setDeletingId(null)
    }
  }

  const attending = responses.filter((response) => response.attending)
  const declined = responses.filter((response) => !response.attending)
  const totalGuests = attending.reduce((total, response) => total + response.partySize, 0)
  const adults = attending.reduce((total, response) => total + response.adults, 0)
  const toddlers = attending.reduce((total, response) => total + response.toddlers, 0)

  return <main className="details-page">
    <header className="details-header">
      <p className="eyebrow">Janvika’s celebration</p>
      <h1>Guest responses</h1>
      <p>A private overview of everyone who has replied.</p>
    </header>
    {loading && <p className="details-state">Loading responses…</p>}
    {loadError && <p className="details-state error" role="alert">{loadError}</p>}
    {!loading && !loadError && <>
      <section className="summary-grid" aria-label="RSVP summary">
        <div><small>RESPONSES</small><strong>{responses.length}</strong></div>
        <div><small>COMING</small><strong>{attending.length}</strong></div>
        <div><small>TOTAL GUESTS</small><strong>{totalGuests}</strong></div>
        <div><small>ADULTS / TODDLERS</small><strong>{adults} / {toddlers}</strong></div>
      </section>
      <section className="response-section">
        <div className="response-title"><h2>Joyfully attending</h2><span>{attending.length}</span></div>
        <div className="response-list">
          {attending.length === 0 && <p className="empty-response">No accepting responses yet.</p>}
          {attending.map((response) => <article className="response-card" key={response.id}>
            <div className="response-top"><h3>{response.guestName}</h3><div className="response-actions"><time>{new Date(response.createdAt).toLocaleDateString()}</time><button className="delete-response" type="button" onClick={() => deleteResponse(response)} disabled={deletingId === response.id}>{deletingId === response.id ? 'Deleting…' : 'Delete'}</button></div></div>
            <p className="headcount"><span>{response.adults} {response.adults === 1 ? 'adult' : 'adults'}</span><span>{response.toddlers} {response.toddlers === 1 ? 'toddler' : 'toddlers'}</span><span>{response.vegetarianCount} veg</span><span>{response.nonVegetarianCount} non-veg</span><strong>{response.partySize} total</strong></p>
            {response.message && <p className="guest-message">“{response.message}”</p>}
          </article>)}
        </div>
      </section>
      <section className="response-section declined-section">
        <div className="response-title"><h2>Sadly declining</h2><span>{declined.length}</span></div>
        <div className="response-list">
          {declined.length === 0 && <p className="empty-response">No declined responses.</p>}
          {declined.map((response) => <article className="response-card" key={response.id}>
            <div className="response-top"><h3>{response.guestName}</h3><div className="response-actions"><time>{new Date(response.createdAt).toLocaleDateString()}</time><button className="delete-response" type="button" onClick={() => deleteResponse(response)} disabled={deletingId === response.id}>{deletingId === response.id ? 'Deleting…' : 'Delete'}</button></div></div>
            {response.message && <p className="guest-message">“{response.message}”</p>}
          </article>)}
        </div>
      </section>
    </>}
  </main>
}

function InvitationApp() {
  const [showInvitation, setShowInvitation] = useState(true)
  const [form, setForm] = useState({ guestName: '', attending: true, adults: 1, toddlers: 0, vegetarianCount: 1, nonVegetarianCount: 0, message: '' })
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

  useEffect(() => {
    document.body.style.overflow = showInvitation ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [showInvitation])

  const update = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: ['adults', 'toddlers', 'vegetarianCount', 'nonVegetarianCount'].includes(name) ? Number(value) : value }))
  }

  const submit = async (event) => {
    event.preventDefault()
    if (form.attending && form.vegetarianCount + form.nonVegetarianCount > form.adults + form.toddlers) {
      setError('Meal selections cannot be more than the total number of guests.')
      return
    }
    setStatus('sending')
    setError('')
    try {
      const response = await fetch(`${API_URL}/api/rsvps`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...form,
          adults: form.attending ? form.adults : 0,
          toddlers: form.attending ? form.toddlers : 0,
          vegetarianCount: form.attending ? form.vegetarianCount : 0,
          nonVegetarianCount: form.attending ? form.nonVegetarianCount : 0,
          partySize: form.attending ? form.adults + form.toddlers : 0,
        }),
      })
      if (!response.ok) throw new Error('We could not save your reply.')
      setStatus('success')
    } catch (err) {
      setError(`${err.message} Please try again in a moment.`)
      setStatus('error')
    }
  }

  return <main>
    {showInvitation && <div className="invitation-overlay" role="presentation">
      <div className="invitation-modal" role="dialog" aria-modal="true" aria-labelledby="invitation-title">
        <div className="invitation-border" aria-hidden="true" />
        <span className="corner-flourish corner-flourish-left" aria-hidden="true">❀</span>
        <span className="corner-flourish corner-flourish-right" aria-hidden="true">❀</span>
        <div className="invitation-stars" aria-hidden="true">✦　·　✧</div>
        <p className="invitation-kicker">You’re warmly invited</p>
        <h2 id="invitation-title">Come celebrate<br /><em>{party.childName}</em></h2>
        <p className="invitation-copy">Our little angel is turning {party.age}, and her special day would be brighter with you there.</p>
        <div className="invitation-date">
          <div className="date-medallion" aria-hidden="true"><span>JUL</span><strong>26</strong></div>
          <div>
            <small>CELEBRATING ON</small>
            <strong>{party.celebrationDate}</strong>
            <span>{party.time}</span>
          </div>
        </div>
        <button className="open-invitation" autoFocus onClick={() => setShowInvitation(false)}>Open the invitation <span>→</span></button>
        <p className="invitation-signoff">{party.hostNames}</p>
      </div>
    </div>}
    <section className="hero">
      <div className="confetti confetti-one" /><div className="confetti confetti-two" />
      <div className="hero-copy">
        <p className="eyebrow">A little celebration for a very big joy</p>
        <div className="hero-date-pill"><span>July 26</span><i />12:00 PM</div>
        <h1>{party.headline}<br /><span className="child-name">{party.childName}</span><br /><em>is turning {party.age}!</em></h1>
        <p className="intro">Cake, giggles, wishes—and all the people we love. Please join us to celebrate her special day.</p>
        <a className="primary-link" href="#rsvp">Say you’ll be there <span>↓</span></a>
      </div>
      <div className="cake" aria-label={`A birthday cake with ${party.age} candles`}>
        <div className="candles">{Array.from({ length: party.age }, (_, i) => <i key={i} />)}</div>
        <div className="cake-top" /><div className="cake-base"><span>{party.age}</span></div>
        <div className="plate" />
      </div>
    </section>

    <section className="details-section">
      <div className="details-card">
        <p className="script">Save the date</p>
        <div className="details-grid">
          <Detail icon="◷" label="WHEN">{party.celebrationDate}<br />{party.time}</Detail>
          <Detail icon="⌂" label="WHERE">{party.venue}<br />{party.address}</Detail>
        </div>
        {party.mapUrl && <a className="text-link" href={party.mapUrl} target="_blank" rel="noreferrer">Open in maps ↗</a>}
        <p className="birthday-note">{party.birthdayNote}</p>
      </div>
    </section>

    <section className="about-section">
      <div><p className="eyebrow">Meet the birthday girl</p><h2>A pocketful of sunshine</h2></div>
        <div><p>{party.about}</p></div>
    </section>

    <section className="rsvp-section" id="rsvp">
      <div className="rsvp-heading"><p className="eyebrow">Kindly reply</p><h2>Will you celebrate with us?</h2></div>
      <div className="form-card">
        {status === 'success' ? <div className="success" role="status"><span>✓</span><h3>{form.attending ? 'We can’t wait to see you!' : 'Thank you for letting us know.'}</h3><p>Your reply has been saved. {form.attending ? `We’ve reserved space for ${form.adults + form.toddlers} (${form.adults} ${form.adults === 1 ? 'adult' : 'adults'} and ${form.toddlers} ${form.toddlers === 1 ? 'toddler' : 'toddlers'}).` : `We’ll miss you and appreciate the birthday wishes.`}</p><button onClick={() => { setStatus('idle'); setForm({ guestName: '', attending: true, adults: 1, toddlers: 0, vegetarianCount: 1, nonVegetarianCount: 0, message: '' }) }}>Send another reply</button></div> :
        <form onSubmit={submit}>
          <label>Your name<input required maxLength="100" name="guestName" value={form.guestName} onChange={update} placeholder="Family or guest name" /></label>
          <fieldset><legend>Can you make it?</legend><div className="choice-row"><label className={form.attending ? 'selected' : ''}><input type="radio" checked={form.attending} onChange={() => setForm({ ...form, attending: true, adults: Math.max(1, form.adults) })} />Joyfully accepting</label><label className={!form.attending ? 'selected' : ''}><input type="radio" checked={!form.attending} onChange={() => setForm({ ...form, attending: false })} />Sadly declining</label></div></fieldset>
          {form.attending && <><fieldset><legend>Who is coming from your family?</legend><div className="guest-counts"><label>Adults<select name="adults" value={form.adults} onChange={update}>{Array.from({ length: 13 }, (_, i) => <option key={i} value={i}>{i}</option>)}</select></label><label>Toddlers<select name="toddlers" value={form.toddlers} onChange={update}>{Array.from({ length: 13 }, (_, i) => <option key={i} value={i}>{i}</option>)}</select></label></div><p className="guest-total">Total guests: <strong>{form.adults + form.toddlers}</strong></p></fieldset><fieldset><legend>Meal preference</legend><div className="guest-counts"><label>Vegetarian<select name="vegetarianCount" value={form.vegetarianCount} onChange={update}>{Array.from({ length: form.adults + form.toddlers + 1 }, (_, i) => <option key={i} value={i}>{i}</option>)}</select></label><label>Non-vegetarian<select name="nonVegetarianCount" value={form.nonVegetarianCount} onChange={update}>{Array.from({ length: form.adults + form.toddlers + 1 }, (_, i) => <option key={i} value={i}>{i}</option>)}</select></label></div><p className="guest-total">Meals selected: <strong>{form.vegetarianCount + form.nonVegetarianCount}</strong> of {form.adults + form.toddlers}</p></fieldset></>}
          <label>Birthday note <span className="optional">optional</span><textarea maxLength="500" name="message" value={form.message} onChange={update} placeholder="Share a sweet wish or anything we should know…" /></label>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="submit" disabled={status === 'sending'}>{status === 'sending' ? 'Sending…' : 'Send my reply'} <span>→</span></button>
        </form>}
      </div>
    </section>
    <footer><span>✦</span><p>{party.hostNames}</p><small>Made especially for {party.childName}</small></footer>
  </main>
}

export default function App() {
  return window.location.pathname.replace(/\/$/, '') === '/naveen'
    ? <RsvpDetails />
    : <InvitationApp />
}

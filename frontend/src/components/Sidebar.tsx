export type View = 'learn' | 'practise' | 'progress'

const ITEMS: { id: View; label: string; glyph: string; hint: string }[] = [
  { id: 'learn', label: 'Learn', glyph: '◳', hint: 'Read a topic before you are asked about it' },
  { id: 'practise', label: 'Practise', glyph: '◑', hint: 'Work with the tutor' },
  { id: 'progress', label: 'Progress', glyph: '◔', hint: 'What the tutor believes you know' },
]

export function Sidebar({ view, onChange }: { view: View; onChange: (view: View) => void }) {
  return (
    <nav className="sidebar" aria-label="Sections">
      <div className="brand">
        <span className="brand-mark">♮</span>
        <span className="brand-name">Leading Tone</span>
      </div>
      <ul>
        {ITEMS.map((item) => (
          <li key={item.id}>
            <button
              type="button"
              className={`nav-item${view === item.id ? ' nav-item-on' : ''}`}
              aria-current={view === item.id ? 'page' : undefined}
              title={item.hint}
              onClick={() => onChange(item.id)}
            >
              <span className="nav-glyph" aria-hidden="true">
                {item.glyph}
              </span>
              {item.label}
            </button>
          </li>
        ))}
      </ul>
      <p className="sidebar-foot">Saved to a file on this machine.</p>
    </nav>
  )
}

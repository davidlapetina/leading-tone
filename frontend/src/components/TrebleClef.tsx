/**
 * The treble clef, drawn as two strokes: the spine with its tail, and the body that wraps it.
 *
 * <p>Stroked rather than filled so it inherits `currentColor` and keeps its weight at any
 * size — the same drawing serves the 2rem brand mark and a 16px browser tab. Kept in step
 * with `public/favicon.svg`, which is this path on the same blue square.
 */
export function TrebleClef({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 48 72"
      width="15"
      height="22"
      fill="none"
      stroke="currentColor"
      strokeWidth="4.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M33 14C33 6 25 5 23 12C21 20 24 28 27 36C30 44 31 52 31 58C31 65 25 68 21 65C18 63 19 59 23 60" />
      <path d="M25 13C34 21 41 29 40 38C39 48 30 54 22 50C15 46 14 36 21 33C28 30 34 37 32 44C31 50 26 52 23 49" />
    </svg>
  )
}

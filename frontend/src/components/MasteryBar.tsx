interface MasteryBarProps {
  value: number
  state: string
}

/**
 * Deliberately unnumbered. The learner model is the engine's business; showing a
 * percentage invites the learner to optimise the number instead of the music.
 */
export function MasteryBar({ value, state }: MasteryBarProps) {
  const filled = Math.round(Math.max(0, Math.min(1, value)) * 12)
  return (
    <span
      className={`bar bar-${state.toLowerCase()}`}
      role="img"
      aria-label={`${state.toLowerCase().replace('_', ' ')}`}
    >
      {'█'.repeat(filled)}
      {'·'.repeat(12 - filled)}
    </span>
  )
}

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'

type Mode = 'play' | 'write' | 'auto'

const OPTIONS: { value: Mode; label: string; hint: string }[] = [
  { value: 'auto', label: 'Mixed', hint: 'The tutor varies how it asks' },
  { value: 'play', label: 'Play', hint: 'Answer at the keyboard where possible' },
  { value: 'write', label: 'Write', hint: 'Answer in writing' },
]

/**
 * How the learner wants to practise. The tutor still decides *what* to work on — that
 * comes from the learner model — but the form is a preference, and it is theirs.
 */
export function PracticeMenu({ current }: { current: 'TEXT' | 'MIDI' | null }) {
  const queryClient = useQueryClient()
  const active: Mode = current === 'MIDI' ? 'play' : current === 'TEXT' ? 'write' : 'auto'

  const choose = useMutation({
    mutationFn: api.setPracticeMode,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['learner'] }),
  })

  return (
    <div className="practice-menu" role="group" aria-label="How to practise">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          title={option.hint}
          aria-pressed={active === option.value}
          className={active === option.value ? 'chosen' : undefined}
          disabled={choose.isPending}
          onClick={() => choose.mutate(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Conversation } from '../Conversation'
import type { ConversationEntry } from '../../state/tutorStore'

vi.mock('../Score', () => ({ Score: ({ abc }: { abc: string }) => <pre data-testid="score">{abc}</pre> }))

const entry = (overrides: Partial<ConversationEntry>): ConversationEntry => ({
  id: 'e1',
  role: 'tutor',
  text: 'Play the dominant triad of A minor.',
  ...overrides,
})

describe('Conversation', () => {
  it('shows the tutor and the learner distinctly', () => {
    render(
      <Conversation
        entries={[entry({}), entry({ id: 'e2', role: 'learner', text: 'played E3 G#3 B3' })]}
        busy={false}
      />,
    )
    expect(screen.getByText('Play the dominant triad of A minor.')).toBeInTheDocument()
    expect(screen.getByText('played E3 G#3 B3')).toBeInTheDocument()
  })

  it('puts the evaluator’s verdict above the tutor’s words', () => {
    render(
      <Conversation
        entries={[
          entry({
            attempt: {
              outcome: {
                result: 'PARTIALLY_CORRECT',
                feedback: 'The right notes, but the bass should be B.',
                detail: null,
                misconceptionCode: 'plays-root-position-when-inversion-asked',
                misconceptionDescription: null,
                confidence: 1,
                requiresModelJudgement: false,
              },
              conceptId: 'chord-inversion',
              masteryBefore: 0.4,
              masteryAfter: 0.45,
              state: 'PRACTICING',
              evidenceRecorded: true,
            },
          }),
        ]}
        busy={false}
      />,
    )
    expect(screen.getByText(/nearly/)).toBeInTheDocument()
    expect(screen.getByText(/bass should be B/)).toBeInTheDocument()
  })

  it('renders notation when the turn carries a score', () => {
    render(<Conversation entries={[entry({ notationAbc: 'X:1\nK:C\n[CEG]' })]} busy={false} />)
    expect(screen.getByTestId('score')).toHaveTextContent('K:C')
  })

  it('shows that the tutor is working', () => {
    render(<Conversation entries={[]} busy />)
    expect(screen.getByText('…')).toBeInTheDocument()
  })
})

describe('the generated question', () => {
  it('is shown on its own when the tutor paraphrased it away', () => {
    render(
      <Conversation
        entries={[
          entry({
            text: 'What do you think of the note F#?',
            exercisePrompt: 'Write another name for F#.',
            exerciseId: 'exercise-1',
            taskKind: 'recognise it',
          }),
        ]}
        busy={false}
      />,
    )
    expect(screen.getByText('Write another name for F#.')).toBeInTheDocument()
  })

  it('says what kind of thinking the question wants, alongside why it was asked', () => {
    render(
      <Conversation
        entries={[
          entry({
            text: 'Have a look at this progression.',
            exercisePrompt: 'Analyse this in C major: C F G7 C',
            exerciseId: 'exercise-2',
            action: 'CHALLENGE',
            taskKind: 'explain it in context',
          }),
        ]}
        busy={false}
      />,
    )
    expect(screen.getByText('explain it in context')).toBeInTheDocument()
    expect(screen.getByText('challenge')).toBeInTheDocument()
    expect(screen.getByText('Analyse this in C major: C F G7 C')).toBeInTheDocument()
  })

  it('is not repeated when the tutor already asked it', () => {
    render(
      <Conversation
        entries={[
          entry({
            text: 'Write another name for F#.',
            exercisePrompt: 'Write another name for F#.',
            exerciseId: 'exercise-1',
          }),
        ]}
        busy={false}
      />,
    )
    expect(screen.getAllByText('Write another name for F#.')).toHaveLength(1)
  })
})

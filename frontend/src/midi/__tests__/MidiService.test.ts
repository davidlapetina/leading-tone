import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MidiService, PHRASE_GAP_MS } from '../MidiService'

const NOTE_ON = 0x90
const NOTE_OFF = 0x80

describe('MidiService', () => {
  let performances: number[][]
  let active: number[][]
  let service: MidiService

  beforeEach(() => {
    vi.useFakeTimers()
    performances = []
    active = []
    service = new MidiService({
      onPerformance: (notes) => performances.push(notes),
      onActiveNotes: (notes) => active.push(notes),
    })
  })

  const play = (notes: number[]) => {
    notes.forEach((note) => service.acceptForTesting([NOTE_ON, note, 90]))
    notes.forEach((note) => service.acceptForTesting([NOTE_OFF, note, 0]))
  }

  it('emits a chord once every key is released and the pause has passed', () => {
    play([55, 59, 62])
    expect(performances).toHaveLength(0)

    vi.advanceTimersByTime(PHRASE_GAP_MS)
    expect(performances).toEqual([[55, 59, 62]])
  })

  it('keeps the order the notes were struck in, so the bass is recoverable', () => {
    play([59, 62, 67])
    vi.advanceTimersByTime(PHRASE_GAP_MS)
    expect(performances[0]).toEqual([59, 62, 67])
  })

  it('does not split a phrase when notes are released one at a time', () => {
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.acceptForTesting([NOTE_OFF, 60, 0])
    vi.advanceTimersByTime(PHRASE_GAP_MS / 2)
    service.acceptForTesting([NOTE_ON, 64, 90])
    service.acceptForTesting([NOTE_OFF, 64, 0])
    vi.advanceTimersByTime(PHRASE_GAP_MS)

    expect(performances).toEqual([[60, 64]])
  })

  it('treats note-on at zero velocity as a release, which many keyboards send', () => {
    service.acceptForTesting([NOTE_ON, 60, 100])
    service.acceptForTesting([NOTE_ON, 60, 0])
    vi.advanceTimersByTime(PHRASE_GAP_MS)
    expect(performances).toEqual([[60]])
  })

  it('reports which keys are down as they are held', () => {
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.acceptForTesting([NOTE_ON, 67, 90])
    expect(active.at(-1)).toEqual([60, 67])
    service.acceptForTesting([NOTE_OFF, 60, 0])
    expect(active.at(-1)).toEqual([67])
  })

  it('sends immediately when the learner asks it to', () => {
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.acceptForTesting([NOTE_ON, 64, 90])
    service.submitNow()
    expect(performances).toEqual([[60, 64]])
  })

  it('ignores a repeated key rather than sending it twice', () => {
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.acceptForTesting([NOTE_OFF, 60, 0])
    vi.advanceTimersByTime(PHRASE_GAP_MS)
    expect(performances).toEqual([[60]])
  })

  it('emits nothing after a reset', () => {
    service.acceptForTesting([NOTE_ON, 60, 90])
    service.reset()
    vi.advanceTimersByTime(PHRASE_GAP_MS * 2)
    expect(performances).toHaveLength(0)
  })
})

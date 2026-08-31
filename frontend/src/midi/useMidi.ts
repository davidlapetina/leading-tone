import { useCallback, useEffect, useRef, useState } from 'react'
import { MidiService, type MidiDevice } from './MidiService'

interface UseMidiOptions {
  /** Called when a phrase finishes. The caller decides whether it is wanted right now. */
  onPerformance: (notes: number[]) => void
}

/**
 * Connects the keyboard to React.
 *
 * The service is built inside an effect and held in a ref, and the callback is held in a
 * second ref, so that changing what a finished phrase should do never rebuilds the
 * service and drops the MIDI connection under the learner's hands. Nothing here is read
 * during render.
 */
export function useMidi({ onPerformance }: UseMidiOptions) {
  const [devices, setDevices] = useState<MidiDevice[]>([])
  const [activeNotes, setActiveNotes] = useState<number[]>([])
  const [error, setError] = useState<string | null>(null)
  const [connected, setConnected] = useState(false)

  const handler = useRef(onPerformance)
  const service = useRef<MidiService | null>(null)

  useEffect(() => {
    handler.current = onPerformance
  }, [onPerformance])

  useEffect(() => {
    const midi = new MidiService({
      onActiveNotes: setActiveNotes,
      onDevices: (found) => {
        setDevices(found)
        setConnected(found.length > 0)
      },
      onError: setError,
      onPerformance: (notes) => handler.current(notes),
    })
    service.current = midi

    let cancelled = false
    void midi.connect().then((found) => {
      if (!cancelled) {
        setConnected(found.length > 0)
      }
    })

    return () => {
      cancelled = true
      midi.disconnect()
      service.current = null
    }
  }, [])

  const submitNow = useCallback(() => service.current?.submitNow(), [])
  const reset = useCallback(() => service.current?.reset(), [])

  return {
    devices,
    activeNotes,
    error,
    connected,
    supported: MidiService.isSupported(),
    submitNow,
    reset,
  }
}

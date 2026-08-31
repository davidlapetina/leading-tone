/**
 * The keyboard, kept behind one small class.
 *
 * <p>Nothing else in the application touches the Web MIDI API, so replacing it later
 * means replacing this file only. What comes out is a list of MIDI note numbers in the
 * order they were struck — the backend does all the judging.
 */

export type MidiEventType = 'NOTE_ON' | 'NOTE_OFF'

export interface MidiEvent {
  type: MidiEventType
  note: number
  velocity: number
  timestamp: number
}

export interface MidiDevice {
  id: string
  name: string
  manufacturer: string
}

export interface MidiServiceListeners {
  /** Fires on every key, so the interface can show what is under the fingers. */
  onActiveNotes?: (notes: number[]) => void
  /** Fires once a phrase has finished: every key released and a short pause. */
  onPerformance?: (notes: number[]) => void
  onDevices?: (devices: MidiDevice[]) => void
  onError?: (message: string) => void
}

/** How long to wait after the last key is released before deciding the answer is finished. */
export const PHRASE_GAP_MS = 550

export class MidiService {
  private access: MIDIAccess | null = null
  private readonly active = new Set<number>()
  private readonly listeners: MidiServiceListeners
  private struck: number[] = []
  private timer: ReturnType<typeof setTimeout> | null = null

  constructor(listeners: MidiServiceListeners = {}) {
    this.listeners = listeners
  }

  static isSupported(): boolean {
    return typeof navigator !== 'undefined' && typeof navigator.requestMIDIAccess === 'function'
  }

  async connect(): Promise<MidiDevice[]> {
    if (!MidiService.isSupported()) {
      this.listeners.onError?.(
        'This browser has no Web MIDI. Chrome or Edge over localhost or https will work.',
      )
      return []
    }
    try {
      this.access = await navigator.requestMIDIAccess({ sysex: false })
      this.access.onstatechange = () => this.bind()
      return this.bind()
    } catch (error) {
      this.listeners.onError?.(
        error instanceof Error ? error.message : 'Could not reach the MIDI devices.',
      )
      return []
    }
  }

  private bind(): MidiDevice[] {
    if (!this.access) {
      return []
    }
    const devices: MidiDevice[] = []
    this.access.inputs.forEach((input) => {
      input.onmidimessage = (message) => this.handle(message)
      devices.push({
        id: input.id,
        name: input.name ?? 'Unnamed device',
        manufacturer: input.manufacturer ?? '',
      })
    })
    this.listeners.onDevices?.(devices)
    return devices
  }

  /** 0x90 is note on, 0x80 is note off, and a note on at velocity 0 is also a note off. */
  private handle(message: MIDIMessageEvent) {
    const data = message.data
    if (!data || data.length < 3) {
      return
    }
    const command = data[0] & 0xf0
    const note = data[1]
    const velocity = data[2]

    if (command === 0x90 && velocity > 0) {
      this.noteOn(note)
    } else if (command === 0x80 || (command === 0x90 && velocity === 0)) {
      this.noteOff(note)
    }
  }

  private noteOn(note: number) {
    this.cancelTimer()
    this.active.add(note)
    if (!this.struck.includes(note)) {
      this.struck.push(note)
    }
    this.listeners.onActiveNotes?.([...this.active].sort((a, b) => a - b))
  }

  private noteOff(note: number) {
    this.active.delete(note)
    this.listeners.onActiveNotes?.([...this.active].sort((a, b) => a - b))
    if (this.active.size === 0 && this.struck.length > 0) {
      this.timer = setTimeout(() => this.finishPhrase(), PHRASE_GAP_MS)
    }
  }

  private finishPhrase() {
    const performance = [...this.struck]
    this.reset()
    if (performance.length > 0) {
      this.listeners.onPerformance?.(performance)
    }
  }

  /** Sends whatever has been played so far, for a learner who would rather not wait. */
  submitNow() {
    this.cancelTimer()
    this.finishPhrase()
  }

  reset() {
    this.cancelTimer()
    this.struck = []
    this.active.clear()
    this.listeners.onActiveNotes?.([])
  }

  private cancelTimer() {
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
  }

  disconnect() {
    this.cancelTimer()
    this.access?.inputs.forEach((input) => {
      input.onmidimessage = null
    })
    this.access = null
  }

  /** Exposed for tests: feeds a raw message in as if it came from the device. */
  acceptForTesting(data: number[]) {
    this.handle({ data: Uint8Array.from(data) } as MIDIMessageEvent)
  }
}

/**
 * A small synthesised piano, so the on-screen keyboard makes a sound.
 *
 * Web Audio rather than samples: nothing to download, nothing to bundle, and it works
 * offline. Two detuned triangles with a struck envelope is not a Steinway, but it is
 * enough to hear an interval as an interval and a chord as a chord — which is the point.
 */

let context: AudioContext | null = null

function audio(): AudioContext | null {
  if (typeof window === 'undefined') {
    return null
  }
  if (!context) {
    const Ctor = window.AudioContext ?? (window as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!Ctor) {
      return null
    }
    context = new Ctor()
  }
  // Browsers start the context suspended until a gesture; a click is one.
  if (context.state === 'suspended') {
    void context.resume()
  }
  return context
}

const frequency = (midi: number) => 440 * Math.pow(2, (midi - 69) / 12)

/** Strikes one note. Safe to call rapidly; each note gets its own short-lived voice. */
export function strike(midi: number, seconds = 1.1) {
  const ctx = audio()
  if (!ctx) {
    return
  }
  const now = ctx.currentTime
  const hz = frequency(midi)

  const gain = ctx.createGain()
  // Struck, not blown: fast attack, immediate decay, no sustain to speak of.
  gain.gain.setValueAtTime(0.0001, now)
  gain.gain.exponentialRampToValueAtTime(0.22, now + 0.008)
  gain.gain.exponentialRampToValueAtTime(0.08, now + 0.18)
  gain.gain.exponentialRampToValueAtTime(0.0001, now + seconds)

  const tone = ctx.createBiquadFilter()
  tone.type = 'lowpass'
  tone.frequency.setValueAtTime(Math.min(hz * 8, 9000), now)

  for (const [wave, detune, level] of [
    ['triangle', 0, 1],
    ['sine', 6, 0.5],
  ] as const) {
    const osc = ctx.createOscillator()
    osc.type = wave
    osc.frequency.setValueAtTime(hz, now)
    osc.detune.setValueAtTime(detune, now)
    const voice = ctx.createGain()
    voice.gain.setValueAtTime(level, now)
    osc.connect(voice).connect(tone)
    osc.start(now)
    osc.stop(now + seconds + 0.05)
  }

  tone.connect(gain).connect(ctx.destination)
}

/** Plays a chord or a line. Spacing of 0 sounds them together. */
export function play(notes: number[], spacingSeconds = 0) {
  notes.forEach((midi, index) => {
    if (spacingSeconds === 0) {
      strike(midi)
    } else {
      window.setTimeout(() => strike(midi), index * spacingSeconds * 1000)
    }
  })
}

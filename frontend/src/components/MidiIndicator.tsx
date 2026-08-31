import type { MidiDevice } from '../midi/MidiService'

interface MidiIndicatorProps {
  devices: MidiDevice[]
  supported: boolean
  error: string | null
}

export function MidiIndicator({ devices, supported, error }: MidiIndicatorProps) {
  if (!supported) {
    return (
      <span className="midi midi-off" title="Chrome or Edge, over localhost or https">
        MIDI unavailable
      </span>
    )
  }
  if (error) {
    const blocked = /permission|denied|not granted/i.test(error)
    return (
      <span className="midi midi-off" title={error}>
        {blocked ? 'MIDI blocked' : 'MIDI unavailable'}
      </span>
    )
  }
  if (devices.length === 0) {
    return <span className="midi midi-off">No keyboard</span>
  }
  return (
    <span className="midi midi-on" title={devices.map((device) => device.name).join(', ')}>
      ● {devices[0].name}
    </span>
  )
}

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Settings } from '../api/types'

/**
 * The application's own configuration, kept in the database rather than the environment.
 *
 * Changing the model takes effect on the next turn: it is rebuilt, not re-read. Which
 * matters, because on a laptop the difference between a usable tutor and an unusable one
 * is usually which model fits in memory.
 */
export function SettingsView() {
  const queryClient = useQueryClient()
  const stored = useQuery({ queryKey: ['settings'], queryFn: api.settings })
  const models = useQuery({ queryKey: ['models'], queryFn: api.availableModels })
  // null until the form is touched, so what is on screen follows what is stored until the
  // moment the learner starts editing it.
  const [edited, setEdited] = useState<Settings | null>(null)
  const draft = edited ?? stored.data ?? null

  const afterSave = {
    onSuccess: (saved: Settings) => {
      setEdited(null)
      void saved
      void queryClient.invalidateQueries({ queryKey: ['settings'] })
      void queryClient.invalidateQueries({ queryKey: ['status'] })
      void queryClient.invalidateQueries({ queryKey: ['models'] })
    },
  }
  const save = useMutation({ mutationFn: api.saveSettings, ...afterSave })
  const reset = useMutation({ mutationFn: api.resetSettings, ...afterSave })

  if (!draft) {
    return <div className="view">Loading settings…</div>
  }

  const set = <K extends keyof Settings>(key: K, value: Settings[K]) =>
    setEdited({ ...draft, [key]: value })

  const dirty = stored.data ? JSON.stringify(draft) !== JSON.stringify(stored.data) : false

  return (
    <div className="view view-settings">
      <header className="view-head">
        <div>
          <h1>Settings</h1>
          <p className="view-sub">
            Kept in the database, not the environment. A change to the model takes effect on the
            next turn — there is nothing to restart.
          </p>
        </div>
        <div className="settings-actions">
          <button type="button" className="btn-ghost" onClick={() => reset.mutate()} disabled={reset.isPending}>
            Restore defaults
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={() => save.mutate(draft)}
            disabled={!dirty || save.isPending}
          >
            {save.isPending ? 'Saving…' : dirty ? 'Save changes' : 'Saved'}
          </button>
        </div>
      </header>

      <section className="panel-card">
        <h2>The teacher's voice</h2>
        <p className="hint">
          Everything that decides <em>what</em> to teach is computed and does not depend on any of
          this. Turn the model off entirely and the tutor still works — plainer, and instant.
        </p>

        <Field label="Use a language model" hint="Off gives short, deterministic teaching with no wait.">
          <Toggle value={draft.llmEnabled} onChange={(v) => set('llmEnabled', v)} />
        </Field>

        <Field label="Model" hint={models.data?.reachable ? 'Installed on this machine' : 'Ollama is not reachable'}>
          {models.data?.reachable && models.data.models.length > 0 ? (
            <select value={draft.model} onChange={(event) => set('model', event.target.value)}>
              {[...new Set([draft.model, ...models.data.models])].map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          ) : (
            <input value={draft.model} onChange={(event) => set('model', event.target.value)} />
          )}
        </Field>

        <Field label="Ollama address" hint="Where the model is served from.">
          <input value={draft.baseUrl} onChange={(event) => set('baseUrl', event.target.value)} />
        </Field>

        <Field label="Theory tools" hint="Lets the model look chords up. Small models type the call out as text instead.">
          <Toggle value={draft.toolsEnabled} onChange={(v) => set('toolsEnabled', v)} />
        </Field>

        <Field
          label="Reasoning"
          hint="Qwen3 thinks before answering. Measured here: three times slower, and the teaching was worse."
        >
          <Toggle value={draft.think} onChange={(v) => set('think', v)} />
        </Field>
      </section>

      <section className="panel-card">
        <h2>Tuning</h2>
        <Field label="Temperature" hint="Lower is more predictable; too low and it repeats itself.">
          <input
            type="number"
            step="0.1"
            min="0"
            max="2"
            value={draft.temperature}
            onChange={(event) => set('temperature', Number(event.target.value))}
          />
        </Field>
        <Field label="Context window" hint="Too small and each turn is slower than the last.">
          <input
            type="number"
            step="1024"
            value={draft.numCtx}
            onChange={(event) => set('numCtx', Number(event.target.value))}
          />
        </Field>
        <Field label="Conversation kept" hint="How many past messages the model can see.">
          <input
            type="number"
            value={draft.memoryMessages}
            onChange={(event) => set('memoryMessages', Number(event.target.value))}
          />
        </Field>
        <Field label="Timeout (seconds)" hint="How long to wait for a turn before giving up on it.">
          <input
            type="number"
            value={draft.timeoutSeconds}
            onChange={(event) => set('timeoutSeconds', Number(event.target.value))}
          />
        </Field>
        <Field label="Cooldown (seconds)" hint="After a failure, how long to stop calling the model.">
          <input
            type="number"
            value={draft.cooldownSeconds}
            onChange={(event) => set('cooldownSeconds', Number(event.target.value))}
          />
        </Field>
      </section>

      <section className="panel-card">
        <h2>You</h2>
        <Field label="What to call you" hint="Used when the tutor addresses you.">
          <input value={draft.learnerName} onChange={(event) => set('learnerName', event.target.value)} />
        </Field>
      </section>
    </div>
  )
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="setting">
      <span className="setting-label">
        {label}
        {hint && <span className="setting-hint">{hint}</span>}
      </span>
      <span className="setting-control">{children}</span>
    </label>
  )
}

function Toggle({ value, onChange }: { value: boolean; onChange: (value: boolean) => void }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={value}
      className={`toggle${value ? ' toggle-on' : ''}`}
      onClick={() => onChange(!value)}
    >
      <span className="toggle-knob" />
    </button>
  )
}

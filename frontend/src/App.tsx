import { useCallback, useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api/client'
import { Sidebar, type View } from './components/Sidebar'
import { useMidi } from './midi/useMidi'
import { useTutorStore } from './state/tutorStore'
import { AskView } from './views/AskView'
import { LearnView } from './views/LearnView'
import { LessonView } from './views/LessonView'
import { PractiseView } from './views/PractiseView'
import { ProgressView } from './views/ProgressView'
import { SettingsView } from './views/SettingsView'

export default function App() {
  const { sessionId, entries, current, busy, error, start, send, play, clearError } = useTutorStore()
  const [view, setView] = useState<View>('learn')
  const [lessonId, setLessonId] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: status } = useQuery({ queryKey: ['status'], queryFn: api.status })

  const awaitingKeyboard = Boolean(
    current?.expectsAnswer && current.answerMode === 'MIDI' && current.exerciseId,
  )

  const onPerformance = useCallback(
    (notes: number[]) => {
      if (awaitingKeyboard) {
        void play(notes)
      }
    },
    [awaitingKeyboard, play],
  )
  const midi = useMidi({ onPerformance })

  useEffect(() => {
    if (!sessionId) {
      void start()
    }
  }, [sessionId, start])

  // The learner model moves with every turn, so anything reading it refetches after one.
  useEffect(() => {
    void queryClient.invalidateQueries({ queryKey: ['learner'] })
    void queryClient.invalidateQueries({ queryKey: ['next-action'] })
    void queryClient.invalidateQueries({ queryKey: ['evidence'] })
  }, [entries.length, queryClient])

  const openLesson = (conceptId: string) => {
    setLessonId(conceptId)
    setView('learn')
  }

  const practise = async (conceptId: string) => {
    await api.focusConcept(conceptId)
    void queryClient.invalidateQueries({ queryKey: ['learner'] })
    setLessonId(null)
    setView('practise')
    void send('')
  }

  const ask = async (conceptId: string, question: string) => {
    await api.focusConcept(conceptId)
    setLessonId(null)
    setView('practise')
    void send(question)
  }

  const letTutorChoose = async () => {
    await api.clearFocus()
    void queryClient.invalidateQueries({ queryKey: ['learner'] })
  }

  return (
    <div className="app">
      <Sidebar
        view={view}
        onChange={(next) => {
          setLessonId(null)
          setView(next)
        }}
      />

      <div className="app-body">
        <header className="topbar">
          <span className="topbar-title">
            {view === 'learn' && lessonId
              ? 'Lesson'
              : view === 'learn'
                ? 'Topics'
                : view === 'practise'
                  ? 'With the tutor'
                  : view === 'ask'
                    ? 'Ask anything'
                    : view === 'progress'
                      ? 'Your progress'
                      : 'Settings'}
          </span>
          <div className="topbar-right">
            {status && (
              <span className="pill" title={status.narrator}>
                {status.languageModelAvailable ? status.model : 'deterministic'}
              </span>
            )}
            <span className={`pill ${midi.connected ? 'pill-good' : ''}`}>
              {midi.connected ? (midi.devices[0]?.name ?? 'keyboard') : 'on-screen piano'}
            </span>
          </div>
        </header>

        {error && (
          <div className="banner" role="alert">
            {error}
            <button type="button" onClick={clearError}>
              dismiss
            </button>
          </div>
        )}

        <main className="app-main">
          {view === 'learn' && !lessonId && <LearnView onOpen={openLesson} onPractise={practise} />}
          {view === 'learn' && lessonId && (
            <LessonView
              conceptId={lessonId}
              onBack={() => setLessonId(null)}
              onPractise={practise}
              onAsk={ask}
              onOpen={openLesson}
            />
          )}
          {view === 'ask' && <AskView />}
          {view === 'practise' && (
            <PractiseView
              entries={entries}
              busy={busy}
              rationale={current?.rationale}
              conceptName={current?.conceptName}
              onSend={(text) => void send(text)}
              onPlayNotes={(notes) => void play(notes)}
              onSubmitPlaying={midi.submitNow}
              awaitingKeyboard={awaitingKeyboard}
              midi={midi}
              onLeaveTopic={() => void letTutorChoose()}
            />
          )}
          {view === 'progress' && <ProgressView onOpen={openLesson} />}
          {view === 'settings' && <SettingsView />}
        </main>
      </div>
    </div>
  )
}

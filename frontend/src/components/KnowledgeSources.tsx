import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/client'
import type { IngestReport, KnowledgeSource } from '../api/types'

/**
 * Bringing published sources in.
 *
 * <p>Nothing downloads on its own. Ingestion reaches the internet and takes minutes, so it
 * is always something a person chose, and the licence is shown at the moment of choosing
 * rather than buried in a notices file. This application is MIT; none of these sources
 * are, and that distinction is the reason this panel looks the way it does.
 */
export function KnowledgeSources() {
  const client = useQueryClient()
  const status = useQuery({ queryKey: ['knowledge-status'], queryFn: api.knowledgeStatus })
  const sources = useQuery({ queryKey: ['knowledge-sources'], queryFn: api.knowledgeSources })
  const [reports, setReports] = useState<Record<string, IngestReport>>({})
  const [showCorpora, setShowCorpora] = useState(false)

  const ingest = useMutation({
    mutationFn: (id: string) => api.ingestSource(id),
    onSuccess: (report) => {
      setReports((previous) => ({ ...previous, [report.sourceId]: report }))
      void client.invalidateQueries({ queryKey: ['knowledge-status'] })
      void client.invalidateQueries({ queryKey: ['knowledge-sources'] })
    },
  })

  if (sources.isPending || status.isPending) {
    return <p className="hint">Looking at what is available…</p>
  }
  if (sources.isError || !sources.data || !status.data) {
    return <p className="hint">Could not read the source list.</p>
  }

  const text = sources.data.filter((s) => s.ingestionMode === 'TEXT_RAG')
  const corpora = sources.data.filter((s) => s.ingestionMode === 'STRUCTURED_HARMONY')
  const indexed = status.data.chunks > 0

  return (
    <>
      <p className="hint">
        The tutor teaches from its own theory engine. These are published sources it can
        quote on top of that, so an explanation can cite where it came from. Nothing is
        downloaded until you ask for it here.
      </p>

      <div className="knowledge-status">
        {indexed ? (
          <>
            <strong>{status.data.chunks.toLocaleString()}</strong> passages from{' '}
            <strong>{status.data.documents}</strong> chapters, searchable by meaning and by
            exact notation.
            {status.data.vectorSearch ? (
              <span className="knowledge-note"> Embeddings run in this application, on your machine.</span>
            ) : (
              <span className="knowledge-note"> Word search only — no embedding model loaded.</span>
            )}
          </>
        ) : (
          <>Nothing has been brought in yet. The tutor works without this.</>
        )}
      </div>

      <ul className="source-list">
        {text.map((source) => (
          <SourceRow
            key={source.id}
            source={source}
            report={reports[source.id]}
            busy={ingest.isPending && ingest.variables === source.id}
            onIngest={() => ingest.mutate(source.id)}
          />
        ))}
      </ul>

      <button type="button" className="btn-ghost source-toggle" onClick={() => setShowCorpora(!showCorpora)}>
        {showCorpora ? 'Hide' : 'Show'} annotated score corpora ({corpora.length})
      </button>

      {showCorpora && (
        <>
          <p className="hint">
            Harmonic analyses of real music, used to answer questions like “show me a
            Beethoven example of V/V” from an actual score instead of inventing one. Every
            one of these is NonCommercial, and each asks to be cited its own way.
          </p>
          <ul className="source-list">
            {corpora.map((source) => (
              <SourceRow
                key={source.id}
                source={source}
                report={reports[source.id]}
                busy={ingest.isPending && ingest.variables === source.id}
                onIngest={() => ingest.mutate(source.id)}
              />
            ))}
          </ul>
        </>
      )}

      {ingest.isError && (
        <p className="source-error">{(ingest.error as Error).message}</p>
      )}
    </>
  )
}

function SourceRow({
  source,
  report,
  busy,
  onIngest,
}: {
  source: KnowledgeSource
  report?: IngestReport
  busy: boolean
  onIngest: () => void
}) {
  return (
    <li className="source-row" data-source-id={source.id}>
      <div className="source-main">
        <div className="source-head">
          <span className="source-name">{source.name}</span>
          <LicenceBadge source={source} />
        </div>
        <div className="source-meta">
          {source.retrievable ? (
            <span className="source-state source-state-active">
              In use · {source.chunks.toLocaleString()} passages
            </span>
          ) : (
            <span className="source-state">Not brought in</span>
          )}
          {source.lastError && <span className="source-failed">{source.lastError}</span>}
        </div>
        {report && <IngestSummary report={report} />}
      </div>
      <button type="button" className="btn-ghost" onClick={onIngest} disabled={busy}>
        {busy ? 'Fetching…' : source.retrievable ? 'Refresh' : 'Bring in'}
      </button>
    </li>
  )
}

/** The terms, stated plainly. Attribution and ShareAlike survive being indexed. */
function LicenceBadge({ source }: { source: KnowledgeSource }) {
  const label = source.license.replace('CC-', 'CC ').replace('-4.0', ' 4.0').replace(/-/g, '-')
  const title = [
    source.licenseName,
    source.commercialUseAllowed ? null : 'NonCommercial: not for commercial use',
    source.shareAlikeRequired ? 'ShareAlike: adaptations keep this licence' : null,
    source.attributionRequired ? 'Attribution required' : null,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <a
      className={`licence-badge${source.commercialUseAllowed ? '' : ' licence-badge-nc'}`}
      href={source.licenseUrl ?? undefined}
      target="_blank"
      rel="noreferrer"
      title={title}
    >
      {label}
    </a>
  )
}

/**
 * What the run actually did — including what it refused. "We have the whole book" and "we
 * have it except one chapter" are different claims, so the difference is shown.
 */
function IngestSummary({ report }: { report: IngestReport }) {
  if (report.skipped) {
    return <p className="source-report">Already up to date.</p>
  }
  return (
    <div className="source-report">
      <p>
        {report.documentsIngested} of {report.documentsSeen} brought in ·{' '}
        {report.chunksWritten.toLocaleString()} passages
      </p>
      {report.skippedForLicense.length > 0 && (
        <p className="source-refused">
          Not included, because their licence does not permit it:{' '}
          {report.skippedForLicense.join('; ')}
        </p>
      )}
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { MusicalExample } from '../api/types'
import { Score } from './Score'

/**
 * Real music, cited.
 *
 * <p>The reason this component exists is that "Beethoven does this" is a claim nobody can
 * check. Showing the bars, with the work and the bar number, makes it a claim anybody can.
 *
 * <p>When there is no verified example it says so. That is the honest answer, and filling
 * the space with something generated while the heading says the composer's name would be
 * the exact failure this whole subsystem is built to prevent.
 */
export function CorpusExample({ conceptId, limit = 2 }: { conceptId: string; limit?: number }) {
  const examples = useQuery({
    queryKey: ['examples', conceptId, limit],
    queryFn: () => api.examplesForConcept(conceptId, limit),
    retry: false,
  })

  if (examples.isPending) {
    return null
  }
  if (examples.isError || !examples.data) {
    // Say so rather than rendering an empty heading. A section that silently disappears
    // when the response shape changes is how a broken contract goes unnoticed.
    return <p className="example-none">Could not load examples for this topic.</p>
  }
  if (examples.data.found === 0) {
    return (
      <p className="example-none">
        No example of this in the scores currently loaded. You can add annotated corpora in
        Settings.
      </p>
    )
  }

  return (
    <div className="example-set">
      {examples.data.examples.map((example, index) => (
        <ExampleCard key={`${example.citation}-${index}`} example={example} />
      ))}
    </div>
  )
}

function ExampleCard({ example }: { example: MusicalExample }) {
  const verified = example.origin === 'VERIFIED_CORPUS'
  return (
    <figure className="example-card" data-origin={example.origin}>
      <figcaption className="example-head">
        <span className="example-cite">{example.citation}</span>
        <span className={`origin-badge${verified ? ' origin-badge-verified' : ''}`}>
          {verified ? 'from the score' : 'written as an example'}
        </span>
      </figcaption>
      {example.romanNumeral && (
        <p className="example-what">
          {example.romanNumeral}
          {example.globalKey ? ` in ${example.globalKey}` : ''}
        </p>
      )}
      {example.abc ? (
        <Score abc={example.abc} staffwidth={620} />
      ) : (
        <p className="example-none">The notes for this passage are not loaded.</p>
      )}
      {example.attribution && <p className="example-licence">{example.attribution}</p>}
    </figure>
  )
}

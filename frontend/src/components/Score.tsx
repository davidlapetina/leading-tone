import { useEffect, useRef } from 'react'

interface ScoreProps {
  abc: string
}

/**
 * Renders ABC as engraved notation. The backend produces the ABC; abcjs draws it.
 *
 * <p>abcjs carries a synthesiser and is by far the largest thing the page loads, so it is
 * imported on first use rather than in the main bundle.
 */
export function Score({ abc }: ScoreProps) {
  const host = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let cancelled = false
    void import('abcjs').then((abcjs) => {
      if (cancelled || !host.current) {
        return
      }
      abcjs.default.renderAbc(host.current, abc, {
        responsive: 'resize',
        staffwidth: 420,
        paddingtop: 4,
        paddingbottom: 4,
        paddingleft: 0,
        paddingright: 0,
      })
    })
    return () => {
      cancelled = true
    }
  }, [abc])

  return <div className="score" ref={host} aria-label="notation" />
}

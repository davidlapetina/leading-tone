# Frontend

The browser client: React 19, TypeScript, Vite. It renders the conversation, the concept
catalogue, the lessons, the on-screen keyboard, and the engraved score examples.

For what the application *is*, read the [root README](../README.md). This file is about
working on the frontend.

## Running it

```shell
npm install
npm run dev                 # http://localhost:5173, proxied to the backend on 8088
```

The backend has to be running (`make backend` from the repo root). In production there is no
separate server: `make package` builds this into static files and bundles them into the
backend jar, so the whole application is one `java -jar`.

```shell
npm test                    # vitest
npm run test:e2e            # playwright, needs the backend running
npm run typecheck           # tsc --noEmit
npm run lint                # oxlint
```

## Layout

| Path | What lives there |
|---|---|
| `api/` | The HTTP client and the **Zod schemas** every response is parsed through |
| `views/` | One file per screen: tutor, learn, lesson, settings |
| `components/` | `Score` (engraved notation), `CorpusExample`, `VirtualKeyboard`, `Conversation`, and the rest |
| `state/` | Zustand stores for session and UI state |
| `midi/` | Web MIDI input, and the fallback when no device is attached |

Server state is TanStack Query; local state is Zustand. Nothing else holds state.

## The API boundary is validated

Every response is parsed with Zod before anything touches it. This is not ceremony: a field
that quietly disappeared from a backend DTO once left a component rendering `null` with no
error anywhere, and the bug was only visible in a screenshot. A schema mismatch now fails
loudly at the boundary instead.

Keep the schemas in `api/types.ts` in step with the backend DTOs. If a component needs a
field, it belongs in the schema.

## Notation

`Score.tsx` renders ABC through **abcjs**, imported lazily so the ~500 KB library is not in
the initial bundle. The ABC comes from the backend, generated from the note tables of
annotated corpora — the frontend never constructs notation.

Two invariants hold on everything the backend sends, and both are tested there rather than
here: every bar is exactly as long as its metre says, and every duration is a real note value.
An excerpt whose rhythm cannot be notated exactly arrives with **no** score rather than an
approximate one, so a missing score is a deliberate answer and the component renders the
citation without it.

## Styling

One stylesheet, `styles.css`, with CSS custom properties for the palette. Warm paper and ink
rather than a dashboard, and the score sits on the page ground rather than in a boxed viewer.
There is no CSS framework and no component library; don't add one for a single screen.

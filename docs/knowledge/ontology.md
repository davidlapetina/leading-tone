# Concepts

`backend/src/main/resources/concepts.json` holds 36 concepts and their prerequisites. It is
the source of truth; the database mirrors it at startup and the graph is validated on load —
a duplicate id, an unknown prerequisite or a cycle stops the application rather than
producing a subtly wrong curriculum later.

## Shape

```json
{
  "id": "secondary-dominant",
  "name": "Secondary dominants",
  "description": "...",
  "prerequisites": ["dominant-seventh", "roman-numeral", "key-signature"],
  "category": "HARMONY",
  "intrinsicDifficulty": 0.75,
  "tradition": "GENERAL"
}
```

`prerequisites` is a statement about what depends on what, not an order of teaching. The
policy decides order; the graph decides what is possible.

## Tradition

`GENERAL` (22), `CLASSICAL` (4), `JAZZ` (10).

The point of the field is to stop one practice's conventions being taught as universal law.
A ii–V–I and a cadential six-four are both correct and are not correct about the same thing;
somebody working through jazz should not be told a chord symbol is wrong because a figured
bass would write it differently.

Most theory is shared, so `GENERAL` is the default and the majority. `CLASSICAL` is reserved
for the genuinely idiomatic — figured bass, voice leading, counterpoint.

Tradition also decides where examples come from: a jazz concept is illustrated from the
jazz corpus, a classical one from the annotated scores. Searching everything would answer
"show me a secondary dominant" with a jazz standard, which is true and not what was asked.

## The jazz path

The interface offers a route through the jazz concepts in the order a jazz musician meets
them, grouped into three themes. Those concepts also appear in their ordinary categories —
the path is a route through the graph, not a separate graph. Walling jazz off would teach it
badly, since almost all of its theory is the same theory.

## Adding a concept

Add an object here, then teach the rest of the application about it:

- an exercise menu and generator in `ExerciseGenerator` — two tests enforce this, because a
  concept nobody can be asked about produces no evidence and its mastery never moves;
- a lesson in `LessonService`, or the page is empty;
- optionally a `TheoryBriefing` entry, so the model is given computed facts about it.

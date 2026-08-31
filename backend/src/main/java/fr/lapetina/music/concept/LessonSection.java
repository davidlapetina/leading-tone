package fr.lapetina.music.concept;

import java.util.List;

/**
 * One part of a lesson: a heading, some points, and optionally a worked example in
 * notation.
 *
 * <p>Everything in it is computed by the theory engine — the same engine that marks the
 * answers — so what the learner is taught and what they are marked against cannot drift
 * apart. Nothing here is written by a language model.
 *
 * @param abc     ABC notation for a worked example, or null
 * @param caption what the example shows
 */
public record LessonSection(String heading, List<String> points, String abc, String caption) {

    public static LessonSection of(String heading, String... points) {
        return new LessonSection(heading, List.of(points), null, null);
    }

    public LessonSection showing(String abc, String caption) {
        return new LessonSection(heading, points, abc, caption);
    }
}

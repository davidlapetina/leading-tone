package fr.lapetina.music.tutor;

/**
 * A concept the learner brought up themselves.
 *
 * @param conceptId the concept the graph recognised
 * @param phrase    the words they used, so the tutor can answer in their terms
 */
public record LearnerFocus(String conceptId, String phrase) {
}

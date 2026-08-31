package fr.lapetina.music.llm;

import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.tutor.TeachingDecision;

/**
 * The tutor with no language model at all.
 *
 * <p>It is deliberately not a stub: the policy, the exercise generator and the evaluator
 * do the teaching, and this only supplies the connecting words. The application is
 * usable, if terse, with Ollama switched off entirely — which also makes every test able
 * to run without a model.
 */
public class TemplateTutor implements TutorModel {

    @Override
    public String respond(TutorRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.evaluationFeedback() != null && !request.evaluationFeedback().isBlank()) {
            builder.append(request.evaluationFeedback()).append(' ');
        }
        builder.append(acknowledgement(request.decision()));
        builder.append(opening(request.decision()));
        Exercise exercise = request.exercise();
        if (exercise != null) {
            builder.append(' ').append(exercise.prompt);
        }
        return builder.toString().trim();
    }

    /**
     * Says out loud that the question was heard. A learner who asks about seventh chords
     * and is answered with a question about note names has been ignored, whatever the
     * policy did underneath.
     */
    private String acknowledgement(TeachingDecision decision) {
        String asked = decision.learnerAskedAbout();
        if (asked == null) {
            return "";
        }
        String topic = asked.replace('-', ' ');
        if (asked.equals(decision.conceptId())) {
            return "";
        }
        return "You asked about %s. That rests on %s, so let's get that solid first. "
                .formatted(topic, decision.conceptName().toLowerCase());
    }

    private String opening(TeachingDecision decision) {
        String name = decision.conceptName().toLowerCase();
        return switch (decision.action()) {
            case DIAGNOSE -> "Before anything else, let's find out where you are.";
            case INTRODUCE -> "Here is something new: " + name + ".";
            case EXPLAIN -> "Let's stay with " + name + " a little longer.";
            case PRACTICE -> "Staying with " + name + ".";
            case REINFORCE -> name.substring(0, 1).toUpperCase() + name.substring(1)
                    + " is holding the next step up, so let's shore it up.";
            case CHALLENGE -> "You have " + name + " fairly well. Something harder:";
            case TRANSFER -> "Now somewhere you have not used " + name + " before.";
            case REVIEW -> "Back to " + name + " for a moment.";
            case CORRECT_MISCONCEPTION -> decision.misconception() == null
                    ? "Something about " + name + " needs another look."
                    : decision.misconception().description();
            case ANSWER_QUESTION -> "You asked about " + name + ".";
        };
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String describe() {
        return "template (no language model)";
    }
}

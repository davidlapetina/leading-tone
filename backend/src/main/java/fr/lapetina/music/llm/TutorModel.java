package fr.lapetina.music.llm;

/**
 * The only thing the tutor asks a language model to do: write the teacher's next turn.
 *
 * <p>Keeping this behind an interface is what stops the pedagogy from being coupled to
 * OpenAI, Anthropic or Ollama — and what lets the application keep working when there is
 * no model available at all.
 */
public interface TutorModel {

    String respond(TutorRequest request);

    /** False when the application is running without a reachable model. */
    boolean isAvailable();

    /** Short name of whatever actually answered, for the API to report. */
    String describe();
}

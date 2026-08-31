package fr.lapetina.music.learner;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * How this learner likes to be taught, inferred from behaviour rather than asked about.
 * Every value is 0..1 and starts neutral.
 */
@Embeddable
public class LearnerPreferences {

    private static final double NUDGE = 0.04;

    @Column(name = "explanation_depth", nullable = false)
    public double explanationDepth = 0.5;

    @Column(name = "socratic_preference", nullable = false)
    public double socraticPreference = 0.5;

    @Column(name = "notation_preference", nullable = false)
    public double notationPreference = 0.5;

    @Column(name = "keyboard_preference", nullable = false)
    public double keyboardPreference = 0.5;

    @Column(name = "aural_preference", nullable = false)
    public double auralPreference = 0.5;

    @Column(name = "example_preference", nullable = false)
    public double examplePreference = 0.5;

    @Column(name = "abstraction_tolerance", nullable = false)
    public double abstractionTolerance = 0.5;

    /** Moves a preference towards 1 when something worked, towards 0 when it did not. */
    public static double nudge(double current, boolean worked) {
        double target = worked ? 1.0 : 0.0;
        return clamp(current + (target - current) * NUDGE);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

}

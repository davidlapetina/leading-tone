package fr.lapetina.music.tutor;

import fr.lapetina.music.exercise.AnswerMode;
import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.learner.Learner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SessionService {

    @Transactional
    public TutorSession start(Learner learner) {
        TutorSession session = new TutorSession();
        session.learner = learner;
        session.persist();
        return session;
    }

    public Optional<TutorSession> find(UUID id) {
        return TutorSession.findByIdOptional(id);
    }

    /** The session an answer belongs to when none was named: the latest, or a new one. */
    @Transactional
    public TutorSession currentOrNew(Learner learner) {
        TutorSession latest = TutorSession
                .<TutorSession>find("learner = ?1 and endedAt is null order by startedAt desc", learner)
                .firstResult();
        return latest != null ? latest : start(learner);
    }

    @Transactional
    public TutorSession end(TutorSession session) {
        session.endedAt = java.time.Instant.now();
        return session;
    }

    @Transactional
    public Interaction append(TutorSession session, InteractionRole role, String content,
                              TeachingDecision decision, Exercise exercise, String notationAbc) {
        Interaction interaction = new Interaction();
        interaction.session = session;
        interaction.sequence = nextSequence(session);
        interaction.role = role;
        interaction.content = content;
        if (decision != null) {
            interaction.teachingAction = decision.action();
            interaction.targetConcepts = decision.conceptId();
        }
        if (exercise != null) {
            interaction.exerciseId = exercise.id;
            interaction.expectsAnswer = true;
            interaction.answerMode = exercise.answerMode;
        } else {
            interaction.answerMode = AnswerMode.NONE;
        }
        interaction.notationAbc = notationAbc;
        interaction.persist();
        return interaction;
    }

    /**
     * One past the highest sequence in this session. Counting rows instead would repeat a
     * number as soon as any interaction were ever removed, and the unique constraint on
     * (session, sequence) would then reject the next turn.
     */
    private int nextSequence(TutorSession session) {
        Interaction last = Interaction
                .<Interaction>find("session = ?1 order by sequence desc", session)
                .firstResult();
        return last == null ? 0 : last.sequence + 1;
    }

    public List<Interaction> interactions(TutorSession session) {
        return Interaction.find("session = ?1 order by sequence", session).list();
    }
}

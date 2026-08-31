package fr.lapetina.music.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.lapetina.music.exercise.Exercise;
import fr.lapetina.music.exercise.ExerciseService;
import fr.lapetina.music.exercise.ExpectedAnswer;
import fr.lapetina.music.learner.Learner;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Note;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The whole loop over HTTP: start a session, get an exercise, answer it, and see the
 * learner model move. The language model is switched off in the test profile, so what is
 * being tested here is the tutor itself rather than a model's prose.
 */
@QuarkusTest
class TutorFlowTest {

    @Inject
    ExerciseService exerciseService;

    @BeforeEach
    @Transactional
    void startFromNothing() {
        Learner.deleteAll();
    }

    @Test
    @DisplayName("a first session opens with a diagnosis rather than a lesson")
    void opensBySizingUpTheLearner() {
        given().when().post("/api/session")
                .then().statusCode(200)
                .body("action", is("DIAGNOSE"))
                .body("conceptId", is("note"))
                .body("message", notNullValue())
                .body("expectsAnswer", is(true))
                .body("exerciseId", notNullValue())
                .body("rationale", notNullValue());
    }

    @Test
    void answeringCorrectlyMovesTheLearnerModel() {
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        UUID exerciseId = UUID.fromString(opening.path("exerciseId"));

        Exercise exercise = exerciseService.find(exerciseId).orElseThrow();
        ExpectedAnswer expected = exerciseService.expectedAnswerOf(exercise);

        Response answered = given()
                .contentType(ContentType.JSON)
                .body(Map.of("answer", expected.canonical()))
                .when().post("/api/exercises/{id}/answer", exerciseId)
                .then().statusCode(200)
                .body("attempt.outcome.result", is("CORRECT"))
                .body("attempt.conceptId", is(exercise.conceptId))
                .body("attempt.evidenceRecorded", is(true))
                .extract().response();

        double before = ((Number) answered.path("attempt.masteryBefore")).doubleValue();
        double after = ((Number) answered.path("attempt.masteryAfter")).doubleValue();
        assertTrue(after > before, before + " should have risen, got " + after);

        given().when().get("/api/learner")
                .then().statusCode(200)
                .body("concepts.find { it.conceptId == '%s' }.mastery".formatted(exercise.conceptId),
                        greaterThan(0.0f));

        given().when().get("/api/learner/evidence")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].conceptId", is(exercise.conceptId));
    }

    @Test
    @DisplayName("a wrong answer is recorded as evidence too, and comes back explained")
    void answeringWronglyIsAlsoEvidence() {
        Response opening = given().when().post("/api/session").then().extract().response();
        UUID exerciseId = UUID.fromString(opening.path("exerciseId"));

        given().contentType(ContentType.JSON)
                .body(Map.of("answer", "definitely not the answer"))
                .when().post("/api/exercises/{id}/answer", exerciseId)
                .then().statusCode(200)
                .body("attempt.outcome.result", is("INCORRECT"))
                .body("attempt.outcome.feedback", notNullValue())
                .body("attempt.evidenceRecorded", is(true));
    }

    @Test
    void aPlayedChordIsJudgedFromTheMidiItself() {
        given().when().post("/api/session").then().statusCode(200);

        Response created = given().contentType(ContentType.JSON)
                .body(Map.of("conceptId", "chord-inversion", "difficulty", 0.5, "answerMode", "MIDI"))
                .when().post("/api/exercises")
                .then().statusCode(200)
                .body("answerMode", is("MIDI"))
                .extract().response();

        UUID exerciseId = UUID.fromString(created.path("id"));
        Exercise exercise = exerciseService.find(exerciseId).orElseThrow();
        ExpectedAnswer expected = exerciseService.expectedAnswerOf(exercise);
        List<Integer> notes = ChordAnalyzer.parse(expected.chordSymbol()).notes(3).stream()
                .map(Note::midi).toList();

        given().contentType(ContentType.JSON)
                .body(Map.of("notes", notes))
                .when().post("/api/exercises/{id}/midi", exerciseId)
                .then().statusCode(200)
                .body("attempt.outcome.result", is("CORRECT"));
    }

    @Test
    @DisplayName("playing root position when an inversion was asked for is partial credit, not failure")
    void reportsTheInversionMistakeSeparately() {
        given().when().post("/api/session").then().statusCode(200);

        Response created = given().contentType(ContentType.JSON)
                .body(Map.of("conceptId", "chord-inversion", "difficulty", 0.5, "answerMode", "MIDI"))
                .when().post("/api/exercises").then().extract().response();

        UUID exerciseId = UUID.fromString(created.path("id"));
        Exercise exercise = exerciseService.find(exerciseId).orElseThrow();
        ExpectedAnswer expected = exerciseService.expectedAnswerOf(exercise);
        List<Integer> rootPosition = ChordAnalyzer.parse(expected.chordSymbol())
                .inverted(fr.lapetina.music.theory.Inversion.ROOT_POSITION)
                .notes(3).stream().map(Note::midi).toList();

        given().contentType(ContentType.JSON)
                .body(Map.of("notes", rootPosition))
                .when().post("/api/exercises/{id}/midi", exerciseId)
                .then().statusCode(200)
                .body("attempt.outcome.result", is("PARTIALLY_CORRECT"))
                .body("attempt.outcome.misconceptionCode", is("plays-root-position-when-inversion-asked"));
    }

    @Test
    void theSessionKeepsTheWholeConversation() {
        Response opening = given().when().post("/api/session").then().extract().response();
        UUID sessionId = UUID.fromString(opening.path("sessionId"));

        given().contentType(ContentType.JSON)
                .body(Map.of("message", "why does the leading tone want to rise?"))
                .when().post("/api/session/{id}/message", sessionId)
                .then().statusCode(200)
                .body("message", notNullValue());

        given().when().get("/api/session/{id}", sessionId)
                .then().statusCode(200)
                .body("interactions.size()", greaterThanOrEqualTo(3))
                .body("interactions[0].role", is("TUTOR"));
    }

    @Test
    @DisplayName("the loop never stalls: every turn can still produce evidence, even after failures")
    void keepsProducingEvidenceAfterWrongAnswers() {
        // Regression. EXPLAIN turns used to carry no exercise, so a learner who got the
        // first question wrong could never generate evidence again: mastery stayed at
        // zero, the policy kept choosing EXPLAIN, and the tutor asked forever.
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        UUID exerciseId = UUID.fromString(opening.path("exerciseId"));

        for (int turn = 0; turn < 6; turn++) {
            Response answered = given().contentType(ContentType.JSON)
                    .body(Map.of("answer", "definitely not the answer"))
                    .when().post("/api/exercises/{id}/answer", exerciseId)
                    .then().statusCode(200)
                    .body("attempt.evidenceRecorded", is(true))
                    .extract().response();

            String next = answered.path("exerciseId");
            assertNotNull(next, "turn " + turn + " (" + answered.path("action")
                    + ") left the learner with nothing to answer");
            exerciseId = UUID.fromString(next);
        }

        List<Object> evidence = given().when().get("/api/learner/evidence")
                .then().statusCode(200).extract().path("$");
        assertTrue(evidence.size() >= 6, "expected an evidence row per turn, got " + evidence.size());
    }

    @Test
    @DisplayName("asking for help is not a wrong answer, and the question stays on the table")
    void askingForHelpIsNotGraded() {
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        String exerciseId = opening.path("exerciseId");

        Response helped = given().contentType(ContentType.JSON)
                .body(Map.of("answer", "explain"))
                .when().post("/api/exercises/{id}/answer", exerciseId)
                .then().statusCode(200)
                .body("attempt", nullValue())
                .extract().response();

        assertEquals(exerciseId, helped.path("exerciseId"),
                "the open question should still be open after asking for help");

        given().when().get("/api/learner/evidence")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @DisplayName("a question about something else is taken up rather than ignored")
    void answersWhatTheLearnerAsked() {
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        UUID sessionId = UUID.fromString(opening.path("sessionId"));

        given().contentType(ContentType.JSON)
                .body(Map.of("message", "what is a C major add 7 chord"))
                .when().post("/api/session/{id}/message", sessionId)
                .then().statusCode(200)
                .body("rationale", containsString("seventh-chord"));
    }

    @Test
    @DisplayName("a question typed while an exercise is open is not marked wrong")
    void doesNotGradeQuestionsAsAnswers() {
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        String exerciseId = opening.path("exerciseId");

        given().contentType(ContentType.JSON)
                .body(Map.of("answer", "what is a C major add 7 chord"))
                .when().post("/api/exercises/{id}/answer", exerciseId)
                .then().statusCode(200)
                .body("attempt", nullValue())
                .body("rationale", containsString("seventh-chord"));

        given().when().get("/api/learner/evidence").then().body("size()", is(0));
    }

    @Test
    @DisplayName("a hesitant but real answer is still graded")
    void stillGradesRealAnswers() {
        Response opening = given().when().post("/api/session").then().statusCode(200).extract().response();
        String exerciseId = opening.path("exerciseId");

        given().contentType(ContentType.JSON)
                .body(Map.of("answer", "is it Gb?"))
                .when().post("/api/exercises/{id}/answer", exerciseId)
                .then().statusCode(200)
                .body("attempt", notNullValue())
                .body("attempt.evidenceRecorded", is(true));
    }

    @Test
    @DisplayName("the server-sent-events endpoint streams a usable turn")
    void streamsATurn() {
        Response opening = given().when().post("/api/session").then().extract().response();
        UUID sessionId = UUID.fromString(opening.path("sessionId"));

        // RestAssured is not an SSE client and trips over the chunked stream's close, so
        // this reads the stream with a plain HTTP client instead.
        String stream = readStream("/api/session/" + sessionId + "/stream?message=tell%20me%20about%20inversions");

        assertTrue(stream.contains("\"type\":\"decision\""), stream);
        assertTrue(stream.contains("\"type\":\"turn\""), stream);
        assertTrue(stream.contains("\"type\":\"done\""), stream);
    }

    private static String readStream(String path) {
        try (java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient()) {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:" + io.restassured.RestAssured.port + path))
                    .header("Accept", "text/event-stream")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();
            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "stream said: " + response.body());
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the event stream", e);
        }
    }

    @Test
    void exposesWhatItWouldDoNextWithoutDoingIt() {
        given().when().get("/api/session/next-action")
                .then().statusCode(200)
                .body("action", notNullValue())
                .body("conceptId", notNullValue())
                .body("rationale", notNullValue());
    }

    @Test
    void reportsWhichNarratorIsAnswering() {
        given().when().get("/api/session/status")
                .then().statusCode(200)
                .body("languageModelAvailable", is(false))
                .body("model", is("qwen3:8b"))
                .body("conceptCount", greaterThanOrEqualTo(20));
    }

    @Test
    @DisplayName("free mode: the learner can choose the topic, and the tutor teaches it")
    void theLearnerCanChooseTheTopic() {
        given().when().post("/api/session").then().statusCode(200);

        given().when().put("/api/learner/focus/concept/cadence")
                .then().statusCode(200).body("focusConceptId", is("cadence"));
        // On a blank profile the groundwork comes first, but the choice is still what is
        // driving the tutor — it says so.
        given().when().get("/api/session/next-action")
                .then().statusCode(200).body("rationale", containsString("cadence"));

        given().when().put("/api/learner/focus/category/CHORDS")
                .then().statusCode(200)
                .body("focusCategory", is("CHORDS"))
                .body("focusConceptId", nullValue());

        given().when().delete("/api/learner/focus")
                .then().statusCode(200)
                .body("focusConceptId", nullValue())
                .body("focusCategory", nullValue());
    }

    @Test
    @DisplayName("everything known about the learner can be taken away in one file")
    void exportsTheLearnerModel() {
        Response opening = given().when().post("/api/session").then().extract().response();
        String exerciseId = opening.path("exerciseId");
        given().contentType(ContentType.JSON).body(Map.of("answer", "wrong"))
                .when().post("/api/exercises/{id}/answer", exerciseId).then().statusCode(200);

        given().when().get("/api/learner/export")
                .then().statusCode(200)
                .body("learner.id", notNullValue())
                .body("exportedAt", notNullValue())
                .body("concepts.size()", greaterThanOrEqualTo(30))
                .body("evidence.size()", greaterThanOrEqualTo(1))
                .body("evidence[0].masteryAfter", notNullValue());
    }

    @Test
    void servesTheConceptGraph() {
        given().when().get("/api/concepts")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(20))
                .body("[0].id", is("note"));

        given().when().get("/api/concepts/secondary-dominant")
                .then().statusCode(200)
                .body("prerequisites", contains("dominant-seventh", "roman-numeral", "key-signature"))
                .body("unlocks", contains("modulation"));

        given().when().get("/api/concepts/nonsense").then().statusCode(404);
    }

    @Test
    void analysesHarmonyOverHttp() {
        given().contentType(ContentType.JSON)
                .body(Map.of("chords", List.of("C", "F", "G7", "C"), "key", "C major"))
                .when().post("/api/theory/progression/analyze")
                .then().statusCode(200)
                .body("romanNumerals", contains("I", "IV", "V7", "I"))
                .body("allDiatonic", is(true))
                .body("cadences[0].cadence", is("perfect authentic cadence"));

        given().contentType(ContentType.JSON)
                .body(Map.of("midiNotes", List.of(59, 62, 67)))
                .when().post("/api/theory/chord/analyze")
                .then().statusCode(200)
                .body("symbol", is("G/B"))
                .body("inversion", is("FIRST"))
                .body("abc", notNullValue());

        given().when().get("/api/theory/key/Eb_major")
                .then().statusCode(200)
                .body("keySignature", is(-3))
                .body("triads[4]", is("Bb"));
    }

    @Test
    void rejectsNonsenseWithABadRequestRatherThanAServerError() {
        given().contentType(ContentType.JSON)
                .body(Map.of("chords", List.of("Hq7"), "key", "C major"))
                .when().post("/api/theory/progression/analyze")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("evidence accumulates across a session and the concept advances")
    void masteryClimbsOverASession() {
        Response opening = given().when().post("/api/session").then().extract().response();
        UUID sessionId = UUID.fromString(opening.path("sessionId"));
        UUID exerciseId = UUID.fromString(opening.path("exerciseId"));

        double lastMastery = 0.0;
        for (int i = 0; i < 5 && exerciseId != null; i++) {
            Exercise exercise = exerciseService.find(exerciseId).orElseThrow();
            ExpectedAnswer expected = exerciseService.expectedAnswerOf(exercise);
            Response answered = given().contentType(ContentType.JSON)
                    .body(Map.of("answer", expected.canonical()))
                    .when().post("/api/exercises/{id}/answer", exerciseId)
                    .then().statusCode(200).extract().response();

            Number after = answered.path("attempt.masteryAfter");
            if (after != null) {
                lastMastery = after.doubleValue();
            }
            String next = answered.path("exerciseId");
            exerciseId = next == null ? null : UUID.fromString(next);
            assertEquals(sessionId.toString(), answered.path("sessionId"));
        }

        assertTrue(lastMastery > 0.0);
        Response snapshot = given().when().get("/api/learner").then().extract().response();
        assertNotNull(snapshot.path("learnerId"));
        List<Object> inProgress = snapshot.path("concepts.findAll { it.state != 'UNKNOWN' }");
        assertTrue(inProgress.size() >= 1);
    }
}

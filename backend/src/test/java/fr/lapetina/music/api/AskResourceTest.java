package fr.lapetina.music.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asking a question directly.
 *
 * <p>These run with no model, no index and no corpora, which is the point: a question box
 * that only works when Ollama is running and sources have been brought in would fail exactly
 * when somebody is trying the application for the first time.
 */
@QuarkusTest
class AskResourceTest {

    @Test
    @DisplayName("a question is answered even with no model and nothing ingested")
    void answersWithoutAModelOrSources() {
        given().contentType(ContentType.JSON)
                .body(Map.of("question", "What is V7/V in C major?"))
                .when().post("/api/ask")
                .then().statusCode(200)
                .body("answer", not(emptyString()))
                .body("question", notNullValue())
                .body("conversationId", notNullValue())
                // The theory engine needs neither an index nor a network, so this one is
                // computed here whatever else is switched off.
                .body("computed[0].answer", notNullValue());
    }

    @Test
    @DisplayName("every passage returned says where it came from")
    void everyPassageIsAttributed() {
        given().contentType(ContentType.JSON)
                .body(Map.of("question", "Explain the Neapolitan sixth"))
                .when().post("/api/ask")
                .then().statusCode(200)
                .body("passages", everyItem(hasKey("citation")))
                .body("passages", everyItem(hasKey("license")))
                .body("examples", notNullValue())
                .body("sources", notNullValue());
    }

    @Test
    @DisplayName("an empty question is refused rather than answered")
    void refusesAnEmptyQuestion() {
        given().contentType(ContentType.JSON).body(Map.of("question", "   "))
                .when().post("/api/ask").then().statusCode(400);
        given().contentType(ContentType.JSON).body(Map.of())
                .when().post("/api/ask").then().statusCode(400);
    }

    @Test
    @DisplayName("a conversation id we did not issue starts a new conversation rather than failing")
    void toleratesAnUnknownConversationId() {
        given().contentType(ContentType.JSON)
                .body(Map.of("question", "What is a tritone?", "conversationId", "not-a-uuid"))
                .when().post("/api/ask")
                .then().statusCode(200)
                .body("conversationId", notNullValue());
    }
}

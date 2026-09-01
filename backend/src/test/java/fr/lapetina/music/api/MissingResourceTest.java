package fr.lapetina.music.api;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A path that names something that is not there answers 404, whatever it names.
 *
 * <p>Knowledge sources used to answer 400, because the registry raises an unknown id as an
 * argument complaint and the mappers turn those into bad requests. It made the same mistake
 * look like two different kinds of failure depending on which resource you asked.
 */
@QuarkusTest
class MissingResourceTest {

    @Test
    @DisplayName("an unknown knowledge source is not found, not a bad request")
    void unknownSourceIsNotFound() {
        given().when().get("/api/knowledge/sources/not-a-source").then().statusCode(404);
        given().when().post("/api/knowledge/sources/not-a-source/ingest").then().statusCode(404);
        given().when().post("/api/knowledge/sources/not-a-source/reindex").then().statusCode(404);
        given().when().post("/api/knowledge/sources/not-a-source/refresh").then().statusCode(404);
    }

    @Test
    @DisplayName("an unknown concept is not found")
    void unknownConceptIsNotFound() {
        given().when().get("/api/concepts/not-a-concept/lesson").then().statusCode(404);
        given().when().get("/api/concepts/not-a-concept").then().statusCode(404);
    }

    @Test
    @DisplayName("a declared source is still served")
    void declaredSourceStillAnswers() {
        given().when().get("/api/knowledge/sources/open-music-theory").then().statusCode(200);
    }
}

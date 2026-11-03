package io.github.foundry4j.quarkforge.web.version;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ApiVersionTest {

    @Test
    void acceptsSupportedVersion() {
        given().header(ApiVersionFilter.VERSION_HEADER, "v1")
                .when().get("/versioned")
                .then().statusCode(200)
                .header(ApiVersionFilter.VERSION_HEADER, "v1")
                .body(is("ok"));
    }

    @Test
    void rejectsUnsupportedVersion() {
        given().header(ApiVersionFilter.VERSION_HEADER, "v2")
                .when().get("/versioned")
                .then().statusCode(400)
                .body("code", is(40001));
    }

    @Test
    void normalizesPlainNumberVersion() {
        given().header(ApiVersionFilter.VERSION_HEADER, "1")
                .when().get("/versioned")
                .then().statusCode(200)
                .header(ApiVersionFilter.VERSION_HEADER, "v1");
    }

    @Test
    void exposesDeprecationHeaders() {
        given().when().get("/versioned/legacy")
                .then().statusCode(200)
                .header(ApiVersionFilter.DEPRECATION_HEADER, "true")
                .header(ApiVersionFilter.SUNSET_HEADER, "2027-01-01")
                .header("Link", "</v2/versioned>; rel=\"successor-version\"");
    }

    @Test
    void resolvesVersionFromPathSegment() {
        given().when().get("/v1/plain")
                .then().statusCode(200)
                .header(ApiVersionFilter.VERSION_HEADER, "v1")
                .body(is("pong"));
    }

    @ApiVersion("v1")
    @Path("/versioned")
    @ApplicationScoped
    public static class VersionedResource {

        @GET
        public String hello() {
            return "ok";
        }

        @GET
        @Path("/legacy")
        @ApiVersion(value = "v1", deprecated = true, sunset = "2027-01-01", successor = "/v2/versioned")
        public String legacy() {
            return "legacy";
        }
    }

    @Path("/v1/plain")
    @ApplicationScoped
    public static class PathVersionedResource {

        @GET
        public String ping() {
            return "pong";
        }
    }
}

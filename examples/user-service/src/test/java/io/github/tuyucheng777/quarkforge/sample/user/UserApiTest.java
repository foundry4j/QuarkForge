package io.github.tuyucheng777.quarkforge.sample.user;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/// 示例服务端到端测试：覆盖统一响应包装、异常映射、参数校验、分页、JWT 认证与鉴权。
@QuarkusTest
class UserApiTest {

    private static final String PASSWORD = "password-123456";

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static long createUser(String username) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "displayName", "测试用户", "password", PASSWORD))
                .when().post("/users")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.id");
    }

    @Test
    void createReturnsUnifiedEnvelopeWithoutSensitiveFields() {
        String username = uniqueUsername("alice");

        given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "displayName", "Alice", "email", "alice@example.com",
                        "password", PASSWORD))
                .when().post("/users")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("message", equalTo("success"))
                .body("data.id", notNullValue())
                .body("data.username", equalTo(username))
                .body("data.status", equalTo("ACTIVE"))
                .body("data.role", equalTo("user"))
                .body("data.passwordHash", equalTo(null))
                .header("X-Trace-Id", notNullValue());
    }

    @Test
    void fetchExistingUser() {
        long id = createUser(uniqueUsername("bob"));

        given().get("/users/" + id)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.id", equalTo(id));
    }

    @Test
    void missingUserMapsToNotFoundEnvelope() {
        given().get("/users/999999999")
                .then().statusCode(404)
                .body("code", equalTo(40400))
                .body("message", equalTo("请求的资源不存在"));
    }

    @Test
    void invalidBodyMapsToParamError() {
        given().contentType(ContentType.JSON)
                .body(Map.of("username", "  ", "displayName", "x", "password", "short"))
                .when().post("/users")
                .then().statusCode(400)
                .body("code", equalTo(40000))
                .body("message", containsString("username"));
    }

    @Test
    void duplicateUsernameIsRejected() {
        String username = uniqueUsername("carol");
        createUser(username);

        given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "displayName", "重复", "password", PASSWORD))
                .when().post("/users")
                .then().statusCode(400)
                .body("code", equalTo(40000))
                .body("message", containsString("用户名已存在"));
    }

    @Test
    void listSupportsKeywordAndPagination() {
        String prefix = uniqueUsername("page");
        createUser(prefix + "-a");
        createUser(prefix + "-b");
        createUser(prefix + "-c");

        given().queryParam("keyword", prefix).queryParam("page", 1).queryParam("size", 2)
                .get("/users")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.page", equalTo(1))
                .body("data.size", equalTo(2))
                .body("data.records.size()", equalTo(2))
                .body("data.total", greaterThanOrEqualTo(3));
    }

    @Test
    void updateChangesDisplayName() {
        long id = createUser(uniqueUsername("dave"));

        given().contentType(ContentType.JSON)
                .body(Map.of("displayName", "新名字", "status", "DISABLED"))
                .when().put("/users/" + id)
                .then().statusCode(200)
                .body("data.displayName", equalTo("新名字"))
                .body("data.status", equalTo("DISABLED"));
    }

    @Test
    void deleteThenFetchReturnsNotFound() {
        long id = createUser(uniqueUsername("erin"));

        given().delete("/users/" + id).then().statusCode(204);

        given().get("/users/" + id).then().statusCode(404).body("code", equalTo(40400));
    }

    @Test
    void loginThenAccessProtectedProfile() {
        String username = uniqueUsername("frank");
        createUser(username);

        String token = given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", PASSWORD))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.tokenType", equalTo("Bearer"))
                .body("data.expiresInSeconds", equalTo(7200))
                .body("data.accessToken", notNullValue())
                .extract().jsonPath().getString("data.accessToken");

        given().header("Authorization", "Bearer " + token)
                .get("/users/me")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.username", equalTo(username));
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        String username = uniqueUsername("grace");
        createUser(username);

        given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", "wrong-password"))
                .when().post("/auth/login")
                .then().statusCode(401)
                .body("code", equalTo(40100))
                .body("message", equalTo("用户名或密码错误"));
    }

    @Test
    void disabledUserCannotLogin() {
        String username = uniqueUsername("heidi");
        long id = createUser(username);

        given().contentType(ContentType.JSON)
                .body(Map.of("status", "DISABLED"))
                .when().put("/users/" + id)
                .then().statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", PASSWORD))
                .when().post("/auth/login")
                .then().statusCode(403)
                .body("code", equalTo(40300));
    }

    @Test
    void protectedProfileRequiresToken() {
        given().get("/users/me").then().statusCode(401);
    }
}

package io.github.foundry4j.quarkforge.sample.order;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/// order-service 端到端测试：验证下单前通过 gRPC 调用 user-service 校验用户。
@QuarkusTest
class OrderApiTest {

    @Test
    void createOrderForValidUser() {
        given().contentType(ContentType.JSON)
                .body(Map.of("userId", 1, "productName", "键盘", "quantity", 2, "amount", 199.00))
                .when().post("/orders")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.id", notNullValue())
                .body("data.userId", equalTo(1))
                .body("data.productName", equalTo("键盘"))
                .body("data.status", equalTo("CREATED"));
    }

    @Test
    void disabledUserCannotCreateOrder() {
        given().contentType(ContentType.JSON)
                .body(Map.of("userId", 2, "productName", "鼠标", "quantity", 1, "amount", 99.00))
                .when().post("/orders")
                .then().statusCode(403)
                .body("code", equalTo(40300))
                .body("message", equalTo("用户已被禁用，无法下单"));
    }

    @Test
    void nonExistentUserRejectedViaGrpcErrorMapping() {
        // user-service 返回 404，经 gRPC trailer 还原为业务错误码 40400
        given().contentType(ContentType.JSON)
                .body(Map.of("userId", 999, "productName", "显示器", "quantity", 1, "amount", 1299.00))
                .when().post("/orders")
                .then().statusCode(404)
                .body("code", equalTo(40400));
    }

    @Test
    void fetchCreatedOrder() {
        long id = given().contentType(ContentType.JSON)
                .body(Map.of("userId", 1, "productName", "笔记本", "quantity", 1, "amount", 5999.00))
                .when().post("/orders")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.id");

        given().get("/orders/" + id)
                .then().statusCode(200)
                .body("data.id", equalTo(id))
                .body("data.amount", equalTo(new BigDecimal("5999.00").floatValue()));
    }

    @Test
    void listOrdersByUser() {
        given().contentType(ContentType.JSON)
                .body(Map.of("userId", 1, "productName", "A", "quantity", 1, "amount", 10.00))
                .post("/orders");
        given().contentType(ContentType.JSON)
                .body(Map.of("userId", 1, "productName", "B", "quantity", 1, "amount", 20.00))
                .post("/orders");

        given().queryParam("userId", 1).queryParam("page", 1).queryParam("size", 10)
                .get("/orders")
                .then().statusCode(200)
                .body("data.records.size()", org.hamcrest.Matchers.greaterThanOrEqualTo(2));
    }
}

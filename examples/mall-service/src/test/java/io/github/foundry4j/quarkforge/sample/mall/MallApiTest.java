package io.github.foundry4j.quarkforge.sample.mall;

import io.github.foundry4j.quarkforge.sample.mall.user.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// ## MallApiTest
/// 电商示例端到端测试：覆盖登录、RBAC、商品缓存、下单幂等、库存扣减/回滚与操作日志。
///
/// 使用应用启动时预置的管理员账号，测试数据通过 UUID 保证互不冲突。
@QuarkusTest
class MallApiTest {

    private static final String BUYER_PASSWORD = "Buyer@123456";

    @Test
    void adminCanCreateProductAndAnyoneCanRead() {
        String admin = loginAdmin();
        String name = uniqueName("keyboard");
        long productId = createProduct(admin, name, 399.00, 10);

        Response detail = given().get("/products/" + productId)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.name", equalTo(name))
                .body("data.stock", equalTo(10))
                .extract().response();
        assertEquals(399.00, detail.jsonPath().getFloat("data.price"), 0.001);

        List<Map<String, Object>> records = given()
                .queryParam("keyword", name)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .get("/products")
                .then().statusCode(200)
                .body("data.records[0].name", equalTo(name))
                .extract().jsonPath().getList("data.records");
        assertEquals(1, records.size());
    }

    @Test
    void buyerCannotCreateProduct() {
        String buyer = registerBuyer();
        String token = login(buyer, BUYER_PASSWORD);

        authed(token)
                .body(Map.of("name", uniqueName("forbidden"), "price", 1.00, "stock", 1))
                .post("/products")
                .then().statusCode(403)
                .body("code", equalTo(40300));
    }

    @Test
    void unauthenticatedOrderCreationIsRejected() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body(orderBody(unique("req"), 1L, 1))
                .post("/orders")
                .then().statusCode(401);
    }

    @Test
    void orderCreationIsIdempotentAndDeductsStock() {
        String admin = loginAdmin();
        long productId = createProduct(admin, uniqueName("idempotent"), 99.00, 10);
        assertStock(productId, 10);

        String buyer = registerBuyer();
        String token = login(buyer, BUYER_PASSWORD);
        String requestNo = unique("req");
        Map<String, Object> body = orderBody(requestNo, productId, 2);

        Response first = authed(token).body(body).post("/orders")
                .then().statusCode(200)
                .body("data.status", equalTo("CREATED"))
                .body("data.items.size()", equalTo(1))
                .extract().response();
        long firstId = first.jsonPath().getLong("data.id");
        assertEquals(198.00, first.jsonPath().getFloat("data.totalAmount"), 0.001);

        Response replay = authed(token).body(body).post("/orders")
                .then().statusCode(200)
                .extract().response();
        assertEquals(firstId, replay.jsonPath().getLong("data.id"));

        assertStock(productId, 8);
        assertEquals(1L, authed(token).get("/orders/mine")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.total"));
    }

    @Test
    void insufficientStockIsRejectedAndLogged() {
        String admin = loginAdmin();
        long productId = createProduct(admin, uniqueName("short-stock"), 10.00, 1);
        String buyer = registerBuyer();
        String token = login(buyer, BUYER_PASSWORD);

        authed(token)
                .body(orderBody(unique("req"), productId, 5))
                .post("/orders")
                .then().statusCode(400)
                .body("code", equalTo(40000))
                .body("message", containsString("库存不足"));

        authed(admin)
                .queryParam("module", "order")
                .queryParam("size", 50)
                .get("/admin/operation-logs")
                .then().statusCode(200)
                .body("data.records.success", hasItem(false));
    }

    @Test
    void cancelOrderRestoresStock() {
        String admin = loginAdmin();
        long productId = createProduct(admin, uniqueName("cancel"), 50.00, 5);
        assertStock(productId, 5);

        String buyer = registerBuyer();
        String token = login(buyer, BUYER_PASSWORD);
        long orderId = createOrder(token, productId, 3);
        assertStock(productId, 2);

        authed(token).post("/orders/" + orderId + "/cancel")
                .then().statusCode(200)
                .body("data.status", equalTo("CANCELLED"));
        assertStock(productId, 5);

        authed(token).post("/orders/" + orderId + "/cancel")
                .then().statusCode(400)
                .body("message", containsString("不可取消"));
    }

    @Test
    void buyerCannotReadAnotherBuyersOrder() {
        String admin = loginAdmin();
        long productId = createProduct(admin, uniqueName("ownership"), 20.00, 10);

        String owner = registerBuyer();
        String ownerToken = login(owner, BUYER_PASSWORD);
        long orderId = createOrder(ownerToken, productId, 1);

        String stranger = registerBuyer();
        String strangerToken = login(stranger, BUYER_PASSWORD);

        authed(strangerToken).get("/orders/" + orderId)
                .then().statusCode(403)
                .body("code", equalTo(40300));
        Response ownerView = authed(ownerToken).get("/orders/" + orderId)
                .then().statusCode(200)
                .extract().response();
        assertEquals(orderId, ownerView.jsonPath().getLong("data.id"));
    }

    @Test
    void weakPasswordIsRejected() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", unique("weak"),
                        "displayName", "弱密码用户",
                        "password", "123456"))
                .post("/auth/register")
                .then().statusCode(400)
                .body("code", equalTo(40000))
                .body("message", containsString("长度至少 8 位"));
    }

    @Test
    void operationLogsRequirePermission() {
        String admin = loginAdmin();
        createProduct(admin, uniqueName("log-permission"), 9.90, 1);

        String buyer = registerBuyer();
        String buyerToken = login(buyer, BUYER_PASSWORD);
        authed(buyerToken).get("/admin/operation-logs")
                .then().statusCode(403)
                .body("code", equalTo(40300));

        long total = authed(admin).get("/admin/operation-logs")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.total");
        assertTrue(total >= 1);
    }

    @Test
    void productUpdateEvictsCache() {
        String admin = loginAdmin();
        long productId = createProduct(admin, uniqueName("cache"), 10.00, 1);

        assertEquals(10.00, given().get("/products/" + productId)
                .then().statusCode(200)
                .extract().jsonPath().getFloat("data.price"), 0.001);

        authed(admin).body(Map.of("price", 20.00))
                .put("/products/" + productId)
                .then().statusCode(200)
                .body("data.price", equalTo(20.00f));

        assertEquals(20.00, given().get("/products/" + productId)
                .then().statusCode(200)
                .extract().jsonPath().getFloat("data.price"), 0.001);
    }

    // ---------- helpers ----------

    private static RequestSpecification authed(String token) {
        return given().header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static String loginAdmin() {
        return login(UserService.ADMIN_USERNAME, UserService.ADMIN_PASSWORD);
    }

    private static String login(String username, String password) {
        return given().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .post("/auth/login")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getString("data.accessToken");
    }

    private static String registerBuyer() {
        String username = unique("buyer");
        given().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", username,
                        "displayName", "测试买家",
                        "password", BUYER_PASSWORD))
                .post("/auth/register")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.role", equalTo("BUYER"));
        return username;
    }

    private static long createProduct(String token, String name, double price, int stock) {
        return authed(token)
                .body(Map.of(
                        "name", name,
                        "description", "测试商品",
                        "price", price,
                        "stock", stock))
                .post("/products")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getLong("data.id");
    }

    private static long createOrder(String token, long productId, int quantity) {
        return authed(token)
                .body(orderBody(unique("req"), productId, quantity))
                .post("/orders")
                .then().statusCode(200)
                .body("data.status", equalTo("CREATED"))
                .extract().jsonPath().getLong("data.id");
    }

    private static Map<String, Object> orderBody(String requestNo, long productId, int quantity) {
        return Map.of(
                "requestNo", requestNo,
                "items", List.of(Map.of("productId", productId, "quantity", quantity)));
    }

    private static void assertStock(long productId, int expected) {
        given().get("/products/" + productId)
                .then().statusCode(200)
                .body("data.stock", equalTo(expected));
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}

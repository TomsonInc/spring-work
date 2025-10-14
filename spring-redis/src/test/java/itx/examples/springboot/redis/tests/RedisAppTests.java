package itx.examples.springboot.redis.tests;

import itx.examples.springboot.redis.model.MessageData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@ContextConfiguration(initializers = RedisAppTests.Initializer.class)
class RedisAppTests {

    private static final Logger LOG = LoggerFactory.getLogger(RedisAppTests.class);

    private static final int REDIS_PORT = 6379;
    private static final String REDIS_IMAGE = "redis:6-alpine";
    private static final String BASE_URL = "/services/data/messages";

    protected static GenericContainer<?> redisContainer;
    private static String messageId;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Order(1)
    void testEmptyRepo() {
        LOG.info("Testing empty message repository...");
        ResponseEntity<MessageData[]> response = restTemplate.getForEntity(BASE_URL, MessageData[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    @Test
    @Order(2)
    void createMessage() {
        String testMessage = "hi-message";

        LOG.info("Creating message: {}", testMessage);
        ResponseEntity<MessageData> response = restTemplate.postForEntity(BASE_URL + "/" + testMessage, null, MessageData.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(testMessage, response.getBody().getMessage());

        messageId = response.getBody().getId();

        // Confirm creation
        ResponseEntity<MessageData[]> confirmResponse = restTemplate.getForEntity(BASE_URL, MessageData[].class);
        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());
        assertNotNull(confirmResponse.getBody());
        assertEquals(1, confirmResponse.getBody().length);
    }

    @Test
    @Order(3)
    void getMessageById() {
        LOG.info("Fetching message with ID: {}", messageId);
        ResponseEntity<MessageData> response = restTemplate.getForEntity(BASE_URL + "/" + messageId, MessageData.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(messageId, response.getBody().getId());
        assertEquals("hi-message", response.getBody().getMessage());
    }

    @Test
    @Order(4)
    void deleteMessage() {
        LOG.info("Deleting message with ID: {}", messageId);
        restTemplate.delete(BASE_URL + "/" + messageId);

        // Confirm deletion
        ResponseEntity<MessageData[]> response = restTemplate.getForEntity(BASE_URL, MessageData[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    /**
     * Testcontainers initializer to start Redis container and inject its properties into the Spring context.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final Logger LOGGER = LoggerFactory.getLogger(Initializer.class);

        static GenericContainer<?> container = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NonNull ConfigurableApplicationContext context) {
            container.start();
            Assertions.assertTrue(container.isRunning(), "Redis container failed to start!");

            redisContainer = container;

            String redisHost = container.getHost();
            Integer redisMappedPort = container.getMappedPort(REDIS_PORT);

            LOGGER.info("REDIS running at {}:{}", redisHost, redisMappedPort);

            TestPropertyValues.of(
                    "spring.redis.host=" + redisHost,
                    "spring.redis.port=" + redisMappedPort
            ).applyTo(context.getEnvironment());
        }
    }
}

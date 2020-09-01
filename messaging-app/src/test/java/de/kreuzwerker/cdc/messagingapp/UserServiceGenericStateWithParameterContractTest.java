package de.kreuzwerker.cdc.messagingapp;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.pactfoundation.consumer.dsl.LambdaDsl;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "user-service.base-url:http://localhost:9000",
    classes = UserServiceClient.class)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "user-service", port = "9000")
public class UserServiceGenericStateWithParameterContractTest {

    private static final String NAME = "user name for CDC";
    private static final LocalDateTime LAST_LOGIN = LocalDateTime.of(2018, 10, 16, 12, 34, 12);

    @Autowired
    private UserServiceClient userServiceClient;


    @Pact(consumer = "messaging-app")
    public RequestResponsePact pactUserExists(PactDslWithProvider builder) {

        // See https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit#dsl-matching-methods
        DslPart body = LambdaDsl.newJsonBody((o) -> o
            .stringType("id", "1")
            .stringType("name", NAME)
            .timestamp("lastLogin", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Date.from(LAST_LOGIN.atZone(ZoneId.systemDefault()).toInstant()))
            .stringMatcher("role", "ADMIN|USER", "ADMIN")
            .minArrayLike("friends", 0, 2, friend -> friend
                .stringType("id", "2")
                .stringType("name", "a friend")
            )).build();

        return builder.given("default", Collections.singletonMap("userExists", true))
            .uponReceiving("A request for an existing user")
            .path("/users/1")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(body)
            .toPact();

    }

    @Pact(consumer = "messaging-app")
    public RequestResponsePact pactUserDoesNotExist(PactDslWithProvider builder) {

        return builder.given("default", Collections.singletonMap("userExists", false))
            .uponReceiving("A request for a non-existing user")
            .path("/users/1")
            .method("GET")
            .willRespondWith()
            .status(404)
            .toPact();
    }

    @PactTestFor(pactMethod = "pactUserExists")
    @Test
    public void userExists() {
        final User user = userServiceClient.getUser("1");

        assertThat(user.getName()).isEqualTo(NAME);
        assertThat(user.getLastLogin()).isEqualTo(LAST_LOGIN);
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.getFriends()).hasSize(2)
            // currently not possible to define multiple values, s. https://github.com/DiUS/pact-jvm/issues/379
            .extracting(Friend::getId, Friend::getName)
            .containsExactly(Tuple.tuple("2", "a friend"), Tuple.tuple("2", "a friend"));
    }

    @PactTestFor(pactMethod = "pactUserDoesNotExist")
    @Test
    public void userDoesNotExist() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () ->
                userServiceClient.getUser("1"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
package co.com.bancolombia.mcpclient.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChatGatewayAdapterTest {

    private static Stream<Arguments> provideRegexTestCases() {
        return Stream.of(
                Arguments.of("<think>reasoning</think>result", "result"),
                Arguments.of("<think>\nmulti-line\nreasoning\n</think>\nActual response",
                        "Actual response"),
                Arguments.of("no reasoning here", "no reasoning here")
        );
    }

    @ParameterizedTest
    @MethodSource("provideRegexTestCases")
    void testRegexFiltering(String input, String expected) {
        String filtered = input.replaceAll("(?s)<think>.*?</think>", "").trim();
        assertEquals(expected, filtered);
    }
}

package bookkeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Load tests: the application and MySQL must be running. */
class ConcurrentTransferLoadIT {

    private static final String BASE = "http://localhost:8080/api";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private long createAccount(String name, int balance) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"balance\":" + balance + "}";
        return om.readTree(post("/accounts", body).body()).get("accountId").asLong();
    }

    private BigDecimal balance(long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/accounts/" + id)).GET().build();
        return om.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body())
                .get("balance").decimalValue();
    }

    /** Sends {@code requests} transfers of 1 unit from 50 threads; route.apply(i) returns {from, to}. */
    private Map<Integer, AtomicInteger> runParallel(int requests, IntFunction<long[]> route) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        Map<Integer, AtomicInteger> codes = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            long[] r = route.apply(i);
            futures.add(pool.submit(() -> {
                String body = "{\"fromAccId\":" + r[0] + ",\"toAccId\":" + r[1]
                        + ",\"amount\":1,\"idempotencyKey\":\"" + UUID.randomUUID() + "\"}";
                int code = post("/transfers", body).statusCode();
                codes.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet();
                return null;
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        return codes;
    }

    private int count(Map<Integer, AtomicInteger> codes, int status) {
        return codes.getOrDefault(status, new AtomicInteger()).get();
    }

    @Test
    void manyParallelTransfersOnOneAccount() throws Exception {
        long from = createAccount("hot-" + UUID.randomUUID(), 10_000);
        long to = createAccount("sink-" + UUID.randomUUID(), 0);

        long t0 = System.nanoTime();
        Map<Integer, AtomicInteger> codes = runParallel(500, i -> new long[]{from, to});
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("hot account: codes=" + codes + " time=" + ms + " ms");

        assertEquals(500, count(codes, 200));
        assertEquals(0, balance(from).compareTo(new BigDecimal("9500")));
        assertEquals(0, balance(to).compareTo(new BigDecimal("500")));
    }

    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        long a = createAccount("a-" + UUID.randomUUID(), 10_000);
        long b = createAccount("b-" + UUID.randomUUID(), 10_000);

        long t0 = System.nanoTime();
        Map<Integer, AtomicInteger> codes =
                runParallel(500, i -> i % 2 == 0 ? new long[]{a, b} : new long[]{b, a});
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("opposite: codes=" + codes + " time=" + ms + " ms");

        assertEquals(500, count(codes, 200));
        assertEquals(0, balance(a).compareTo(new BigDecimal("10000")));
        assertEquals(0, balance(b).compareTo(new BigDecimal("10000")));
    }
}
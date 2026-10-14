package bookkeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

/** Load tests: the application and MySQL must be running. */

class ConcurrentTransferLoadIT {

    private static final String BASE = "http://localhost:8080/api";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    Map<Integer, AtomicInteger> codes = new ConcurrentHashMap<>();
    long start = System.nanoTime();

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

    @Test
    void manyParallelTransfersOnOneAccount() throws Exception {
        long from = createAccount("hot-" + UUID.randomUUID(), 10_000);
        long to = createAccount("sink-" + UUID.randomUUID(), 0);

        int requests = 500;
        ExecutorService pool = Executors.newFixedThreadPool(50);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                String body = "{\"fromAccId\":" + from + ",\"toAccId\":" + to
                        + ",\"amount\":1,\"idempotencyKey\":\"" + UUID.randomUUID() + "\"}";
                HttpResponse<String> r = post("/transfers", body);
                codes.computeIfAbsent(r.statusCode(), k -> new AtomicInteger()).incrementAndGet();
                if (r.statusCode() == 200 && r.body().contains("\"OK\"")) ok.incrementAndGet();
                else failed.incrementAndGet();
                return null;
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("codes=" + codes + " time=" + ms + " ms");
        System.out.println("OK=" + ok + " FAILED=" + failed);
    }
}
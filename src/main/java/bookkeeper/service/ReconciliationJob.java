package bookkeeper.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final JdbcTemplate jdbc;
    private final AtomicInteger mismatches = new AtomicInteger();

    public ReconciliationJob(JdbcTemplate jdbc, MeterRegistry registry) {
        this.jdbc = jdbc;
        registry.gauge("ledger.reconciliation.mismatches", mismatches);
    }

    @Scheduled(fixedDelay = 30_000)
    public void run() {
        List<Long> bad = jdbc.queryForList("""
                SELECT a.accountId
                FROM account a
                LEFT JOIN ledger_entry e ON e.accountId = a.accountId
                GROUP BY a.accountId, a.balance, a.openingBalance
                HAVING a.balance <> a.openingBalance + COALESCE(SUM(e.amount), 0)
                """, Long.class);
        mismatches.set(bad.size());
        if (!bad.isEmpty()) {
            log.error("Reconciliation mismatch on accounts {}", bad);
        }
    }
}
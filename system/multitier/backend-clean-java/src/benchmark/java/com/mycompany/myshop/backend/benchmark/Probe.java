package com.mycompany.myshop.backend.benchmark;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.function.Supplier;

final class Probe {

    private static final int GC_HINTS = 3;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final DataSource dataSource;
    private final Statistics statistics;

    Probe(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    record Timed<T>(T value, long millis, long statements, long retainedHeapMb) { }

    <T> Timed<T> measure(Supplier<T> operation) {
        var heapBefore = settledHeapBytes();
        statistics.clear();

        var start = System.nanoTime();
        var value = operation.get();
        var millis = (System.nanoTime() - start) / 1_000_000L;

        var statements = statistics.getPrepareStatementCount();
        var retained = (settledHeapBytes() - heapBefore) / BYTES_PER_MB;
        // A GC between the two readings can make the delta negative; that is noise, not a negative
        // footprint, so it is floored rather than printed as a number nobody can defend.
        return new Timed<>(value, millis, statements, Math.max(retained, 0));
    }

    String explainAnalyze(String sql) {
        return explain("EXPLAIN (ANALYZE, BUFFERS) " + sql);
    }

    String explainOnly(String sql) {
        return explain("EXPLAIN " + sql);
    }

    private String explain(String statement) {
        try (var connection = dataSource.getConnection();
             var jdbcStatement = connection.createStatement();
             var rows = jdbcStatement.executeQuery(statement)) {
            var plan = new StringBuilder();
            while (rows.next()) {
                plan.append(rows.getString(1)).append('\n');
            }
            return plan.toString().stripTrailing();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not explain: " + statement, e);
        }
    }

    void runScript(Path script) {
        String sql;
        try {
            sql = Files.readString(script, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + script.toAbsolutePath(), e);
        }
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not run " + script.toAbsolutePath(), e);
        }
    }

    long countRows(String table) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count " + table, e);
        }
    }

    private static long settledHeapBytes() {
        var runtime = Runtime.getRuntime();
        for (var i = 0; i < GC_HINTS; i++) {
            System.gc();
        }
        return runtime.totalMemory() - runtime.freeMemory();
    }
}

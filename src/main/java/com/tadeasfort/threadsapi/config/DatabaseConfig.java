package com.tadeasfort.threadsapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConfig implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Test SQLite connection
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("✅ SQLite connection successful! Result: " + result);

            // Create a simple test table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, message TEXT)");
            jdbcTemplate.update("INSERT OR REPLACE INTO test_table (id, message) VALUES (?, ?)", 1,
                    "SQLite is working!");

            String message = jdbcTemplate.queryForObject("SELECT message FROM test_table WHERE id = ?", String.class,
                    1);
            System.out.println("✅ SQLite test message: " + message);

        } catch (Exception e) {
            System.err.println("❌ SQLite connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
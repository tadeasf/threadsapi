package com.tadeasfort.threadsapi.config;

import com.tadeasfort.threadsapi.entity.Post;
import com.tadeasfort.threadsapi.entity.User;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@RegisterReflectionForBinding({ User.class, Post.class })
@ImportRuntimeHints(NativeConfiguration.SQLiteRuntimeHints.class)
public class NativeConfiguration {

    static class SQLiteRuntimeHints implements org.springframework.aot.hint.RuntimeHintsRegistrar {

        @Override
        public void registerHints(org.springframework.aot.hint.RuntimeHints hints, ClassLoader classLoader) {
            // SQLite JDBC driver hints
            hints.reflection()
                    .registerType(org.sqlite.JDBC.class)
                    .registerType(org.sqlite.SQLiteConnection.class);

            // Resource hints for database files
            hints.resources()
                    .registerPattern("*.properties")
                    .registerPattern("*.sql")
                    .registerPattern("*.db");
        }
    }
}
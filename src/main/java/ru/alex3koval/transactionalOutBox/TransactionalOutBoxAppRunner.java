package ru.alex3koval.transactionalOutBox;

import io.debezium.engine.DebeziumEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class TransactionalOutBoxAppRunner implements ApplicationRunner {
    private final DebeziumEngine<?> debeziumEngine;

    @Override
    public void run(ApplicationArguments args) {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(debeziumEngine);
        }
    }
}

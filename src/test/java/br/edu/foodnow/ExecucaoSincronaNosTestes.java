package br.edu.foodnow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
class ExecucaoSincronaNosTestes {
    @Bean("taskExecutor")
    Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}

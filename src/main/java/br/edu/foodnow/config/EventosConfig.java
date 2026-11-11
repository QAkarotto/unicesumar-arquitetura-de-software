package br.edu.foodnow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class EventosConfig {
    private static final Logger log = LoggerFactory.getLogger(EventosConfig.class);

    @Bean
    public ThreadPoolTaskExecutor eventosExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("foodnow-eventos-");
        // A fila é local e limitada: uma rejeição não pode falhar uma confirmação já persistida.
        executor.setRejectedExecutionHandler((tarefa, pool) ->
                log.error("Processamento de evento descartado: executor indisponível ou fila cheia"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }
}

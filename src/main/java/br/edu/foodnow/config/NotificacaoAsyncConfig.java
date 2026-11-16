package br.edu.foodnow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class NotificacaoAsyncConfig {
    private static final Logger logger = LoggerFactory.getLogger(NotificacaoAsyncConfig.class);

    @Bean
    public ThreadPoolTaskExecutor notificacoesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notificacao-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setRejectedExecutionHandler((tarefa, pool) ->
                logger.error("Notificação descartada: executor indisponível ou fila cheia."));
        return executor;
    }
}

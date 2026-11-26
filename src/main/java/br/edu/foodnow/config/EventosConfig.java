package br.edu.foodnow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Processamento assíncrono dos consumidores de eventos locais.
 * Com {@code foodnow.eventos.assincrono=false} os consumidores rodam na thread que publicou o evento
 * (ainda após o commit e com erros isolados), o que torna determinísticos os testes que verificam
 * os e-mails logo após a requisição.
 */
@Configuration
@EnableAsync
public class EventosConfig implements AsyncConfigurer {
    public static final String EXECUTOR_EVENTOS = "eventosExecutor";
    private static final Logger LOG = LoggerFactory.getLogger(EventosConfig.class);

    @Bean(name = EXECUTOR_EVENTOS)
    public TaskExecutor eventosExecutor(@Value("${foodnow.eventos.assincrono:true}") boolean assincrono) {
        if (!assincrono) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("eventos-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (falha, metodo, parametros) -> LOG.error("Falha não tratada no consumidor assíncrono {}",
                metodo.getName(), falha);
    }
}

package demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SimulationServiceConfig {

    //https://docs.spring.io/spring/docs/current/spring-framework-reference/html/scheduling.html#scheduling-task-scheduler-implementations
    @Bean
    public AsyncTaskExecutor taskExecutor(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }
}

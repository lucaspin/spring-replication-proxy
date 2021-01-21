package io.github.lucaspin.replicatingproxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.dsl.Udp;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ReceiverConfig {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ReceiverConfig.class);

    public static final String UDP_RECEIVER_EXECUTOR = "udpReceiverExecutor";

//    @Bean
//    public IntegrationFlow udpIn() {
//        return IntegrationFlows.from(Udp.inboundAdapter(11111).taskExecutor(executor()))
//                .handle(message -> LOG.info("Received message: {}", message))
//                .get();
//    }

//    @Bean
//    public UnicastReceivingChannelAdapter adapter() {
//        UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(11111);
//        adapter.setOutputChannelName("udpChannel");
//        adapter.setTaskExecutor(executor());
//        return adapter;
//    }

    @Bean(UDP_RECEIVER_EXECUTOR)
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        return exec;
    }
}

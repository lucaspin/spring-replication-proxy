package io.github.lucaspin.replicatingproxy.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.*;

import static io.github.lucaspin.replicatingproxy.config.ReceiverConfig.UDP_RECEIVER_EXECUTOR;

@RestController
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Queue<Integer> availablePorts;
    private final TaskExecutor executor;
    private final Map<Integer, IntegrationFlowRegistration> portsInUse = new HashMap<>();
    private final IntegrationFlowContext flowContext;

    public MainController(@Qualifier(UDP_RECEIVER_EXECUTOR) TaskExecutor executor,
                          IntegrationFlowContext flowContext) {
        this.executor = executor;
        this.flowContext = flowContext;
        this.availablePorts = new ArrayDeque<>();
        this.availablePorts.add(11111);
        this.availablePorts.add(11112);
        this.availablePorts.add(11113);
        this.availablePorts.add(11114);
        this.availablePorts.add(11115);
    }

    @GetMapping("/ports")
    public Queue<Integer> getAvailablePorts() {
        return availablePorts;
    }

    @GetMapping("/allocations")
    public Set<Integer> getAllocations() {
        return portsInUse.keySet();
    }

    @PostMapping("/ports")
    public AllocateResponse allocatePortForReplication() {
        Integer port = availablePorts.poll();
        if (port == null) {
            throw new RuntimeException("No more ports available");
        }

        registerNewInboundAdapter(port);
        return new AllocateResponse(port);
    }

    @DeleteMapping("/ports/{port}")
    public ResponseEntity<Void> allocatePortForReplication(@PathVariable("port") Integer port) {
        LOG.info("Deleting port {}", port);
        IntegrationFlowRegistration registration = portsInUse.get(port);
        if (registration == null) {
            return ResponseEntity.notFound().build();
        }

        registration.stop();
        registration.destroy();
        portsInUse.remove(port);
        availablePorts.add(port);
        return ResponseEntity.ok().build();
    }

    private void registerNewInboundAdapter(Integer port) {
        StandardIntegrationFlow flow = IntegrationFlows.from(new UnicastReceivingChannelAdapter(port))
                .handle(message -> LOG.info("Received message on port {}: {}", port, message))
                .get();
        IntegrationFlowRegistration register = flowContext.registration(flow).register();
        portsInUse.put(port, register);
    }

    static class AllocateResponse {
        public Integer port;

        public AllocateResponse(int port) {
            this.port = port;
        }
    }
}

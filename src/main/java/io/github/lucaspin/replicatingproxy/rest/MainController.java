package io.github.lucaspin.replicatingproxy.rest;

import io.github.lucaspin.replicatingproxy.service.CustomMessageHandler;
import io.github.lucaspin.replicatingproxy.service.RTPManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.web.bind.annotation.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;

@RestController
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final IntegrationFlowContext flowContext;
    private final RTPManager rtpManager;
    private final Queue<Integer> availablePorts;
    private final Map<Integer, IntegrationFlowRegistration> portsInUse = new HashMap<>();

    public MainController(IntegrationFlowContext flowContext, RTPManager rtpManager) {
        this.flowContext = flowContext;
        this.rtpManager = rtpManager;
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

        stopInboundAdapter(registration, port);
        return ResponseEntity.ok().build();
    }

    private void stopInboundAdapter(IntegrationFlowRegistration registration, Integer port) {
        registration.stop();
        registration.destroy();
        portsInUse.remove(port);
        availablePorts.add(port);
    }

    private void registerNewInboundAdapter(Integer port) {
        StandardIntegrationFlow flow = IntegrationFlows.from(new UnicastReceivingChannelAdapter(port))
                .handle(new CustomMessageHandler(rtpManager))
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

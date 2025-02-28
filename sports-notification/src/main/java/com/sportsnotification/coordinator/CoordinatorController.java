package com.sportsnotification.coordinator;

import com.sportsnotification.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Profile("coordinator")
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {

    @Autowired
    private CoordinatorService coordinatorService;

    @PostMapping("/register")
    public List<Broker>  register(@RequestBody Broker broker) {
        return coordinatorService.register(broker);
    }

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody Heartbeat heartbeat) {
        coordinatorService.heartbeat(heartbeat);
    }

    @GetMapping("/brokers")
    public List<Broker> getBrokers() {
        return coordinatorService.getBrokers();
    }

    @GetMapping("/leader")
    public Broker getLeader() {
        return coordinatorService.getLeader();
    }
}
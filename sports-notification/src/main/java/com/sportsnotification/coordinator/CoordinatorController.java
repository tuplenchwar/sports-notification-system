package com.sportsnotification.coordinator;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.CoordinatorSyncData;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/sync-data")
    public ResponseEntity<String> syncData(@RequestBody CoordinatorSyncData syncData) {
        coordinatorService.syncData(syncData);
        return ResponseEntity.ok("Sync successful");
    }

    @GetMapping("/isPrimary-coordinator")
    public boolean isPrimaryCoordinator() {
        return coordinatorService.isPrimaryCoordinator();
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
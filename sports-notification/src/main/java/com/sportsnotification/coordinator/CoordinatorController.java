package com.sportsnotification.coordinator;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.CoordinatorHeartbeat;
import com.sportsnotification.dto.CoordinatorSyncData;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Profile("coordinator")
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorController.class);

    @Autowired
    private CoordinatorService coordinatorService;

    @PostMapping("/register")
    public ResponseEntity<List<Broker>> register(@RequestBody Broker broker) {
        try {
            logger.info("Registering broker: {}", broker);
            return ResponseEntity.ok(coordinatorService.register(broker));
        } catch (Exception e) {
            logger.error("Error registering broker", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/register-coordinator")
    public ResponseEntity<Void> registerCoordinator(@RequestParam String secondaryCoordinatorURL) {
        try {
            logger.info("Registering secondary coordinator: {}", secondaryCoordinatorURL);
            coordinatorService.registerCoordinator(secondaryCoordinatorURL);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error registering secondary coordinator", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/coordinator-heartbeat")
    public ResponseEntity<Void> coordinatorHeartbeat(@RequestBody CoordinatorHeartbeat coordinatorHeartbeat) {
        try {
            logger.info("Received coordinator heartbeat: {}", coordinatorHeartbeat);
            coordinatorService.coordinatorHeartbeat(coordinatorHeartbeat);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing coordinator heartbeat", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody Heartbeat heartbeat) {
        try {
            logger.info("Received broker heartbeat: {}", heartbeat);
            coordinatorService.heartbeat(heartbeat);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing broker heartbeat", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/sync-data")
    public ResponseEntity<String> syncData(@RequestBody CoordinatorSyncData syncData) {
        try {
            logger.info("Syncing coordinator data: {}", syncData);
            coordinatorService.syncData(syncData);
            return ResponseEntity.ok("Sync successful");
        } catch (Exception e) {
            logger.error("Error syncing data", e);
            return ResponseEntity.internalServerError().body("Sync failed");
        }
    }

    @GetMapping("/isPrimary-coordinator")
    public ResponseEntity<Boolean> isPrimaryCoordinator() {
        try {
            boolean isPrimary = coordinatorService.isPrimaryCoordinator();
            logger.info("Primary coordinator status: {}", isPrimary);
            return ResponseEntity.ok(isPrimary);
        } catch (Exception e) {
            logger.error("Error checking primary coordinator status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/brokers")
    public ResponseEntity<List<Broker>> getBrokers() {
        try {
            logger.info("Fetching all brokers");
            return ResponseEntity.ok(coordinatorService.getBrokers());
        } catch (Exception e) {
            logger.error("Error fetching brokers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leader")
    public ResponseEntity<Broker> getLeader() {
        try {
            logger.info("Fetching leader broker");
            return ResponseEntity.ok(coordinatorService.getLeader());
        } catch (Exception e) {
            logger.error("Error fetching leader broker", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

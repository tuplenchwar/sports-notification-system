package com.sportsnotification.coordinator;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.CoordinatorHeartbeat;
import com.sportsnotification.dto.CoordinatorSyncData;
import com.sportsnotification.dto.Heartbeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Profile("coordinator")
@Service
@EnableAsync
public class CoordinatorService {
    private static final Logger logger = LoggerFactory.getLogger(CoordinatorService.class);
    private static final String METADATA_URL = "http://169.254.169.254/latest/meta-data/public-ipv4";
    private static final String PRIMARY_TAG = "Primary-Coordinator";
    private static final String SECONDARY_TAG = "Secondary-Coordinator";
    private static final Region AWS_REGION = Region.US_WEST_2;
    private final List<Broker> brokers = new ArrayList<>();
    private final ConcurrentHashMap<Integer, Long> brokerHeartbeatMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();
    private Broker leaderBroker = new Broker();
    //DS for coordinator
    private boolean isPrimary = false;
    //Run locally
    @Value("${isLocal:false}")
    private boolean isLocal;
    @Value("${isLocalPrimary:false}")
    private boolean isLocalPrimary;
    @Value("${secondaryCoordinatorURL:http://localhost:8080}")
    private String secondaryCoordinatorURL;
    @Value("${primaryCoordinatorURL:http://localhost:8080}")
    private String primaryCoordinatorURL;
    private final ConcurrentHashMap<String, Long> coordinatorHeartBeatMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (isLocal) {
                setConnectionUrls();
                if (isLocalPrimary) {
                    startBrokerHeartbeatMonitor();
                } else {
                    sendSecondaryCoordinatorRegistration();
                }
            } else {
                determinePrimary();
                setConnectionUrls();
                if (isPrimary) {
                    startBrokerHeartbeatMonitor();
                } else {
                    sendSecondaryCoordinatorRegistration();
                }
            }
        } catch (Exception e) {
            logger.error("Error during service initialization", e);
        }
    }

    public List<Broker> register(Broker broker) {
        try {
            if (brokers.isEmpty()) {
                broker.setLeader(true);
                leaderBroker = broker;
            } else {
                broker.setLeader(false);
            }
            brokers.add(broker);
            sendBrokersListToAllBrokersAsync(broker);
            return brokers;
        } catch (Exception e) {
            logger.error("Error registering broker: {}", broker, e);
            return Collections.emptyList();
        }
    }

    public void heartbeat(Heartbeat heartbeat) {
        try {
            brokerHeartbeatMap.put(heartbeat.getBrokers().getId(), System.currentTimeMillis());
        } catch (Exception e) {
            logger.error("Error processing heartbeat: {}", heartbeat, e);
        }
    }

    public void coordinatorHeartbeat(CoordinatorHeartbeat coordinatorHeartbeat) {
        try {
            if (coordinatorHeartBeatMap.containsKey(coordinatorHeartbeat.getCoordinatorURL())) {
                coordinatorHeartBeatMap.replace(coordinatorHeartbeat.getCoordinatorURL(), coordinatorHeartbeat.getHeartbeatTimestamp());
            } else {
                coordinatorHeartBeatMap.put(coordinatorHeartbeat.getCoordinatorURL(), coordinatorHeartbeat.getHeartbeatTimestamp());
            }
        } catch (Exception e) {
            logger.error("Error processing coordinator heartbeat: {}", coordinatorHeartbeat, e);
        }
    }

    public Broker getLeader() {
        return leaderBroker;
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void syncData(CoordinatorSyncData syncData) {
        try {
            if (!isPrimary) {
                brokers.clear();
                brokers.addAll(syncData.getBrokers());
                leaderBroker = syncData.getLeaderBroker();
                brokerHeartbeatMap.clear();
                brokerHeartbeatMap.putAll(syncData.getBrokerHeartbeatMap());

                logger.info("Secondary coordinator data synced.");
            }
        } catch (Exception e) {
            logger.error("Error syncing data", e);
        }
    }

    public boolean isPrimaryCoordinator() {
        return isPrimary;
    }

    public void registerCoordinator(String secondaryCoordinatorURL) {
        try {
            this.secondaryCoordinatorURL = secondaryCoordinatorURL;
            startSendingHeartbeatToSecondary();
            startSyncingWithSecondary(this.secondaryCoordinatorURL);
        } catch (Exception e) {
            logger.error("Error registering secondary coordinator", e);
        }
    }

    public void sendSecondaryCoordinatorRegistration() {
        String primaryUrl = primaryCoordinatorURL + "/coordinator/register-coordinator?secondaryCoordinatorURL="
                + secondaryCoordinatorURL;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // Set correct content type

            HttpEntity<String> requestEntity = new HttpEntity<>("{}", headers); // Sending an empty JSON body

            restTemplate.postForEntity(primaryUrl, requestEntity, String.class);
            logger.info("Registered secondary coordinator with primary: {}", primaryCoordinatorURL);
            startPrimaryCoordinatorMonitor();
        } catch (Exception e) {
            logger.error("Failed to register secondary coordinator", e);
        }
    }

    private void startPrimaryCoordinatorMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                coordinatorHeartBeatMap.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 5000);

                if (coordinatorHeartBeatMap.isEmpty()) {
                    logger.warn("Primary coordinator is down. Promoting secondary to primary.");
                    isPrimary = true;
                    startBrokerHeartbeatMonitor();
                }
            } catch (Exception e) {
                logger.error("Error monitoring primary coordinator", e);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void startBrokerHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                List<Integer> brokersToRemove = new ArrayList<>();

                for (Map.Entry<Integer, Long> entry : brokerHeartbeatMap.entrySet()) {
                    int brokerId = entry.getKey();
                    long lastHeartbeat = entry.getValue();

                    if ((currentTime - lastHeartbeat) > 5000) { // 5 seconds timeout
                        brokersToRemove.add(brokerId);
                    }
                }

                for (Integer brokerId : brokersToRemove) {
                    Broker broker = removeBrokerById(brokerId);
                    brokerHeartbeatMap.remove(brokerId);
                    // removeBrokerById(brokerId);
                    if (broker != null && broker.isLeader()) {
                        electNewLeader();
                    } else {
                        sendBrokersListToAllBrokersAsync(broker);
                    }
                }
            } catch (Exception e) {
                logger.error("Error monitoring broker heartbeats", e);
            }

        }, 0, 3, TimeUnit.SECONDS); // Check every 3 seconds
    }

    private void startSendingHeartbeatToSecondary() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                CoordinatorHeartbeat coordinatorHeartbeat = new CoordinatorHeartbeat(System.currentTimeMillis(), primaryCoordinatorURL);
                String secondaryUrl = secondaryCoordinatorURL + "/coordinator/coordinator-heartbeat";
                restTemplate.postForEntity(secondaryUrl, coordinatorHeartbeat, String.class);
                logger.info("Sent heartbeat to secondary: " + secondaryCoordinatorURL);
            } catch (Exception e) {
                logger.error("Failed to send heartbeat to secondary: " + secondaryCoordinatorURL);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private Broker removeBrokerById(int brokerId) {
        for (Broker broker : brokers) {
            if (broker.getId() == brokerId) {
                brokers.remove(broker);
                return broker;
            }
        }
        return null;
    }

    private void electNewLeader() {
        Optional<Broker> newLeaderOpt = brokers.stream()
                .max(Comparator.comparingInt(Broker::getId));

        newLeaderOpt.ifPresent(newLeader -> {
            newLeader.setLeader(true);
            leaderBroker = newLeader;
            notifyBrokersAboutNewLeader(newLeader);
        });
    }

    private void notifyBrokersAboutNewLeader(Broker newLeader) {
        for (Broker broker : brokers) {
            try {
                String brokerUrl = broker.getConnectionUrl() + "/broker/update-leader";
                restTemplate.put(brokerUrl, newLeader, Broker.class);
                logger.info("New leader broker: " + broker.getId());
                sendBrokersListToAllBrokersAsync(null);
            } catch (Exception e) {
                logger.error("Failed to notify broker: " + broker.getId());
            }
        }
    }

    private void determinePrimary() {
        String instanceId = getInstanceId();
        if (instanceId == null) {
            logger.error("Failed to fetch instance ID.");
            return;
        }

        String instanceTag = getInstanceTag(instanceId);
        if (PRIMARY_TAG.equalsIgnoreCase(instanceTag)) {
            isPrimary = true;
        } else if (SECONDARY_TAG.equalsIgnoreCase(instanceTag)) {
            isPrimary = false;
        }
        logger.info("Running on instance " + instanceId + " with tag: " + instanceTag);
        logger.info("isPrimary: " + isPrimary);
    }

    private String getInstanceId() {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                return scanner.nextLine();
            }
        } catch (Exception e) {
            logger.error("Error retrieving instance ID: " + e.getMessage());
            return null;
        }
    }

    private String getInstanceTag(String instanceId) {
        try (Ec2Client ec2Client = Ec2Client.create()) {
            DescribeInstancesResponse response = ec2Client.describeInstances(
                    DescribeInstancesRequest.builder().instanceIds(instanceId).build()
            );

            List<Instance> instances = response.reservations().get(0).instances();
            for (Instance instance : instances) {
                for (Tag tag : instance.tags()) {
                    if ("Name".equalsIgnoreCase(tag.key())) {
                        return tag.value();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving instance tag: " + e.getMessage());
        }
        return null;
    }


    private void setConnectionUrls() {
        if (isLocal) {
            this.primaryCoordinatorURL = "http://localhost:8080";
            this.secondaryCoordinatorURL = "http://localhost:8081";
        } else {
            try (Ec2Client ec2Client = Ec2Client.builder()
                    .region(AWS_REGION)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()) {

                DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder().build());

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        String publicIp = instance.publicIpAddress();
                        if (publicIp == null || instance.state().nameAsString().equalsIgnoreCase("terminated")) {
                            continue; // Skip instances that are not running
                        }

                        for (Tag tag : instance.tags()) {
                            if (PRIMARY_TAG.equalsIgnoreCase(tag.value())) {
                                this.primaryCoordinatorURL = "http://" + publicIp + ":8080";
                            } else if (SECONDARY_TAG.equalsIgnoreCase(tag.value())) {
                                this.secondaryCoordinatorURL = "http://" + publicIp + ":8080";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error retrieving EC2 connection URLs: " + e.getMessage());
            }
        }
    }

    private void startSyncingWithSecondary(String secondaryCoordinatorURL) {
        scheduler.scheduleAtFixedRate(() -> {
            {
                try {
                    String secondaryUrl = secondaryCoordinatorURL + "/coordinator/sync-data";
                    CoordinatorSyncData syncData = new CoordinatorSyncData(brokers, leaderBroker, brokerHeartbeatMap);
                    restTemplate.postForEntity(secondaryUrl, syncData, String.class);
                    logger.info("Synced data with secondary: " + secondaryCoordinatorURL);
                } catch (Exception e) {
                    logger.error("Failed to sync data with secondary: " + secondaryCoordinatorURL);
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }


    @Async
    protected void sendBrokersListToAllBrokersAsync(Broker skipBroker) {
        for (Broker broker : brokers) {
            if (skipBroker != null && broker.getId() == skipBroker.getId()) {
                continue; // Skip notifying the broker immediately
            }
            try {
                String brokerUrl = broker.getConnectionUrl() + "/broker/update-brokers";
                restTemplate.put(brokerUrl, brokers, List.class);
                logger.info("Updated brokers list for broker: " + broker.getId());
            } catch (Exception e) {
                logger.error("Failed to update brokers list for broker: " + broker.getId());
            }
        }
    }

}

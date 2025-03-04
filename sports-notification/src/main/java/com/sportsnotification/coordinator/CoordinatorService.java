package com.sportsnotification.coordinator;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.CoordinatorHeartbeat;
import com.sportsnotification.dto.CoordinatorSyncData;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.regions.Region;
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
    private final List<Broker> brokers = new ArrayList<>();
    private Broker leaderBroker = new Broker();
    private final ConcurrentHashMap<Integer, Long> brokerHeartbeatMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final RestTemplate restTemplate = new RestTemplate();


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

    private static final String METADATA_URL = "http://169.254.169.254/latest/meta-data/public-ipv4";
    private static final String PRIMARY_TAG = "Primary-Coordinator";
    private static final String SECONDARY_TAG = "Secondary-Coordinator";
    private static final Region AWS_REGION = Region.US_WEST_2;
    private ConcurrentHashMap<String,Long> coordinatorHeartBeatMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if(isLocal) {
            setConnectionUrls();
            if(isLocalPrimary) {
                startBrokerHeartbeatMonitor();// Keeps track of broker health
            }else{
                sendSecondaryCoordinatorRegistration();
            }
        }else{
            determinePrimary(); // Assigns role (primary/secondary)
            setConnectionUrls();
            if(isPrimary){
                startBrokerHeartbeatMonitor(); // Keeps track of broker health
            }else {
                sendSecondaryCoordinatorRegistration();
            }
        }
    }

    public List<Broker> register(Broker broker) {
        if (brokers.isEmpty()) {
            broker.setLeader(true);
            leaderBroker = broker;
        } else {
            broker.setLeader(false);
        }
        brokers.add(broker);
        sendBrokersListToAllBrokersAsync(broker);
        return brokers;
    }

    public void heartbeat(Heartbeat heartbeat) {
        brokerHeartbeatMap.put(heartbeat.getBrokers().getId(), System.currentTimeMillis());
    }

    public void coordinatorHeartbeat(CoordinatorHeartbeat coordinatorHeartbeat) {
        if (coordinatorHeartBeatMap.containsKey(coordinatorHeartbeat.getCoordinatorURL())) {
            coordinatorHeartBeatMap.replace(coordinatorHeartbeat.getCoordinatorURL(), coordinatorHeartbeat.getHeartbeatTimestamp());
        } else {
            coordinatorHeartBeatMap.put(coordinatorHeartbeat.getCoordinatorURL(), coordinatorHeartbeat.getHeartbeatTimestamp());
        }
    }

    public Broker getLeader() {
        return leaderBroker;
    }

    public List<Broker> getBrokers(){
        return brokers;
    }

    public void syncData(CoordinatorSyncData syncData) {
        if (!isPrimary) { // Ensure only the secondary updates its state
            this.brokers.clear();
            this.brokers.addAll(syncData.getBrokers());

            this.leaderBroker = syncData.getLeaderBroker();
            this.brokerHeartbeatMap.clear();
            this.brokerHeartbeatMap.putAll(syncData.getBrokerHeartbeatMap());

            System.out.println("Secondary coordinator data is synced.");
        }
    }

    public boolean isPrimaryCoordinator() {
        return isPrimary;
    }

    public void registerCoordinator(String secondaryCoordinatorURL){
        this.secondaryCoordinatorURL = secondaryCoordinatorURL;
        startSendingHeartbeatToSecondary();
        startSyncingWithSecondary(this.secondaryCoordinatorURL);

    }

    public void sendSecondaryCoordinatorRegistration(){
        String primaryUrl = primaryCoordinatorURL + "/coordinator/register-coordinator?secondaryCoordinatorURL="
                + secondaryCoordinatorURL;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // Set correct content type

            HttpEntity<String> requestEntity = new HttpEntity<>("{}", headers); // Sending an empty JSON body

            restTemplate.postForEntity(primaryUrl, requestEntity, String.class);
            System.out.println("Registered secondary coordinator with primary: " + primaryCoordinatorURL);
            startPrimaryCoordinatorMonitor();
        } catch (Exception e) {
            System.err.println("Failed to register secondary coordinator with primary: " + primaryUrl);
            //exitApplication();
        }
    }

    private void startPrimaryCoordinatorMonitor(){
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, Long> entry : coordinatorHeartBeatMap.entrySet()) {
                long lastHeartbeat = entry.getValue();

                if ((currentTime - lastHeartbeat) > 5000) { // 5 seconds timeout
                    System.err.println("Primary coordinator is down. Promoting secondary to primary.");
                    isPrimary = true;
                    coordinatorHeartBeatMap.clear();
                    startBrokerHeartbeatMonitor();
                    break;
                }
            }

        }, 0, 3, TimeUnit.SECONDS);
    }

    private void startBrokerHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
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
        }, 0, 3, TimeUnit.SECONDS); // Check every 3 seconds
    }

    private void startSendingHeartbeatToSecondary() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                CoordinatorHeartbeat coordinatorHeartbeat = new CoordinatorHeartbeat(System.currentTimeMillis(), primaryCoordinatorURL);
                String secondaryUrl = secondaryCoordinatorURL + "/coordinator/coordinator-heartbeat";
                restTemplate.postForEntity(secondaryUrl, coordinatorHeartbeat, String.class);
                System.out.println("Sent heartbeat to secondary: " + secondaryCoordinatorURL);
            } catch (Exception e) {
                System.err.println("Failed to send heartbeat to secondary: " + secondaryCoordinatorURL);
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
                    System.out.println("New leader broker: " + broker.getId());
                    sendBrokersListToAllBrokersAsync(null);
                } catch (Exception e) {
                    System.err.println("Failed to notify broker: " + broker.getId());
                }
        }
    }

    private void determinePrimary() {
        String instanceId = getInstanceId();
        if (instanceId == null) {
            System.err.println("Failed to fetch instance ID.");
            return;
        }

        String instanceTag = getInstanceTag(instanceId);
        if (PRIMARY_TAG.equalsIgnoreCase(instanceTag)) {
            isPrimary = true;
        } else if (SECONDARY_TAG.equalsIgnoreCase(instanceTag)) {
            isPrimary = false;
        }
        System.out.println("Running on instance " + instanceId + " with tag: " + instanceTag);
        System.out.println("isPrimary: " + isPrimary);
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
            System.err.println("Error retrieving instance ID: " + e.getMessage());
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
            System.err.println("Error retrieving instance tag: " + e.getMessage());
        }
        return null;
    }


    private void setConnectionUrls() {
            if(isLocal){
                this.primaryCoordinatorURL = "http://localhost:8080";
                this.secondaryCoordinatorURL = "http://localhost:8081";
            }
            else {
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
                    System.err.println("Error retrieving EC2 connection URLs: " + e.getMessage());
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
                    System.out.println("Synced data with secondary: " + secondaryCoordinatorURL);
                } catch (Exception e) {
                    System.err.println("Failed to sync data with secondary: " + secondaryCoordinatorURL);
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
                    System.out.println("Updated brokers list for broker: " + broker.getId());
                } catch (Exception e) {
                    System.err.println("Failed to update brokers list for broker: " + broker.getId());
            }
        }
    }

}

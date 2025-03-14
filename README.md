# AWS Setup Runbook for Distributed Sports Notification Pub-Sub System

This runbook provides step-by-step instructions to set up and run the Distributed Sports Notification Pub-Sub system project on AWS.

## Prerequisites:
- Domain purchased and hosted zone set up on AWS Route53.
- EC2 key pair created (`coordinator-key-pair.pem`).
- Docker images available locally (`coordinator-image.tar`, etc.).
- AWS credentials ready.

## YAML Configuration for AWS Setup

```yaml
AWSTemplateFormatVersion: '2025-11-03'
Description: 'Template for sports-buzz AWS resources configuration'

Resources:
  HostedZone:
    Type: 'AWS::Route53::HostedZone'
    Properties:
      Name: coordinator-sports-notification.click
      
  PrimaryARecord:
    Type: 'AWS::Route53::RecordSet'
    Properties:
      HostedZoneId: !Ref HostedZone
      Name: coordinator-sports-notification.click.
      Type: A
      TTL: 30
      ResourceRecords:
        - 44.246.139.142
      SetIdentifier: Primary_coordinator
      Failover: PRIMARY
      HealthCheckId: f04a1e04-d944-4074-b509-dbe0f22de72f

  SecondaryARecord:
    Type: 'AWS::Route53::RecordSet'
    Properties:
      HostedZoneId: !Ref HostedZone
      Name: coordinator-sports-notification.click.
      Type: A
      TTL: 300
      ResourceRecords:
        - 52.41.4.136
      SetIdentifier: Secondary_coordinator
      Failover: SECONDARY
      HealthCheckId: bff13903-e343-4985-b448-f0f7b59db981

  NSRecord:
    Type: 'AWS::Route53::RecordSet'
    Properties:
      HostedZoneId: !Ref HostedZone
      Name: coordinator-sports-notification.click.
      Type: NS
      TTL: 172800
      ResourceRecords:
        - ns-262.awsdns-32.com.
        - ns-1222.awsdns-24.org.
        - ns-937.awsdns-53.net.
        - ns-1868.awsdns-41.co.uk.

  SOARecord:
    Type: 'AWS::Route53::RecordSet'
    Properties:
      HostedZoneId: !Ref HostedZone
      Name: coordinator-sports-notification.click.
      Type: SOA
      TTL: 900
      ResourceRecords:
        - ns-262.awsdns-32.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400

  PrimaryCoordinatorInstance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGroups: 
        - launch-wizard-1
      SubnetId: subnet-00e76b9560d843c6b
      Tags:
        - Key: Name
          Value: Primary-Coordinator
      BlockDeviceMappings:
        - DeviceName: /dev/xvda
          Ebs:
            DeleteOnTermination: true
      DisableApiTermination: false
      EbsOptimized: false
      Monitoring: false
      SourceDestCheck: true

  SecondaryCoordinatorInstance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGroups:
        - launch-wizard-1
      SubnetId: subnet-00e76b9560d843c6b
      Tags:
        - Key: Name
          Value: Secondary-Coordinator
      BlockDeviceMappings:
        - DeviceName: /dev/xvda
          Ebs:
            DeleteOnTermination: true
      DisableApiTermination: false
      EbsOptimized: false
      Monitoring: false
      SourceDestCheck: true

  Broker1Instance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGroups:
        - launch-wizard-1
      SubnetId: subnet-00e76b9560d843c6b
      Tags:
        - Key: Name
          Value: Broker_1
      BlockDeviceMappings:
        - DeviceName: /dev/xvda
          Ebs:
            DeleteOnTermination: true
      DisableApiTermination: false
      EbsOptimized: false
      Monitoring: false
      SourceDestCheck: true

  Broker2Instance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGroups:
        - launch-wizard-1
      SubnetId: subnet-00e76b9560d843c6b
      Tags:
        - Key: Name
          Value: Broker_2
      BlockDeviceMappings:
        - DeviceName: /dev/xvda
          Ebs:
            DeleteOnTermination: true
      DisableApiTermination: false
      EbsOptimized: false
      Monitoring: false
      SourceDestCheck: true

  Broker3Instance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGroups:
        - launch-wizard-1
      SubnetId: subnet-00e76b9560d843c6b
      Tags:
        - Key: Name
          Value: Broker_3
      BlockDeviceMappings:
        - DeviceName: /dev/xvda
          Ebs:
            DeleteOnTermination: true
      DisableApiTermination: false
      EbsOptimized: false
      Monitoring: false
      SourceDestCheck: true

  Subscriber1Instance:
    Type: 'AWS::EC2::Instance'
    Properties:
      ImageId: ami-027951e78de46a00e
      InstanceType: t2.micro
      KeyName: coordinator-key-pair
      SecurityGro

  LaunchWizard1SecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupName: launch-wizard-1
      GroupDescription: launch-wizard-1 created 2025-03-02T05:07:03.724Z
      VpcId: vpc-08bda50ef26f6056e
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 8080
          ToPort: 8080
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 22
          ToPort: 22
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 8091
          ToPort: 8091
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 3000
          ToPort: 3000
          CidrIp: 0.0.0.0/0
      SecurityGroupEgress:
        - IpProtocol: -1  # -1 means all protocols
          FromPort: -1    # -1 means all ports
          ToPort: -1      # -1 means all ports
          CidrIp: 0.0.0.0/0

  # Optional: Add tags if needed
  Tags:
    - Key: Name
      Value: launch-wizard-1

  PrimaryCoordinatorHealthCheck:
    Type: AWS::Route53::HealthCheck
    Properties:
      HealthCheckConfig:
        IPAddress: 44.246.139.142
        Port: 8080
        Type: HTTP
        ResourcePath: '/coordinator/brokers'
        RequestInterval: 30
        FailureThreshold: 1
        EnableSNI: false
        MeasureLatency: false
        Inverted: false
        Disabled: false
        Regions:
          - us-east-1
          - us-west-1
          - us-west-2
      HealthCheckTags:
        - Key: Name
          Value: PrimaryCoordinatorHealthCheck

  SecondaryCoordinatorHealthCheck:
    Type: AWS::Route53::HealthCheck
    Properties:
      HealthCheckConfig:
        IPAddress: 52.41.4.136
        Port: 8080
        Type: HTTP
        ResourcePath: '/coordinator/brokers'
        RequestInterval: 30
        FailureThreshold: 1
        EnableSNI: false
        MeasureLatency: false
        Inverted: false
        Disabled: false
        Regions:
          - us-east-1
          - us-west-1
          - us-west-2
      HealthCheckTags:
        - Key: Name
          Value: SecondaryCoordinatorHealthCheck

Outputs:
  PrimaryHealthCheckId:
    Description: 'Health Check ID for Primary Coordinator'
    Value: !Ref PrimaryCoordinatorHealthCheck
  SecondaryHealthCheckId:
    Description: 'Health Check ID for Secondary Coordinator'
    Value: !Ref SecondaryCoordinatorHealthCheck
```

## Post Deployment Steps and EC2 Instance Setup Commands
- SSH into each EC2 instance.
```bash
ssh -i ~/Downloads/coordinator-key-pair.pem ec2-user@<EC2-Public-DNS>
```

- Export AWS keys on EC2 machines and allowed access AWS APIs
```bash
export AWS_REGION=us-west-2
export AWS_ACCESS_KEY_ID=<Your Access Key>
export AWS_SECRET_ACCESS_KEY=<Your Secret Access Key>
aws ec2 modify-instance-metadata-options --instance-id <INSTANCE_ID> --http-tokens optional --http-endpoint enabled
```
- Install Docker 
```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
newgrp docker
```

- Use below command to scp(copy) tar files to respective EC2 instances
```bash
scp -i ~/Downloads/coordinator-key-pair.pem <docker-image-file>.tar ec2-user@<EC2-IP>:/home/ec2-user/

```
- Load and run container images
```bash
docker load -i <docker-image-file>.tar
docker run -d -p 8080:8080 <docker-image-name>
docker logs -f <container_id_or_name>
```
- Configure Route53 Hosted Zone and health checks for Coordinator instances (Mentioned in yaml file).


## Route53 Health Check Configuration

 - Configure Route53 health checks for your hosted zone as follows:

 - Health check for Primary Coordinator EC2 IP:

   - Specify endpoint by: IP address

   - Protocol: HTTP

   - IP address: _Primary Coordinator IP address_

   - Host name: _domain_name_

   - Port: 8080

   - Path: _/coordinator/brokers_

Ensure Route53 health checks pass by:
```bash
curl --location 'http://<Coordinator-IP>:8080/coordinator/brokers'
```
Do the same for secondary coordinator as well. 

## Final Checks
- Confirm all EC2 instances are up.
- Validate Docker containers with `docker ps`.
- Ensure Route53 health checks are successful.

Your Distributed sport-notification pub-sub System is now ready for use.


## Division of work:

| Task                                  | Owner          |
|----------------------------------------|---------------|
| Publisher (Backend + Frontend)        | Inderpreet    |
| Coordinator + Route53 setup + Broker + Protocols | Tanvi         |
| Subscriber (Backend + Frontend)       | Gauri         |
| Testing                               | Team          |
| Deployment on AWS                     | Team          |
| Presentation                          | Team          |
| Report                                | Team          |

 


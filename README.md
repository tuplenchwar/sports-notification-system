# AWS Setup Runbook for Distributed Pub-Sub System

This runbook provides step-by-step instructions to set up and run the Distributed Pub-Sub system project on AWS.

## Prerequisites:
- Domain purchased and hosted zone set up on AWS Route53.
- EC2 key pair created (`coordinator-key-pair.pem`).
- Docker images available locally (`coordinator-image.tar`, etc.).
- AWS credentials ready.

## YAML Configuration for AWS Setup

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Distributed Pub-Sub System Setup

Parameters:
  KeyPairName:
    Type: AWS::EC2::KeyPair::KeyName
    Description: Name of an existing EC2 KeyPair for SSH access
  InstanceType:
    Type: String
    Default: t2.micro

Resources:
  CoordinatorInstance1:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: t2.micro
      KeyName: !Ref KeyPairName
      ImageId: ami-0abcdef1234567890 # replace with actual AMI ID
      SecurityGroupIds:
        - sg-1234567890abcdefg # replace with actual Security Group ID
      Tags:
        - Key: Name
          Value: Coordinator1

  Coordinator2:
    Type: AWS::EC2::Instance
    Properties:
      KeyName: !Ref KeyPairName
      ImageId: ami-0abcdef1234567890 # replace with actual AMI ID
      SecurityGroupIds:
        - sg-1234567890abcdefg # replace with actual Security Group ID
      Tags:
        - Key: Name
          Value: Coordinator2

  Brokers:
    Type: AWS::EC2::Instance
    Properties:
      ImageId: ami-0abcdef1234567890 # replace with actual AMI ID
      InstanceType: t2.micro
      KeyName: !Ref KeyPairName
      SecurityGroupIds:
        - sg-1234567890abcdefg # replace with actual Security Group ID
      MinCount: 3
      MaxCount: 3
      Tags:
        - Key: Name
          Value: Broker

  Publisher:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: t2.micro
      ImageId: ami-0abcdef1234567890 # replace with actual AMI ID
      KeyName: !Ref KeyPairName
      SecurityGroupIds:
        - sg-1234567890abcdefg # replace with actual Security Group ID
      Tags:
        - Key: Name
          Value: Publisher

  Subscriber:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: t2.micro
      ImageId: ami-0abcdef1234567890 # replace with actual AMI ID
      KeyName: !Ref KeyPairName
      SecurityGroupIds:
        - sg-1234567890abcdefg # replace with actual Security Group ID
      Tags:
        - Key: Name
          Value: Subscriber

Outputs:
  Coordinator1IP:
    Description: IP Address of the Primary Coordinator
    Value: !GetAtt Coordinator1.PublicIp
  Coordinator2IP:
    Description: IP Address of the Secondary Coordinator
    Value: !GetAtt Coordinator2.PublicIp
```

## Post Deployment Steps
- SSH into each EC2 instance.
- Install Docker and deploy your container images as per the original runbook.
- Configure Route53 Hosted Zone and health checks for Coordinator instances as described earlier.

### EC2 Instance Setup Commands

Connect to each EC2 instance and run:
```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
newgrp docker

export AWS_REGION=us-west-2
export AWS_ACCESS_KEY_ID=<Your Access Key>
export AWS_SECRET_ACCESS_KEY=<Your Secret Access Key>

aws ec2 modify-instance-metadata-options --instance-id <INSTANCE_ID> --http-tokens optional --http-endpoint enabled

scp -i ~/Downloads/coordinator-key-pair.pem <docker-image-file>.tar ec2-user@<EC2-IP>:/home/ec2-user/

ssh -i ~/Downloads/coordinator-key-pair.pem ec2-user@<EC2-Public-DNS>

docker load -i <docker-image-file>.tar
docker run -d -p 8080:8080 <docker-image-name>
```

## Route53 Health Check Configuration

 - Configure Route53 health checks for your hosted zone as follows:

 - Health check for Primary Coordinator EC2 IP:

   - Specify endpoint by: IP address

   - Protocol: HTTP

   - IP address: 52.41.4.136

   - Host name: www.example.com

   - Port: 8080

   - Path: /coordinator/brokers

Ensure Route53 health checks pass by:
```bash
curl --location 'http://<Coordinator-IP>:8080/coordinator/brokers'
```
do the same for secondary coordinator as well. 

## Final Checks
- Confirm all EC2 instances are up.
- Validate Docker containers with `docker ps`.
- Ensure Route53 health checks are successful.

Your Distributed Pub-Sub System is now ready for use.


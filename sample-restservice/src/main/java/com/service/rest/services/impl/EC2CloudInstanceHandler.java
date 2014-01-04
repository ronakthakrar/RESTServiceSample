package com.service.rest.services.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.service.rest.services.ServiceException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.io.IOUtils.*;


/* This class is responsible for handling all the relevant services implementation which are relevant to Amazon AWS EC2 Cloud instances */

public class EC2CloudInstanceHandler {

        public static final String RUNNING_STATE = "running";
        public static final String NAME_TAG_KEY = "Name";
        private String instanceType;
        private int instanceCount;
        private static AmazonEC2 ec2Service;
        private static Properties amazonProperties = new Properties();
        private static InputStream ioStream = null;

        public EC2CloudInstanceHandler() throws ServiceException {
            initializeAWSServices();
        };

        private static void initializeAWSServices() throws ServiceException {
            try{

            /* Get the AWS Credentials properties for Authentication */
                ioStream = new ClassPathResource("aws/AwsCredentials.properties").getInputStream();
                AWSCredentials credentials = new PropertiesCredentials(ioStream);

                /* Construct the AmazonEC2Client using the loaded credentials for using the EC2 services */
                ec2Service = new AmazonEC2Client(credentials);
                closeQuietly(ioStream);

            /* Load the AWS properties to launch the instance including instance type, AMI, security group etc. */
                ioStream = new ClassPathResource("aws/AWSCloud.properties").getInputStream();
                amazonProperties.load(ioStream);
                closeQuietly(ioStream);
            }catch(IOException ioException){

                /* In any case of exception print the stack trace, set the provisioning status as failed,
                also correspondingly set the error message based on the exception*/
                ioException.printStackTrace();
                throw new ServiceException(ioException.getMessage());
            }finally {
                closeQuietly(ioStream);
            }
        }

        public EC2CloudInstanceHandler(String instanceType)throws ServiceException{
            this.instanceType = instanceType;
            this.instanceCount = 1;
            initializeAWSServices();
        }

        public AWSInstanceEx launchNewCloudAgentInstances() throws ServiceException{
            AWSInstanceEx acquiredInstance = null;
            try {

                if (amazonProperties != null) {

                    /* Read the corresponding property for building new instance */
                    String agentServerAMI = amazonProperties.getProperty("AGENT_SERVER_AMI");
                    String securityKeyName = amazonProperties.getProperty("KEY_NAME");
                    String securityGroupName = amazonProperties.getProperty("SECURITY_GROUP_NAME");
                    String availabilityZone = amazonProperties.getProperty("AVAILABILITY_ZONE");
                    String instanceNameTagValue = amazonProperties.getProperty("INSTANCE-TAG-NAME");

                    /* Build the on demand instance request with the details using the above properties */
                    RunInstancesRequest newInstance = new RunInstancesRequest();
                    newInstance.setImageId(agentServerAMI);
                    newInstance.setMinCount(instanceCount);
                    newInstance.setMaxCount(instanceCount);
                    newInstance.setInstanceType(instanceType);
                    newInstance.setKeyName(securityKeyName);

                    /* Set the security group based on the details provided */
                    DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2Service.describeSecurityGroups();
                    List<SecurityGroup> securityGroupsList = describeSecurityGroupsResult.getSecurityGroups();
                    if (CollectionUtils.isNotEmpty(securityGroupsList)) {
                        for (SecurityGroup securityGroup : securityGroupsList) {
                            if (StringUtils.equals(securityGroup.getGroupName(), securityGroupName)) {
                                newInstance.setSecurityGroups(Arrays.asList(securityGroupName));
                            }
                        }
                    }

                    /* Decide the availability zone for the instance and accordingly set the same */
                    Placement placement = new Placement();
                    if (StringUtils.isNotEmpty(availabilityZone)) {
                        DescribeAvailabilityZonesResult availabilityZonesResult = ec2Service.describeAvailabilityZones();
                        List<AvailabilityZone> availabilityZoneList = availabilityZonesResult.getAvailabilityZones();
                        for (AvailabilityZone availabilityZoneInstance : availabilityZoneList) {
                            if (StringUtils.equals(availabilityZoneInstance.getZoneName(), availabilityZone))
                                placement.setAvailabilityZone(availabilityZone);
                        }
                    }
                    newInstance.setPlacement(placement);

                    /* Reserve required instances */
                    String instancesReservationId = ec2Service.runInstances(newInstance).getReservation().getReservationId();

                    /* Get the list of the reserved instances */
                    List<Instance> reservedInstances = getInstancesListByReservation(instancesReservationId, ec2Service);
                    if (reservedInstances != null && !reservedInstances.isEmpty()) {
                        acquiredInstance = acquiredInstance== null ? new AWSInstanceEx(): acquiredInstance;
                        for (Instance eachInstance : reservedInstances) {
                            /* Wait for availability of each instance from the list of instances we have acquired */
                            Instance returnedInstance = waitForAvailabilityOfInstance(instancesReservationId, eachInstance.getInstanceId(), ec2Service);
                            acquiredInstance.setImageId(eachInstance.getImageId());
                            acquiredInstance.setInstanceId(eachInstance.getInstanceId());
                            acquiredInstance.setInstanceType(eachInstance.getInstanceType());
                            acquiredInstance.setPrivateDnsName(eachInstance.getPrivateDnsName());
                            acquiredInstance.setPrivateIpAddress(eachInstance.getPrivateIpAddress());
                            acquiredInstance.setPublicDnsName(eachInstance.getPublicDnsName());
                            acquiredInstance.setPublicIpAddress(eachInstance.getPublicIpAddress());

                            if (returnedInstance == null) {
                                throw new ServiceException("Instance procured not available...!!!");
                            }
                        }

                        /* Tag all of the acquired instances one by one */
                        List<String> resources = new ArrayList<String>();
                        List<Tag> instanceTags = new ArrayList<Tag>();
                        for (Instance eachInstance : reservedInstances) {
                            resources.add(eachInstance.getInstanceId());
                            instanceTags.add(new Tag(NAME_TAG_KEY, instanceNameTagValue));
                        }

                        CreateTagsRequest tagRequest = new CreateTagsRequest();
                        tagRequest.setResources(resources);
                        tagRequest.setTags(instanceTags);
                        ec2Service.createTags(tagRequest);
                    }
                }
                return acquiredInstance;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("exception thrown from launch is " + ex.getMessage());
                System.out.println("Unable to load the instance with given instance details..!!!");
                System.out.println(ex.getMessage());
                throw new ServiceException(ex.getMessage());
            }
        }

        public Boolean terminateInstance(String instanceIdToTerminate) throws ServiceException{
            try{
                TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest(Arrays.asList(instanceIdToTerminate));
                TerminateInstancesResult terminateInstancesResult = ec2Service.terminateInstances(terminateInstancesRequest);
                return Boolean.TRUE;
            }catch(Exception ex){
                throw new ServiceException(ex.getMessage());
            }
        }


        private List<Instance> getInstancesListByReservation(String rID, AmazonEC2 ec2Service) throws ServiceException {
            List<Instance> reservedInstances = null;
            if(ec2Service != null){
                DescribeInstancesResult describeInstancesRequest = ec2Service.describeInstances();
                List<Reservation> reservations = describeInstancesRequest.getReservations();
                for (Reservation reservation : reservations) {
                    if (reservation.getReservationId().equals(rID)) {
                        reservedInstances = reservation.getInstances();
                        break;
                    }
                }
            }
            return reservedInstances;
        }

        private Instance waitForAvailabilityOfInstance(String instancesReservationId, String instanceId, AmazonEC2 ec2Service) throws ServiceException {
        /* Get the list of the reserved instances by reservationId.
        NOTE:- For Instance state check we will have to get the Instance object always via reservationId otherwise state will always be the initial state when we get the list of Instance object after acquiring them */
            Instance returnInstance = null ;
            while(true){
                Instance instance = getInstanceByReservation(instancesReservationId, instanceId, ec2Service);
                if(instance != null && instance.getInstanceId().equals(instanceId)){
                    if(RUNNING_STATE.equals(instance.getState().getName()) && StringUtils.isNotEmpty(instance.getPublicIpAddress()) && StringUtils.isNotEmpty(instance.getPrivateIpAddress())){
                        returnInstance = instance;
                        break;
                    }
                }
                continue;
            }
            return returnInstance;
        }

        private Instance getInstanceByReservation(String instanceReservationId, String instanceId, AmazonEC2 ec2Service) throws ServiceException {
            Instance instanceByReservation = null ;
            List<Instance> reservedInstances = getInstancesListByReservation(instanceReservationId, ec2Service);
            for(Instance instance : reservedInstances){
                if(instanceId != null && !"".equals(instanceId)){
                    if(instance.getInstanceId().equals(instanceId)){
                        instanceByReservation= instance;
                        break;
                    }
                }
            }
            return instanceByReservation;
        }
}
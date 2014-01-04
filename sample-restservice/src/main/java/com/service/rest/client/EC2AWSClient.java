package com.service.rest.client;

import com.amazonaws.services.ec2.model.InstanceType;
import com.service.rest.services.impl.AWSInstanceEx;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.core.MediaType;

public class EC2AWSClient {

    private static final String AWSEC2_InstanceProvisioning_Endpoint = "http://localhost:8080/sample-restservice/awsrest/ec2Service/provisionAWSInstance";
    private static final String AWSEC2_InstanceDeprovisioning_Endpoint = "http://localhost:8080/sample-restservice/awsrest/ec2Service/deprovisionAWSInstance";

    public static void main(String[] args) {

        try {

            ClientRequest clientRequest_provisioning = new ClientRequest(AWSEC2_InstanceProvisioning_Endpoint);
            clientRequest_provisioning.accept(MediaType.APPLICATION_JSON);
            clientRequest_provisioning.queryParameter("instanceType", InstanceType.T1Micro.toString());

            ClientResponse clientResponse_provisioning = clientRequest_provisioning.get();
            boolean isAWSInstanceProvisioned = clientResponse_provisioning.getStatus() == 200;
            if(!isAWSInstanceProvisioned)
            {
                System.out.println("AWS Provisioning Service failed with HTTP Status Code - " + clientResponse_provisioning.getStatus());
                String error = (String) clientResponse_provisioning.getEntity(String.class);
                System.out.println(error);
            }else{
                AWSInstanceEx instance = (AWSInstanceEx) clientResponse_provisioning.getEntity(AWSInstanceEx.class);
                if(instance == null || (instance !=null && instance.getInstanceId()==null)){
                    System.out.println("Instance not provisioned.. ");
                }else
                {
                    if(instance.getInstanceId() != null){
                        System.out.println("InstanceId of provisioned cloud instance is --" + instance.getInstanceId());
                        ClientRequest clientRequest_deProvisioning = new ClientRequest(AWSEC2_InstanceDeprovisioning_Endpoint);
                        clientRequest_deProvisioning.accept(MediaType.APPLICATION_JSON);
                        clientRequest_deProvisioning.queryParameter("instanceIdToTerminate", instance.getInstanceId());

                        ClientResponse clientResponse_deProvisioning = clientRequest_deProvisioning.get();
                        boolean isAWSInstancesdeProvisioned = (clientResponse_deProvisioning.getStatus() == 200);
                        if(!isAWSInstancesdeProvisioned){
                            System.out.println("AWS De-provisioning Service failed with HTTP Status Code :- " + clientResponse_provisioning.getStatus());
                            String error = (String) clientResponse_provisioning.getEntity(String.class);
                            System.out.println(error);
                        }else{
                            Boolean instanceTerminatedRequestSuccessful = (Boolean) clientResponse_deProvisioning.getEntity(Boolean.class);
                            System.out.println("Instance termination request status " + instanceTerminatedRequestSuccessful.booleanValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

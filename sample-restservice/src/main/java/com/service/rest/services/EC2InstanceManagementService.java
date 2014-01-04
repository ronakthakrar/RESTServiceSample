package com.service.rest.services;

import com.amazonaws.services.ec2.model.InstanceType;
import com.service.rest.services.impl.AWSInstanceEx;
import com.service.rest.services.impl.EC2CloudInstanceHandler;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/* Service class for the Amazon AWS EC2 Instances related operations, i.e. provision, de-provision cloud instances on run time */

@Path("/ec2Service")
public class EC2InstanceManagementService {

    @GET
    @Path("/provisionAWSInstance")
    @Produces(MediaType.APPLICATION_JSON)
    public AWSInstanceEx launchNewCloudAgentInstances(@QueryParam("instanceType")String instanceType)throws Exception{
        EC2CloudInstanceHandler ec2CloudInstanceHandler = new EC2CloudInstanceHandler(InstanceType.T1Micro.toString());
        if(instanceType == null)
            throw new ServiceException("instance type cannot be null");
        try{
            return ec2CloudInstanceHandler.launchNewCloudAgentInstances();
        }catch (Exception exception){
            System.out.println("Service provisionAWSInstance failed with following exception");
            System.out.println(exception.getMessage());
            throw  new ServiceException(exception.getMessage());
        }
    }

    @GET
    @Path("/deprovisionAWSInstance")
    @Produces(MediaType.APPLICATION_JSON)
    public Boolean terminateInstance(@QueryParam("instanceIdToTerminate")String instanceIdToTerminate) throws Exception {
        try{
            EC2CloudInstanceHandler ec2CloudInstanceHandler = new EC2CloudInstanceHandler();
            return ec2CloudInstanceHandler.terminateInstance(instanceIdToTerminate);
        }catch(Exception exception){
            System.out.println("Service deprovisionAWSInstance failed with following exception");
            System.out.println(exception.getMessage());
            throw  new ServiceException(exception.getMessage());
        }
    }
}
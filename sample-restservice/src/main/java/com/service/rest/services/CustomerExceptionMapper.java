package com.service.rest.services;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CustomerExceptionMapper implements ExceptionMapper<ServiceException> {

    public CustomerExceptionMapper(){}

    @Override
    public Response toResponse(ServiceException exception) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
}

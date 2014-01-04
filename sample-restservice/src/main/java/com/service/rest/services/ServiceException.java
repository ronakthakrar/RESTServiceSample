package com.service.rest.services;

import java.io.Serializable;

public class ServiceException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;
    public ServiceException(){
        super();
    }

    public ServiceException(String serviceExceptionMessage){
        super(serviceExceptionMessage);
    }

    public ServiceException(String msg, Exception e){
        super(msg,e);
    }

}

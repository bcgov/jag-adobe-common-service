package ca.bc.gov.open.adobe.exceptions;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }
}

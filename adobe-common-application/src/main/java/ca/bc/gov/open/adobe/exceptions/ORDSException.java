package ca.bc.gov.open.adobe.exceptions;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(
        faultCode = FaultCode.CLIENT,
        faultStringOrReason =
                "An error response was received from the adobe life cycle gateWay please check that your request is of valid form")
public class ORDSException extends RuntimeException {
    public ORDSException() {
        super();
    }
}

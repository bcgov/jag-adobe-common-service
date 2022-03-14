package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReference;
import ca.bc.gov.open.adobe.scp.PDFTransformations;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@Slf4j
public class PDFTransformSCPController {

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsSCPWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;

    @Autowired
    public PDFTransformSCPController(
            ObjectMapper objectMapper, WebServiceTemplate webServiceTemplate) {
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformations")
    @ResponsePayload
    public PDFTransformationsResponse transformPDFScp(@RequestPayload PDFTransformations request)
            throws JsonProcessingException {

        return null;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFDiagnosticsByReference")
    @ResponsePayload
    public PDFTransformationsResponse getPDFDiagnosticByReference(
            @RequestPayload PDFDiagnosticsByReference request) throws JsonProcessingException {

        return null;
    }
}

package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.ws.PDFTransformations;
import ca.bc.gov.open.adobe.ws.PDFTransformationsResponse;
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
public class PDFTransformWSController {

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;

    @Autowired
    public PDFTransformWSController(
            ObjectMapper objectMapper, WebServiceTemplate webServiceTemplate) {
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformations")
    @ResponsePayload
    public PDFTransformationsResponse getPDFDiagnostic(@RequestPayload PDFTransformations request)
            throws JsonProcessingException {

        return null;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformationsByReference")
    @ResponsePayload
    public PDFTransformationsResponse getPDFDiagnosticByReference(
            @RequestPayload PDFTransformations request) throws JsonProcessingException {

        return null;
    }
}

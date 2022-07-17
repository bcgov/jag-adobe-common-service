package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.diagnostic.PDFDiagnostics;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReference;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReferenceResponse;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsResponse;
import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
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
public class PDFDiagnosticController {

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFDiagnosticsWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;

    @Autowired
    public PDFDiagnosticController(
            ObjectMapper objectMapper, WebServiceTemplate webServiceTemplate) {
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFDiagnostics")
    @ResponsePayload
    public PDFDiagnosticsResponse getPDFDiagnostic(@RequestPayload PDFDiagnostics request)
            throws JsonProcessingException {

        PDFDiagnosticsResponse resp = new PDFDiagnosticsResponse();
        try {
            resp = (PDFDiagnosticsResponse) webServiceTemplate.marshalSendAndReceive(host, request);

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFDiagnostics")));

            return resp;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to get pdf diagnostic",
                                    "PDFDiagnostics",
                                    ex.getMessage(),
                                    request)));

            return resp;
        }
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFDiagnosticsByReference")
    @ResponsePayload
    public PDFDiagnosticsByReferenceResponse getPDFDiagnosticByReference(
            @RequestPayload PDFDiagnosticsByReference request) throws JsonProcessingException {
        PDFDiagnosticsByReferenceResponse resp = new PDFDiagnosticsByReferenceResponse();
        try {
            resp =
                    (PDFDiagnosticsByReferenceResponse)
                            webServiceTemplate.marshalSendAndReceive(host, request);

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFDiagnosticsByReference")));
            resp.setPDFDiagnosticsByReferenceReturn(1);
            return resp;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to get pdf diagnostic",
                                    "PDFDiagnosticsByReference",
                                    ex.getMessage(),
                                    request)));

            return resp;
        }
    }
}

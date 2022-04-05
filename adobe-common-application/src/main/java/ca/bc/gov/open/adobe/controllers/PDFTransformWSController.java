package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.ws.PDFTransformations;
import ca.bc.gov.open.adobe.ws.PDFTransformationsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Base64Utils;
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
            ObjectMapper objectMapper,
            @Qualifier("transformWS") WebServiceTemplate webServiceTemplate) {
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformations")
    @ResponsePayload
    public PDFTransformationsResponse transformPDFWS(
            @RequestPayload ca.bc.gov.open.adobe.gateway.PDFTransformations request)
            throws JsonProcessingException {
        PDFTransformationsResponse out = new PDFTransformationsResponse();
        try {
            ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse resp =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse)
                            webServiceTemplate.marshalSendAndReceive(host, request);

            out.setStatusVal(1);
            out.setStatusMsg("ok");

            String pdf = Base64Utils.encodeToString(resp.getPDFTransformationsReturn());

            out.setOutputFile(pdf);

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFTransformations")));

            return out;

        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "PDFTransformations",
                                    ex.getMessage(),
                                    null)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        }
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformationsByReference")
    @ResponsePayload
    public PDFTransformationsResponse transformPDFByReference(
            @RequestPayload PDFTransformations request) throws JsonProcessingException {

        PDFTransformationsResponse out = new PDFTransformationsResponse();
        try {
            ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse resp =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse)
                            webServiceTemplate.marshalSendAndReceive(host, request);

            out.setStatusVal(1);
            out.setStatusMsg("ok");

            String pdf = Base64Utils.encodeToString(resp.getPDFTransformationsReturn());

            out.setOutputFile(pdf);

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success", "PDFTransformationsByReference")));

            return out;

        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "PDFTransformationsByReference",
                                    ex.getMessage(),
                                    null)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        }
    }
}

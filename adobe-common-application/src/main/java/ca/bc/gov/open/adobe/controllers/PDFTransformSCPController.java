package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.scp.PDFTransformations;
import ca.bc.gov.open.adobe.scp.PDFTransformations2;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse2;
import ca.bc.gov.open.sftp.starter.JschSessionProvider;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import ca.bc.gov.open.sftp.starter.SftpServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;

@Endpoint
@Slf4j
public class PDFTransformSCPController {

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private final SftpProperties sftpProperties;

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsSCPWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;
    private final String tempFileDir = "temp-pdfs/";

    private final ModelMapper mapper;

    @Autowired JschSessionProvider jschSessionProvider;

    @Autowired
    public PDFTransformSCPController(
            ObjectMapper objectMapper,
            @Qualifier("transformWS") WebServiceTemplate webServiceTemplate,
            ModelMapper mapper,
            SftpProperties sftpProperties) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
        this.sftpProperties = sftpProperties;
    }

    @SoapAction(
            value =
                    "AdobeCommonServices_Source_CommonServices_ws_provider_PDFTransformationsSCPWS_Binder_PDFTransformationsScp")
    @ResponsePayload
    public PDFTransformationsResponse2 transformPDFScpAction(
            @RequestPayload PDFTransformations2 request) throws JsonProcessingException {
        return transformPDFScp(request);
    }

    public PDFTransformationsResponse2 transformPDFScp(@RequestPayload PDFTransformations2 request)
            throws JsonProcessingException {
        File f = null;
        var out = new PDFTransformationsResponse2();
        try {
            // Post File to LCG and convert from base64 encode then write to a file
            var gatewayResp =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse)
                            webServiceTemplate.marshalSendAndReceive(
                                    host,
                                    mapper.map(
                                            request,
                                            ca.bc.gov.open.adobe.gateway.PDFTransformations.class));

            f = new File(tempFileDir + "TmpPDF" + UUID.randomUUID() + ".pdf");
            FileUtils.writeByteArrayToFile(f, gatewayResp.getPDFTransformationsReturn());
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "transformPDFScp",
                                    ex.getMessage(),
                                    request)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        }

        try {
            // SCP the file to a server
            sftpTransfer(request.getRemotefile(), f);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "transformPDFScp")));
            out.setStatusMsg("ok");
            out.setStatusVal(1);
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failure of SFTP to " + sftpProperties.getHost(),
                                    "transformPDFScp",
                                    ex.getMessage(),
                                    request)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        } finally {
            if (f != null && f.exists()) {
                if (!f.delete()) {
                    log.warn("Failed to delete temp pdf file.");
                }
            }
        }
    }

    @SoapAction(
            value =
                    "AdobeCommonServices_Source_CommonServices_ws_provider_PDFTransformationsSCPWS_Binder_PDFTransformationsByReferenceScp")
    @ResponsePayload
    public PDFTransformationsResponse pdfTransformSCPByReferenceAction(
            @RequestPayload PDFTransformations request) throws JsonProcessingException {
        return pdfTransformSCPByReference(request);
    }

    public PDFTransformationsResponse pdfTransformSCPByReference(
            @RequestPayload PDFTransformations request) throws JsonProcessingException {
        File f = null;
        var out = new PDFTransformationsResponse();
        try {
            var gatewayResp =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsByReferenceResponse)
                            webServiceTemplate.marshalSendAndReceive(
                                    host,
                                    mapper.map(
                                            request,
                                            ca.bc.gov.open.adobe.gateway
                                                    .PDFTransformationsByReference.class));

            f = new File(tempFileDir + "TmpPDF" + UUID.randomUUID() + ".pdf");
            FileUtils.writeByteArrayToFile(f, gatewayResp.getPDFTransformationsByReferenceReturn());
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to SFTP",
                                    "pdfTransformSCPByReference",
                                    ex.getMessage(),
                                    request)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        }

        try {
            // SCP the file to a server
            sftpTransfer(request.getRemotefile(), f);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success", "pdfTransformSCPByReference")));
            out.setStatusMsg("ok");
            out.setStatusVal(1);
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failure of SFTP to " + sftpProperties.getHost(),
                                    "pdfTransformSCPByReference",
                                    ex.getMessage(),
                                    request)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        } finally {
            if (f != null && f.exists()) {
                if (!f.delete()) {
                    log.warn("Failed to delete temp pdf file.");
                }
            }
        }
    }

    public void sftpTransfer(String dest, File payload) {
        SftpServiceImpl sftpService = new SftpServiceImpl(jschSessionProvider, sftpProperties);
        InputStream src = new ByteArrayInputStream(payload.getAbsoluteFile().getPath().getBytes(StandardCharsets.UTF_8));
        sftpService.put(src, dest);
    }
}

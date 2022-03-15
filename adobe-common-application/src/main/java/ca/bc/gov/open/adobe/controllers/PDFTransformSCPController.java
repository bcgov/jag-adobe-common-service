package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReference;
import ca.bc.gov.open.adobe.exceptions.AdobeLCGException;
import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.scp.PDFTransformations;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Base64Utils;
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
    private final String tempFileDir = "temp-pdfs/";
    private final SSHClient ssh;

    @Autowired
    public PDFTransformSCPController(
            ObjectMapper objectMapper, WebServiceTemplate webServiceTemplate) throws IOException {
        ssh = new SSHClient();
        ssh.loadKnownHosts();
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformations")
    @ResponsePayload
    public PDFTransformationsResponse transformPDFScp(@RequestPayload PDFTransformations request)
            throws JsonProcessingException {
        File f = null;
        try {
            // Post File to LCG and convert from base64 encode then write to a file
            byte[] gatewayResp = (byte[]) webServiceTemplate.marshalSendAndReceive(host, request);
            byte[] decodedContent = Base64Utils.decode(gatewayResp);
            f = new File(tempFileDir + "TmpPDF" + UUID.randomUUID() + ".pdf");
            FileUtils.writeByteArrayToFile(f, decodedContent);

            // SCP the file to a server
            String host = "", dest = "";
            scpTransfer(dest, host, f);

            // Return the good response
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFTransformations")));
            var out = new PDFTransformationsResponse();
            out.setStatusVal(1);
            out.setStatusMsg("ok");
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to scp message",
                                    "PDFTransformations",
                                    ex.getMessage(),
                                    null)));
            throw new AdobeLCGException();
        } finally {
            if (f != null && f.exists()) {
                if (!f.delete()) {
                    log.warn("Failed to delete temp pdf file.");
                }
            }
        }
    }

    private boolean scpTransfer(String host, String dest, File payload) throws IOException {
        ssh.connect(host);
        try {

            // Not sure allowed but would be best
            ssh.useCompression();

            ssh.newSCPFileTransfer()
                    .upload(new FileSystemFile(payload.getAbsoluteFile().getPath()), dest);
            return true;
        } catch (Exception ex) {
            log.error("Failed to scp file to remote");
        } finally {
            ssh.disconnect();
        }

        return false;
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFDiagnosticsByReference")
    @ResponsePayload
    public PDFTransformationsResponse getPDFDiagnosticByReference(
            @RequestPayload PDFDiagnosticsByReference request) throws JsonProcessingException {

        return null;
    }
}

package ca.bc.gov.open.adobe.controllers;

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
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@Slf4j
public class PDFTransformSCPController {

    @Value("${adobe.ssh.private-key}")
    private String prvtKey = "";

    @Value("${adobe.ssh.public-key}")
    private String pubKey = "";

    @Value("${adobe.ssh.username}")
    private String sshUserName = "";

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsSCPWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;
    private final String tempFileDir = "temp-pdfs/";
    private final SSHClient ssh;

    private final ModelMapper mapper;

    @Autowired
    public PDFTransformSCPController(
            ObjectMapper objectMapper,
            @Qualifier("transformWS") WebServiceTemplate webServiceTemplate,
            ModelMapper mapper)
            throws IOException {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
        ssh = new SSHClient();
        ssh.loadKnownHosts();
    }

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformations")
    @ResponsePayload
    public PDFTransformationsResponse transformPDFScp(@RequestPayload PDFTransformations request)
            throws JsonProcessingException {
        File f = null;
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

            // SCP the file to a server
            scpTransfer(request.getRemotehost(), request.getRemotefile(), f);

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
            var out = new PDFTransformationsResponse();
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

    @PayloadRoot(namespace = SOAP_NAMESPACE, localPart = "PDFTransformationsByReference")
    @ResponsePayload
    public PDFTransformationsResponse pdfTransformSCPByReference(
            @RequestPayload PDFTransformations request) throws JsonProcessingException {
        File f = null;
        try {
            var gatewayResp =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse)
                            webServiceTemplate.marshalSendAndReceive(
                                    host,
                                    mapper.map(
                                            request,
                                            ca.bc.gov.open.adobe.gateway.PDFTransformations.class));

            f = new File(tempFileDir + "TmpPDF" + UUID.randomUUID() + ".pdf");
            FileUtils.writeByteArrayToFile(f, gatewayResp.getPDFTransformationsReturn());

            // SCP the file to a server
            scpTransfer(request.getRemotehost(), request.getRemotefile(), f);

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFTransformations")));
            var out = new PDFTransformationsResponse();
            out.setStatusMsg("ok");
            out.setStatusVal(1);
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "PDFTransformations",
                                    ex.getMessage(),
                                    null)));
            var out = new PDFTransformationsResponse();
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return out;
        }
    }

    public boolean scpTransfer(String host, String dest, File payload) throws IOException {
        KeyProvider keyProvider = ssh.loadKeys(prvtKey, pubKey, null);
        ssh.authPublickey(sshUserName, keyProvider);
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
}

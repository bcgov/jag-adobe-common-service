package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.scp.PDFTransformations;
import ca.bc.gov.open.adobe.scp.PDFTransformations2;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse;
import ca.bc.gov.open.adobe.scp.PDFTransformationsResponse2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.net.InetAddress;
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

    @Value("${adobe.ssh.username}")
    private String sfegUserName = "";

    @Value("${adobe.ssh.host}")
    private String sfegHost = "";

    @Value("${adobe.ssh.nfs-dir}")
    private String nfsDir = "";

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    private static final String SOAP_NAMESPACE =
            "http://brooks/AdobeCommonServices.Source.CommonServices.ws.provider:PDFTransformationsSCPWS";

    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;
    private final String tempFileDir = "temp-pdfs/";
    private JSch jsch = null;

    private final ModelMapper mapper;

    @Autowired
    public PDFTransformSCPController(
            ObjectMapper objectMapper,
            @Qualifier("transformWS") WebServiceTemplate webServiceTemplate,
            ModelMapper mapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
        jsch = new JSch();
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
            sftpTransfer(request.getRemotefile(), f);

            // Return the good response
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "transformPDFScp")));
            var out = new PDFTransformationsResponse2();
            out.setStatusVal(1);
            out.setStatusMsg("ok");
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "transformPDFScp",
                                    ex.getMessage(),
                                    request)));
            var out = new PDFTransformationsResponse2();
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

            // SCP the file to a server
            sftpTransfer(request.getRemotefile(), f);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success", "pdfTransformSCPByReference")));
            var out = new PDFTransformationsResponse();
            out.setStatusMsg("ok");
            out.setStatusVal(1);
            return out;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "pdfTransformSCPByReference",
                                    ex.getMessage(),
                                    request)));
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

    public void sftpTransfer(String dest, File payload) throws JSchException {
        ChannelSftp channelSftp = null;
        jsch.setKnownHosts(".ssh/known_hosts");
        try {
            InetAddress address = InetAddress.getByName(sfegHost);
            jsch.addIdentity(".ssh/id_rsa");
            Session jschSession = jsch.getSession(sfegUserName, address.getHostAddress());
            jschSession.setConfig("StrictHostKeyChecking", "no");
            jschSession.connect();
            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        } catch (Exception ex) {
            log.error("Failed to connect to SFEG host: " + sfegHost);
            throw new JSchException(ex.getMessage());
        }

        try {
            log.info("src: " + payload.getAbsoluteFile().getPath());
            channelSftp.put(payload.getAbsoluteFile().getPath(), dest);
        } catch (Exception ex) {
            log.error(
                    "Failed to transfer file to remote: "
                            + sfegUserName
                            + "@"
                            + sfegHost
                            + ":"
                            + dest);
            throw new JSchException(ex.getMessage());
        } finally {
            channelSftp.exit();
            channelSftp.disconnect();
        }
    }
}

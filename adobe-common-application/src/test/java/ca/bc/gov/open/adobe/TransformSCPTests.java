package ca.bc.gov.open.adobe;

import static org.mockito.Mockito.*;

import ca.bc.gov.open.adobe.controllers.PDFTransformSCPController;
import ca.bc.gov.open.adobe.exceptions.AdobeLCGException;
import ca.bc.gov.open.adobe.scp.PDFTransformations;
import ca.bc.gov.open.adobe.scp.PDFTransformations2;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.client.core.WebServiceTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TransformSCPTests {

    @Autowired private ObjectMapper objectMapper;

    @Autowired private SftpProperties sftpProperties;

    @Mock private WebServiceTemplate webServiceTemplate;

    @Mock private PDFTransformSCPController controller;

    @Test
    public void transformPDFScpSuccessTest() throws IOException {
        var req = new PDFTransformations2();
        req.setFlags(1);
        req.setRemotefile("A");
        req.setInputFile("A");
        req.setRemotehost("A");

        controller =
                new PDFTransformSCPController(
                        objectMapper, webServiceTemplate, new ModelMapper(), sftpProperties);
        controller = spy(controller);

        byte[] a = "AAAAAAAAAA".getBytes(StandardCharsets.UTF_8);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations2.class)))
                .thenReturn(a);

        var resp = controller.transformPDFScp(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFByReferenceScpSuccessTest() throws JsonProcessingException {
        var req = new PDFTransformations();
        req.setInputFileUrl("A");

        Object a = new Object();
        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenReturn(a);

        controller =
                new PDFTransformSCPController(
                        objectMapper, webServiceTemplate, new ModelMapper(), sftpProperties);
        var resp = controller.pdfTransformSCPByReference(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFByReferenceScpFailTest() throws JsonProcessingException {
        var req = new PDFTransformations();
        req.setInputFileUrl("A");

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenThrow(new AdobeLCGException());

        controller =
                new PDFTransformSCPController(
                        objectMapper, webServiceTemplate, new ModelMapper(), sftpProperties);
        var resp = controller.pdfTransformSCPByReference(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFScpFailTest() throws JsonProcessingException {
        var req = new PDFTransformations2();
        req.setFlags(1);
        req.setRemotefile("A");
        req.setInputFile("A");
        req.setRemotehost("A");

        controller =
                new PDFTransformSCPController(
                        objectMapper, webServiceTemplate, new ModelMapper(), sftpProperties);
        controller = spy(controller);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations2.class)))
                .thenThrow(new AdobeLCGException());

        var resp = controller.transformPDFScp(req);

        Assertions.assertNotNull(resp);
    }
}

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
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.ws.client.core.WebServiceTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransformSCPTests {

    @InjectMocks private ObjectMapper objectMapper;

    @InjectMocks private SftpProperties sftpProperties;

    @Mock private WebServiceTemplate webServiceTemplate;

    @Mock private PDFTransformSCPController controller;

    // All pseudo values
    public static final String HOST = "host";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    @Mock public Session sessionMock;

    @Mock public JSch jSchMock;

    @BeforeEach
    public void setUp() throws JSchException {

        MockitoAnnotations.openMocks(this);

        SftpProperties sftpProperties = new SftpProperties();
        sftpProperties.setHost(HOST);
        sftpProperties.setUsername(USERNAME);
        sftpProperties.setPassword(PASSWORD);

        Mockito.doNothing().when(sessionMock).connect();
        Mockito.doNothing().when(sessionMock).disconnect();
        Mockito.when(sessionMock.isConnected()).thenReturn(true);

        Mockito.when(sessionMock.getHost()).thenReturn(HOST);
        Mockito.when(sessionMock.getUserName()).thenReturn(USERNAME);
        Mockito.when(jSchMock.getSession(Mockito.eq(USERNAME), Mockito.eq(HOST)))
                .thenReturn(sessionMock);
    }

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

package ca.bc.gov.open.adobe;

import static org.mockito.Mockito.when;

import ca.bc.gov.open.adobe.controllers.PDFTransformWSController;
import ca.bc.gov.open.adobe.exceptions.AdobeLCGException;
import ca.bc.gov.open.adobe.ws.PDFTransformations;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.client.core.WebServiceTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TransformWSTests {

    @InjectMocks private ObjectMapper objectMapper;

    @Mock private WebServiceTemplate webServiceTemplate;

    @Mock private PDFTransformWSController controller;

    @Test
    public void transformPDFWSSuccessTest() throws JsonProcessingException {

        ca.bc.gov.open.adobe.gateway.PDFTransformations req =
                new ca.bc.gov.open.adobe.gateway.PDFTransformations();
        req.setInputFile("AAA");
        req.setFlags(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenReturn("AAAAA");

        controller = new PDFTransformWSController(objectMapper, webServiceTemplate);

        var resp = controller.transformPDFWS(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFByReferenceWSSuccessTest() throws JsonProcessingException {
        PDFTransformations req = new PDFTransformations();
        req.setInputFile("AAAAA");
        req.setFlags(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenReturn("AAAAA");

        controller = new PDFTransformWSController(objectMapper, webServiceTemplate);

        var resp = controller.transformPDFByReference(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFWSFailTest() throws JsonProcessingException {

        ca.bc.gov.open.adobe.gateway.PDFTransformations req =
                new ca.bc.gov.open.adobe.gateway.PDFTransformations();
        req.setInputFile("AAA");
        req.setFlags(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenThrow(new AdobeLCGException());

        controller = new PDFTransformWSController(objectMapper, webServiceTemplate);

        var resp = controller.transformPDFWS(req);

        Assertions.assertNotNull(resp);
    }

    @Test
    public void transformPDFByReferenceWSFailTest() throws JsonProcessingException {
        PDFTransformations req = new PDFTransformations();
        req.setInputFile("AAAAA");
        req.setFlags(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFTransformations.class)))
                .thenThrow(new AdobeLCGException());

        controller = new PDFTransformWSController(objectMapper, webServiceTemplate);

        var resp = controller.transformPDFByReference(req);

        Assertions.assertNotNull(resp);
    }
}

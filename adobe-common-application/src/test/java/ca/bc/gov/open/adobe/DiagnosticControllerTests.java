package ca.bc.gov.open.adobe;

import static org.mockito.Mockito.when;

import ca.bc.gov.open.adobe.controllers.PDFDiagnosticController;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnostics;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReference;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsByReferenceResponse;
import ca.bc.gov.open.adobe.diagnostic.PDFDiagnosticsResponse;
import ca.bc.gov.open.adobe.exceptions.AdobeLCGException;
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
public class DiagnosticControllerTests {

    @InjectMocks private ObjectMapper objectMapper;

    @Mock private WebServiceTemplate webServiceTemplate;

    @Mock private PDFDiagnosticController controller;

    @Test
    public void getPDFDiagnosticSuccessTest() throws JsonProcessingException {
        var req = new PDFDiagnostics();
        req.setInputFile("AAA");

        var response = new PDFDiagnosticsResponse();
        response.setPDFDiagnosticsReturn(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFDiagnostics.class)))
                .thenReturn(response);

        controller = new PDFDiagnosticController(objectMapper, webServiceTemplate);
        var resp = controller.getPDFDiagnostic(req);

        Assertions.assertSame(response, resp);
    }

    @Test
    public void getPDFDiagnosticByReferenceSuccessTest() throws JsonProcessingException {
        var req = new PDFDiagnosticsByReference();
        req.setInputFileUrl("AAA");

        var response = new PDFDiagnosticsByReferenceResponse();
        response.setPDFDiagnosticsByReferenceReturn(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFDiagnosticsByReference.class)))
                .thenReturn(response);

        controller = new PDFDiagnosticController(objectMapper, webServiceTemplate);
        var resp = controller.getPDFDiagnosticByReference(req);

        Assertions.assertSame(response, resp);
    }

    @Test
    public void getPDFDiagnosticThenFailTest() throws JsonProcessingException {
        var response = new PDFDiagnosticsResponse();
        response.setPDFDiagnosticsReturn(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFDiagnostics.class)))
                .thenThrow(new AdobeLCGException());

        controller = new PDFDiagnosticController(objectMapper, webServiceTemplate);

        var resp = controller.getPDFDiagnostic(new PDFDiagnostics());

        Assertions.assertNotNull(resp);
        Assertions.assertNotEquals(response, resp);
        Assertions.assertEquals(resp.getPDFDiagnosticsReturn(), null);
    }

    @Test
    public void getPDFDiagnosticByReferenceThenFailTest() throws JsonProcessingException {
        var response = new PDFDiagnosticsByReferenceResponse();
        response.setPDFDiagnosticsByReferenceReturn(1);

        when(webServiceTemplate.marshalSendAndReceive(
                        Mockito.anyString(), Mockito.any(PDFDiagnosticsByReference.class)))
                .thenThrow(new AdobeLCGException());

        controller = new PDFDiagnosticController(objectMapper, webServiceTemplate);

        var resp = controller.getPDFDiagnosticByReference(new PDFDiagnosticsByReference());

        Assertions.assertNotNull(resp);
        Assertions.assertNotEquals(response, resp);
        Assertions.assertEquals(resp.getPDFDiagnosticsByReferenceReturn(), null);
    }
}

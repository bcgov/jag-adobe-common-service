package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.exceptions.ServiceException;
import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.models.ServletErrorLog;
import ca.bc.gov.open.adobe.models.TransformationServletRequest;
import ca.bc.gov.open.adobe.ws.PDFTransformationsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.XfaForm;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

@Slf4j
@RestController
@WebServlet("/")
public class TransformationServletController extends HttpServlet {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final WebServiceTemplate webServiceTemplate;

    private static Integer MAX_OPTIONS = 111;

    @Value("${adobe.lifecycle-host}")
    private String host = "https://127.0.0.1/";

    @Autowired PDFTransformWSController pdfTransformWSController;

    @Autowired
    public TransformationServletController(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("transformWS") WebServiceTemplate webServiceTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.webServiceTemplate = webServiceTemplate;
    }

    @GetMapping(value = "transformationServlet", produces = MediaType.TEXT_XML_VALUE)
    public void transformService(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            TransformationServletRequest servletRequest,
            HttpServletResponse response)
            throws IOException {
        if (!isValidOptions(servletRequest.getOptions())) {
            String errMsg =
                    "Transformation options must be a summation of allowable values: 1, 2, 4, 8, 32 or 64.";
            log.error(objectMapper.writeValueAsString(new ServletErrorLog(errMsg, servletRequest)));
            throw new ServiceException(errMsg);
        }

        // Fetch file
        ResponseEntity<byte[]> resp = null;
        LocalDateTime startTime = LocalDateTime.now();
        try {
            resp =
                    restTemplate.exchange(
                            servletRequest.getUrl(),
                            HttpMethod.GET,
                            new HttpEntity<>(new HttpHeaders()),
                            byte[].class);
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new ServletErrorLog(ex.getMessage(), servletRequest)));
            return;
        }

        if (resp.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            String errMsg =
                    "File defined for URL parameter not found (HTTP Status "
                            + resp.getStatusCode()
                            + " returned from server).";
            log.error(objectMapper.writeValueAsString(new ServletErrorLog(errMsg, servletRequest)));
            return;
        }

        PdfReader reader = null;
        XfaForm xfa = null;
        try {
            // ignore the presence of an owner password. It will only throw an exception if a user
            // password is in place
            PdfReader.unethicalreading = true;
            reader = new PdfReader(resp.getBody());
            AcroFields form = reader.getAcroFields();
            xfa = form.getXfa();
            reader.close();
        } catch (Exception ie) {

            if (reader != null) reader.close();

            OutputStream os = response.getOutputStream();
            response.setContentLength(resp.getBody().length);
            os.write(resp.getBody());
            os.flush();
            os.close();
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success",
                                    "transformationServlet (Not a PDF file or exception when reading a file)")));
            LogGetDocumentPerformance(startTime, correlationId);
            return;
        }

        // Setting content type
        response.setContentType("application/pdf");
        if (xfa == null || !xfa.isXfaPresent()) {
            OutputStream os = response.getOutputStream();
            response.setContentLength(resp.getBody().length);
            os.write(resp.getBody());
            os.flush();
            os.close();
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success",
                                    "transformationServlet (Not need to be flattened on LiveCycleGateway)")));
            LogGetDocumentPerformance(startTime, correlationId);
            return;
        }

        String bs64 = Base64Utils.encodeToString(resp.getBody());
        ca.bc.gov.open.adobe.gateway.PDFTransformations request =
                new ca.bc.gov.open.adobe.gateway.PDFTransformations();
        request.setFlags(Integer.valueOf(servletRequest.getOptions()));
        request.setInputFile(bs64);
        // For efficiency concern,
        // PDFTransformWSController.transformPDFWS call is replace with as such:
        PDFTransformationsResponse out = new PDFTransformationsResponse();
        ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse pdfTransformationsResponse = null;
        try {
            LocalDateTime transformationStartTime = LocalDateTime.now();
            pdfTransformationsResponse =
                    (ca.bc.gov.open.adobe.gateway.PDFTransformationsResponse)
                            webServiceTemplate.marshalSendAndReceive(
                                    host,
                                    request,
                                    webServiceMessage -> {
                                        SaajSoapMessage soapMessage =
                                                (SaajSoapMessage) webServiceMessage;
                                        MimeHeaders mimeHeader =
                                                soapMessage.getSaajMessage().getMimeHeaders();
                                        mimeHeader.setHeader("x-correlation-id", correlationId);
                                    });
            out.setStatusVal(1);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "PDFTransformations")));
            LogTransformationPerformance(transformationStartTime, correlationId);
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failed to send message to adobe LCG",
                                    "PDFTransformations",
                                    ex.getMessage(),
                                    request)));
            out.setStatusVal(0);
            out.setStatusMsg(ex.getMessage());
            return;
        }

        // Watch for LCG error
        if (out.getStatusVal().equals("0")) {
            log.error(
                    objectMapper.writeValueAsString(
                            new ServletErrorLog(out.getStatusMsg(), servletRequest)));
            return;
        }
        response.setContentLength(pdfTransformationsResponse.getPDFTransformationsReturn().length);
        OutputStream os = response.getOutputStream();
        os.write(pdfTransformationsResponse.getPDFTransformationsReturn());
        os.flush();
        os.close();
        LogGetDocumentPerformance(startTime, correlationId);
        log.info(
                objectMapper.writeValueAsString(
                        new RequestSuccessLog("Request Success", "transformationServlet")));
    }

    // options : Input string sum of binary options.
    // return  : Either boolean true or false if options are valid.
    public static final boolean isValidOptions(String options) throws ServiceException {
        //  For example
        // ----------------
        //	options = 32			0010 0000
        //	maxOptions = 47			0010 1111
        //	value =  true			---------
        //							0010 0000  true
        //
        //	options = 16			0001 0000
        //	maxOptions = 47         0010 1111
        //	value = false           ---------
        //							0000 0000 false

        // input value test
        try {
            Integer.parseInt(options);
        } catch (NumberFormatException e) {
            return false;
        }

        // AND test
        if ((MAX_OPTIONS & Integer.parseInt(options)) != Integer.parseInt(options)) {
            return false;
        } else {
            return true;
        }
    }

    private static void LogTransformationPerformance(LocalDateTime start, String correlationId) {
        if (correlationId != null) {
            Duration duration = Duration.between(start, LocalDateTime.now());
            log.info(
                    "GetDocument Transformation Performance - Duration:"
                            + duration.toMillis() / 1000.0
                            + " CorrelationId:"
                            + correlationId);
        }
    }

    private static void LogGetDocumentPerformance(LocalDateTime start, String correlationId) {
        if (correlationId != null) {
            Duration duration = Duration.between(start, LocalDateTime.now());
            log.info(
                    "GetDocument Performance - Duration:"
                            + duration.toMillis() / 1000.0
                            + " CorrelationId:"
                            + correlationId);
        }
    }
}

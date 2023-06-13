package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.exceptions.ServiceException;
import ca.bc.gov.open.adobe.models.OrdsErrorLog;
import ca.bc.gov.open.adobe.models.RequestSuccessLog;
import ca.bc.gov.open.adobe.models.ServletErrorLog;
import ca.bc.gov.open.adobe.models.TransformationServletRequest;
import ca.bc.gov.open.adobe.ws.PDFTransformationsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        String version = getPDFVersion(resp.getBody());
        // Setting content type
        response.setContentType("application/pdf");
        if (version.equals("1.4")
                || version.equals("1.3")
                || version.equals("1.2")
                || version.equals("1.1")
                || version.equals("1.0")
                || version.equals("0.0")) {
            OutputStream os = response.getOutputStream();
            response.setContentLength(resp.getBody().length);
            os.write(resp.getBody());
            os.flush();
            os.close();
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success",
                                    "transformationServlet (Version less than PDF 1.5)")));
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

    public static final String getPDFVersion(byte[] fileBytes) throws ServiceException {
        int MIN_LENGTH = 10;
        int VERSION_BUFFER = 4;

        if (null == fileBytes || fileBytes.length <= MIN_LENGTH) {
            return "0.0";
        } else {
            String retval = "0.0";
            byte[] b = new byte[(int) MIN_LENGTH];
            InputStream is = new ByteArrayInputStream(fileBytes);
            try {
                is.read(b);

                // starts with %PDF. If this is not found, all bets are off.
                if (b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46) {
                    // Get version - this has to return a string as we don't know
                    // how Adobe will deal with later versions of PDF greater than 1.9
                    // They may use 2.0 or 1.10. 1.10 converts to double 1.1 which is not
                    // the correct version.
                    byte[] buff = new byte[VERSION_BUFFER];
                    System.arraycopy(b, 5, buff, 0, 4);
                    retval = new String(buff).trim(); // 0x10 may trail value. trim it.
                }
            } catch (IOException ie) {
                // do nothing.
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return retval;
            }
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

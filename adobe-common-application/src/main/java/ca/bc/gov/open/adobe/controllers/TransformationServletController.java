package ca.bc.gov.open.adobe.controllers;

import ca.bc.gov.open.adobe.exceptions.ServiceException;
import ca.bc.gov.open.adobe.models.ServletErrorLog;
import ca.bc.gov.open.adobe.models.TransformationServletRequest;
import ca.bc.gov.open.adobe.ws.PDFTransformationsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/")
public class TransformationServletController extends HttpServlet {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static Integer MAX_OPTIONS = 111;

    @Autowired PDFTransformWSController pdfTransformWSController;

    @Autowired
    public TransformationServletController(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "transformationServlet", produces = MediaType.TEXT_XML_VALUE)
    public byte[] transformService(TransformationServletRequest servletRequest)
            throws JsonProcessingException {
        if (!isValidOptions(servletRequest.getOptions())) {
            String errMsg =
                    "Transformation options must be a summation of allowable values: 1, 2, 4, 8, 32 or 64.";
            log.error(objectMapper.writeValueAsString(new ServletErrorLog(errMsg, servletRequest)));
            throw new ServiceException(errMsg);
        }

        ResponseEntity<byte[]> resp = null;
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
            throw new ServiceException(ex.getMessage());
        }

        if (resp.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            String errMsg =
                    "File defined for URL parameter not found (HTTP Status "
                            + resp.getStatusCode()
                            + " returned from server).";
            log.error(objectMapper.writeValueAsString(new ServletErrorLog(errMsg, servletRequest)));
            throw new ServiceException(errMsg);
        }


        String version = getPDFVersion(resp.getBody());

        if (version.equals("1.4")
                || version.equals("1.3")
                || version.equals("1.2")
                || version.equals("1.1")
                || version.equals("1.0")
                || version.equals("0.0")) {
            return resp.getBody();
        }

        String bs64 = resp.getBody() != null ? Base64Utils.encodeToString(resp.getBody()) : "";

        ca.bc.gov.open.adobe.gateway.PDFTransformations request =
                new ca.bc.gov.open.adobe.gateway.PDFTransformations();
        request.setFlags(Integer.valueOf(servletRequest.getOptions()));
        request.setInputFile(bs64);
        PDFTransformationsResponse pdfTransformationsResponse =
                pdfTransformWSController.transformPDFWS(request);

        if (pdfTransformationsResponse.getStatusVal().equals("0")) {
            log.error(
                    objectMapper.writeValueAsString(
                            new ServletErrorLog(
                                    pdfTransformationsResponse.getStatusMsg(), servletRequest)));
            throw new ServiceException(pdfTransformationsResponse.getStatusMsg());
        }
        return pdfTransformationsResponse.getOutputFile().getBytes(StandardCharsets.UTF_8);
    }

    // options 		: Input string sum of binary options.
    // return		: Either boolean true or false if options are valid.
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
}

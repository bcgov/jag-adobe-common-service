package ca.bc.gov.open.adobe.models;

import lombok.Data;

@Data
public class TransformationServletRequest {
    private String options;
    private String url;
    private String document_format;
    private String policy;
}

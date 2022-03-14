package ca.bc.gov.open.adobe.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequestSuccessLog {
    private String type;
    private String endpoint;
}

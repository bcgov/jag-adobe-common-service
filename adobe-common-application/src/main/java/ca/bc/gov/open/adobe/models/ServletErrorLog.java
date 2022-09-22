package ca.bc.gov.open.adobe.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServletErrorLog {
    private String message;
    private Object request;
}

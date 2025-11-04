package com.dbschema.mongo.oidc;

public class OidcResponse {
    private String code;
    private String state;
    private String error;
    private String errorDescription;

    public String getCode() {
        return code;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (code != null) {
            sb.append("Code: ").append(code).append("\n");
        }
        if (state != null) {
            sb.append("State: ").append(state).append("\n");
        }
        if (error != null) {
            sb.append("Error: ").append(error).append("\n");
        }
        if (errorDescription != null) {
            sb.append("Error Description: ").append(errorDescription).append("\n");
        }
        return sb.toString();
    }
}

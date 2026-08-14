package com.dbschema.mongo.oidc;

public final class OidcResponse {
  private final String code;
  private final String state;
  private final String error;
  private final String errorDescription;

  private OidcResponse(String code, String state, String error, String errorDescription) {
    this.code = code;
    this.state = state;
    this.error = error;
    this.errorDescription = errorDescription;
  }

  public static OidcResponse success(String code, String state) {
    return new OidcResponse(code, state, null, null);
  }

  /**
   * The state is carried on an error too: a provider echoes it back on the error redirect as well
   * (RFC 6749 section 4.1.2.1), and it is what tells the error of the login in progress apart from an
   * unsolicited request to the callback port.
   */
  public static OidcResponse error(String error, String errorDescription, String state) {
    return new OidcResponse(null, state, error, errorDescription);
  }

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

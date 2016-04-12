package com.dbschema.mongo.parser;

public class JsonParseException extends RuntimeException {

    final String jsonString;
    final int pos;

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if ( pos > -1 && pos < jsonString.length() ){
            sb.append("Wrong formatted Json : ");
            sb.append( jsonString.substring(0, pos)).append("???").append(jsonString.substring(pos));
        }  else {
            sb.append("Wrong formatted Json : ");
            sb.append( jsonString );
        }
        /*
        sb.append(jsonString);
        sb.append("\n");
        for (int i = 0; i < pos; i++) {
            sb.append(" ");
        }
        sb.append("^");
        */
        return sb.toString();
    }

    /**
     * Creates a new instance.
     *
     * @param jsonString the JSON being parsed
     * @param position   the position of the failure
     */
    public JsonParseException(final String jsonString, final int position) {
        this.jsonString = jsonString;
        this.pos = position;
    }

    /**
     * Creates a new instance.
     *
     * @param jsonString the JSON being parsed
     * @param position   the position of the failure
     * @param cause      the root cause
     */
    public JsonParseException(final String jsonString, final int position, final Throwable cause) {
        super(cause);
        this.jsonString = jsonString;
        this.pos = position;
    }
}
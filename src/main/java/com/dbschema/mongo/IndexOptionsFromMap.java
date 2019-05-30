package com.dbschema.mongo;

import com.mongodb.client.model.IndexOptions;

import java.security.InvalidParameterException;
import java.util.Map;

public class IndexOptionsFromMap extends IndexOptions {

    public IndexOptionsFromMap( Map map ){
        for ( Object key : map.keySet() ){
            Object value = map.get( key );
            if ( "background".equals( key )){
                background( ( value instanceof Boolean && ((Boolean) value).booleanValue() ) || "1".equals(value));
            } else if ( "bits".equals( key )){
                bits( asInt(key, value));
            } else if ( "bucketSize".equals( key )){
                bucketSize(asDouble(key, value));
            } else if ( "defaultLanguage".equals( key )){
                defaultLanguage( (String)value );
            } else if ( "languageOverride".equals( key )){
                languageOverride((String) value);
            } else if ( "name".equals( key )){
                name((String) value);
            } else if ( "sparse".equals( key )){
                sparse(asBoolean(key, value));
            } else if ( "unique".equals( key )){
                unique(asBoolean(key, value));
            } else if ( "version".equals( key )){
                version(asInt(key, value));
            } else if ( "textVersion".equals( key )){
                textVersion(asInt(key, value));
            } else {
                throw new InvalidParameterException("Invalid key '" + key + "'. Please inform DbSchema developers, the driver may miss a mapping for this parameter.");
            }
        }

    }


    private int asInt( Object key, Object value ){
        if ( value instanceof Integer ) return ((Integer)value).intValue();
        if ( value instanceof String ){
            try {
                return Integer.parseInt( (String)value );
            } catch ( NumberFormatException ex ){
                throw new NumberFormatException("Expected integer value for '" + key + "'. Got " + value );
            }
        }
        throw new NumberFormatException("Expected Integer for key '" + key + "'. Got " + value + " as " + value.getClass() );
    }

    private boolean asBoolean( Object key, Object value ){
        if ( value instanceof Boolean ) return ((Boolean)value).booleanValue();
        if ( value instanceof String ){
            return "1".equals( value ) || "true".equalsIgnoreCase( (String)value );
        }
        throw new NumberFormatException("Expected Boolean for key '" + key + "'. Got " + value + " as " + value.getClass() );
    }

    private double asDouble( Object key, Object value ){
        if ( value instanceof Number ) return ((Number)value).doubleValue();
        if ( value instanceof String ){
            try {
                return Double.parseDouble((String) value);
            } catch ( NumberFormatException ex ){
                throw new NumberFormatException("Expected double value for '" + key + "'. Got " + value );
            }
        }
        throw new NumberFormatException("Expected Double for key '" + key + "'. Got " + value + " as " + value.getClass() );
    }



}

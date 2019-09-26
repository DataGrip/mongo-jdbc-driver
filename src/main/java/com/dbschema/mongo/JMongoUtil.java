package com.dbschema.mongo;

import com.dbschema.mongo.parser.JsonLoaderCallback;
import com.dbschema.mongo.parser.JsonParseException;
import com.dbschema.mongo.parser.JsonParser;
import org.bson.Document;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JMongoUtil {


    public static Document parse( String text ){
        Thread.dumpStack();
        if ( text != null && text.trim().length() > 0 && !text.trim().startsWith("{")){
            throw new JsonParseException( "Json should start with '{'. Json text : " + text, 0 );
        }
        JsonLoaderCallback callback = new JsonLoaderCallback();
        JsonParser.parse( text, callback );
        return new Document(callback.map);
    }

    /*
    try {
            return Document.parse(text );
        } catch ( JsonParseException ex ){
            StringBuilder sb = new StringBuilder();
            String msg = ex.getLocalizedMessage();
            if ( msg != null ){
                if ( msg.endsWith(".")) msg = msg.substring(0, msg.length()-1);
                sb.append( msg );
            }
            sb.append( " in string " ).append( text);

            throw new JsonParseException( sb.toString() );
        }
     */



    public static Map doConversions(Map map){
        for (Object key : map.keySet()){
            Object value = map.get( key );
            if ( value instanceof Map ){
                doConversions((Map) value);
            }
            if ( value instanceof Map && canConvertMapToArray( (Map)value )){
                map.put( key, convertMapToArray((Map) value));
            }
        }
        return map;
    }


    private static boolean canConvertMapToArray( Map map ) {
        boolean isArray = true;
        for (int i = 0; i < map.size(); i++) {
            if (!map.containsKey("" + i)) isArray = false;
        }
        return isArray;
    }

    private static List convertMapToArray( Map map ) {
            ArrayList array = new ArrayList();
            for ( int i = 0; i < map.size(); i++ ){
                array.add(map.get("" + i));
            }
            return array;
    }

    static String nullize(String text) {
        return text == null || text.isEmpty() ? null : text;
    }

    @NotNull
    public static String escapeChars(@NotNull final String str, final char... character) {
        final StringBuilder buf = new StringBuilder(str);
        for (char c : character) {
            escapeChar(buf, c);
        }
        return buf.toString();
    }

    public static void escapeChar(@NotNull final StringBuilder buf, final char character) {
        int idx = 0;
        while ((idx = indexOf(buf, character, idx)) >= 0) {
            buf.insert(idx, "\\");
            idx += 2;
        }
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence s, char c, int start) {
        return indexOf(s, c, start, s.length());
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence s, char c, int start, int end) {
        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (s.charAt(i) == c) return i;
        }
        return -1;
    }
}

package com.nosql.mongo.parser;


import java.util.*;

public class JsonLoaderCallback implements JsonCallback {

    public final Map<String,Object> map = new LinkedHashMap<String, Object>();
    private final List<Object> path = new ArrayList<Object>();

    private Object getCurrent(){
        return path.get( path.size() - 1 );
    }

    private void add( String key, Object value ){
        if ( getCurrent() instanceof Map ) ((Map)getCurrent()).put( key, value );
        else if ( getCurrent() instanceof List ) ((List)getCurrent()).add(value);
    }

    public JsonLoaderCallback(){
        path.add( map );
    }

    @Override
    public void objectStart() {
    }

    @Override
    public void objectStart(String name) {
        Map<String,Object> child = new LinkedHashMap<String, Object>();
        add( name, child );
        path.add( child );
    }

    @Override
    public Object objectDone() {
        if ( path.size() > 1 ) {
            Object ret = path.get(path.size() - 1);
            path.remove( path.size() - 1 );
            return ret;
        }
        return null;
    }

    @Override
    public void reset() {
        map.clear();
        path.clear();
        path.add( map );
    }

    @Override
    public Object get() {
        return path.get( path.size() - 1 );
    }

    @Override
    public JsonCallback createBSONCallback() {
        return null;
    }

    @Override
    public void arrayStart() {
    }

    @Override
    public void arrayStart(String name) {
        final List child = new ArrayList();
        add( name, child );
        path.add(child);
    }

    @Override
    public Object arrayDone() {
        return objectDone();
    }

    @Override
    public void gotNull(String name) {
    }

    @Override
    public void gotUndefined(String name) {
    }

    @Override
    public void gotMinKey(String name) {
    }

    @Override
    public void gotMaxKey(String name) {
    }

    @Override
    public void gotBoolean(String name, boolean value) {
        add( name, value );
    }

    @Override
    public void gotDouble(String name, double value) {
        add(name, value);
    }

    @Override
    public void gotInt(String name, int value) {
        add(name, value);
    }

    @Override
    public void gotLong(String name, long value) {
        add(name, value);
    }

    @Override
    public void gotDate(String name, long millis) {
        add(name, new Date(millis));
    }

    @Override
    public void gotString(String name, String value) {
        add(name, value);
    }

    @Override
    public void gotSymbol(String name, String value) {
        add(name, value);
    }

    @Override
    public void gotRegex(String name, String pattern, String flags) {
        add(name, pattern );
    }

    @Override
    public void gotTimestamp(String name, int time, int increment) {
        add(name, time );
    }

    @Override
    public void gotObjectId(String name, Object id) {
        add(name, id );
    }

    @Override
    public void gotDBRef(String name, String namespace, Object id) {
        add(name, id );
    }

    @Override
    public void gotBinaryArray(String name, byte[] data) {
        add(name, data );
    }

    @Override
    public void gotBinary(String name, byte type, byte[] data) {
        add(name, data );
    }

    @Override
    public void gotUUID(String name, long part1, long part2) {
        add(name, part1 );
    }

    @Override
    public void gotCode(String name, String code) {
        add(name, code );
    }

    @Override
    public void gotCodeWScope(String name, String code, Object scope) {
        add(name, code );
    }
}
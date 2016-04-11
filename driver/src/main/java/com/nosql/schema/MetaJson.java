package com.nosql.schema;

import java.util.ArrayList;
import java.util.List;

public class MetaJson extends MetaField {

    public static final int TYPE_MAP = 4999544;
    public static final int TYPE_LIST = 4999545;

    public final List<MetaField> fields = new ArrayList<MetaField>();

    public MetaJson(MetaJson parentJson, String name, int type ){
        super( parentJson, name, ( type == TYPE_LIST ? "list" : "map" ), type );
    }

    public MetaField createField(String name, String typeName, int type ){
        for ( MetaField column : fields){
            if ( column.name.equals( name )) return column;
        }
        MetaField column = new MetaField( this, name, typeName, type );
        fields.add( column );
        return column;
    }

    public MetaJson createJsonMapField(String name){
        for ( MetaField field : fields){
            if ( field instanceof MetaJson && field.name.equals( name )) return (MetaJson)field;
        }
        MetaJson json = new MetaJson( this, name, TYPE_MAP);
        fields.add( json );
        return json;
    }

    public MetaJson createJsonListField(String name){
        for ( MetaField field : fields){
            if ( field instanceof MetaJson && field.name.equals( name )) return (MetaJson)field;
        }
        MetaJson json = new MetaJson( this, name, TYPE_LIST);
        fields.add( json );
        return json;
    }

    public MetaField getColumn ( String name ){
        for ( MetaField column : fields){
            if ( column.name.equals( name ) ) return column;
        }
        return null;
    }

    @Override
    public void collectFieldsWithObjectId(List<MetaField> unsolvedFields) {
        super.collectFieldsWithObjectId(unsolvedFields);
        for ( MetaField field : fields ){
            field.collectFieldsWithObjectId(unsolvedFields);
        }
    }

    public MetaField findField( String name ){
        for ( MetaField other : fields ){
            if ( name.startsWith( other.getNameWithPath())){
                MetaField found = null;
                if ( other instanceof MetaJson){
                    found = ((MetaJson)other).findField(  name );
                }
                return found != null ? found : other;
            }
        }
        return null;
    }

}

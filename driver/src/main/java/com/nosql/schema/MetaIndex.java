package com.nosql.schema;


import java.util.ArrayList;
import java.util.List;

public class MetaIndex {

    public final MetaJson metaMap;
    public final String name;
    public final List<MetaField> metaFields = new ArrayList<MetaField>();
    public final boolean pk, unique;

    public MetaIndex(MetaJson metaMap, String name, boolean pk, boolean unique){
        this.metaMap = metaMap;
        this.name = name;
        this.pk = pk;
        this.unique = unique;
    }

    public void addColumn( MetaField metaField ){
        if ( metaField != null ){
            metaFields.add( metaField );
        }
    }
}

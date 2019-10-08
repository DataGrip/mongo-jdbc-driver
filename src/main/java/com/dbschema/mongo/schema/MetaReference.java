package com.dbschema.mongo.schema;


public class MetaReference {

  public final MetaField fromField;
  public final MetaCollection pkCollection;

  public MetaReference(MetaField fromField, MetaCollection pkCollection) {
    this.fromField = fromField;
    this.pkCollection = pkCollection;
  }

}

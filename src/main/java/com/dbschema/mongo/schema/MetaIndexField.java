package com.dbschema.mongo.schema;


public class MetaIndexField {

  public final MetaField metaField;
  public final int ascOrDesc;


  public MetaIndexField(final MetaField metaField, final int ascOrDesc) {
    this.metaField = metaField;
    this.ascOrDesc = ascOrDesc;
  }

  public String getNameWithPath() {
    return metaField.getNameWithPath();
  }

  @Override
  public String toString() {
    return metaField.toString() + ": " + ascOrDesc;
  }
}

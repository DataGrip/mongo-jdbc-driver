package com.dbschema.mongo.schema;


import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.dbschema.mongo.Util.escapeChars;

public class MetaField {

  public final MetaJson parentJson;
  public final String name, typeName;
  public final List<ObjectId> objectIds = new ArrayList<ObjectId>();
  public final int type;
  public boolean mandatory = true;


  public MetaField(final MetaJson parentJson, final String name, final String typeName, int type) {
    this.parentJson = parentJson;
    this.name = name;
    this.typeName = typeName;
    this.type = type;
  }

  public void addObjectId(ObjectId objectId) {
    if (objectIds.size() < 4) {
      objectIds.add(objectId);
    }
  }

  public String getNameWithPath() {
    String qualifier = parentJson != null && !(parentJson instanceof MetaCollection) ? parentJson.getNameWithPath() + "." : "";
    return qualifier + escapeChars(name, '\\', '.');
  }

  @Override
  public String toString() {
    return getNameWithPath();
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public boolean isMandatory() {
    return mandatory;
  }
}

package com.dbschema.mongo.nashorn;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.jetbrains.annotations.NotNull;

/**
 * @author Liudmila Kornilova
 **/
public class JSFunction {
  private final ScriptObjectMirror objectMirror;

  public JSFunction(ScriptObjectMirror objectMirror) {
    assert objectMirror.isFunction();
    this.objectMirror = objectMirror;
  }

  @NotNull
  public String getText() {
    return (String) objectMirror.getDefaultValue(String.class);
  }
}

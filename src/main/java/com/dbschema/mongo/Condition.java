package com.dbschema.mongo;

public interface Condition<T> {
  boolean value(T t);
}

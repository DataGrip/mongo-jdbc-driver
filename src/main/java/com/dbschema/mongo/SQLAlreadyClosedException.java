package com.dbschema.mongo;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class SQLAlreadyClosedException extends SQLException {
  public SQLAlreadyClosedException(@NotNull String name) {
    super(name + " has already been closed.");
  }
}

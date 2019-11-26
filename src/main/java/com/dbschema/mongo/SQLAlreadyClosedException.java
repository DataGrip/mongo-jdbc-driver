package com.dbschema.mongo;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * @author Liudmila Kornilova
 **/
public class SQLAlreadyClosedException extends SQLException {
  public SQLAlreadyClosedException(@NotNull String name) {
    super(name + " has already been closed.");
  }
}

package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.mongodb.mongosh.MongoShell;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface ShellHolder {
  @NotNull
  MongoShell getShell(@NotNull MongoConnection connection) throws SQLException;

  void recreateShell();

  void close();
}

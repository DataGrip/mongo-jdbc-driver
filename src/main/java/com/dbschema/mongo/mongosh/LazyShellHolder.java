package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.mongodb.mongosh.MongoShell;
import org.jetbrains.annotations.NotNull;

public class LazyShellHolder implements ShellHolder {
  private MongoShell shell;

  @Override
  @NotNull
  public synchronized MongoShell getShell(@NotNull MongoConnection connection) {
    if (shell == null) {
      // disable warning about not available runtime compilation
      System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
      shell = new MongoShell(connection.getService().getMongoClient(), null);
      shell.eval("use " + connection.getSchema());
    }
    return shell;
  }

  @Override
  public synchronized void recreateShell() {
    if (shell == null) return;
    shell.close();
    shell = null;
  }

  @Override
  public synchronized void close() {
    if (shell != null) shell.close();
  }
}

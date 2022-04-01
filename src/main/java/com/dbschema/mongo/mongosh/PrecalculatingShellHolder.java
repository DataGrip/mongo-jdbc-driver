package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.mongodb.mongosh.MongoShell;
import org.graalvm.polyglot.Engine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class PrecalculatingShellHolder implements ShellHolder {
  private final ExecutorService executorService;
  private final Engine engine;
  private @NotNull Future<MongoShell> shellFuture;
  private @Nullable MongoShell shell;

  public PrecalculatingShellHolder(@NotNull ExecutorService executorService, @Nullable Engine engine) {
    this.executorService = executorService;
    this.engine = engine;
    this.shellFuture = submitNewShell();
  }

  @NotNull
  private Future<MongoShell> submitNewShell() {
    return executorService.submit(() -> {
      // disable warning about not available runtime compilation
      System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
      MongoShell shell = new MongoShell(null, engine);
      shell.eval("'initial warm up'");
      return shell;
    });
  }

  @Override
  @NotNull
  public synchronized MongoShell getShell(@NotNull MongoConnection connection) throws SQLException {
    if (shell == null) {
      try {
        shell = shellFuture.get();
        shell.setClient(connection.getService().getMongoClient());
        shell.eval("use " + connection.getSchema());
      }
      catch (InterruptedException | ExecutionException e) {
        throw new SQLException(e);
      }
    }
    return shell;
  }

  @Override
  public synchronized void recreateShell() {
    if (shell == null) return;
    shell.close();
    shell = null;
    shellFuture = submitNewShell();
  }

  public synchronized void close() {
    try {
      if (!shellFuture.isDone()) {
        shellFuture.cancel(true);
      }
    }
    finally {
      if (shell != null) shell.close();
    }
  }
}

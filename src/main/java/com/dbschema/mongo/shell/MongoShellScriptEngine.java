package com.dbschema.mongo.shell;

import com.dbschema.mongo.MongoConnectionParameters;
import com.dbschema.mongo.ScriptEngine;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.ArrayList;
import java.util.List;

import static com.dbschema.mongo.Util.ok;

/**
 * @author Liudmila Kornilova
 **/
public class MongoShellScriptEngine implements ScriptEngine {
  private final MongoConnectionParameters parameters;

  public MongoShellScriptEngine(@NotNull MongoConnectionParameters parameters) {
    this.parameters = parameters;
  }

  private String eval(@NotNull String command) throws SQLInvalidAuthorizationSpecException {
    Runtime runtime = Runtime.getRuntime();
    List<String> args = new ArrayList<>();
    args.add("mongo");
    if (parameters.database != null) args.add(parameters.database);
    args.add("--authenticationDatabase");
    args.add(parameters.authSource);
    if (parameters.mechanism != null) {
      args.add("--authenticationMechanism");
      args.add(parameters.mechanism.getMechanismName());
    }
    if (parameters.username != null) {
      args.add("--username");
      args.add(parameters.username);
    }
    if (parameters.password != null) {
      args.add("--password");
      args.add(new String(parameters.password));
    }
    args.add("--quiet");
    args.add("--eval");
    args.add(command);
    try {
      Process process = runtime.exec(args.toArray(new String[0]));
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        sb.append(line).append("\n");
        line = reader.readLine();
      }
      return sb.toString();
    }
    catch (IOException e) {
      throw new SQLInvalidAuthorizationSpecException(e.getMessage(), e);
    }
  }

  @Override
  @Nullable
  public synchronized ResultSet execute(@NotNull String query, int fetchSize) throws SQLException {
    String result = eval(query);
    if (result.isEmpty()) return ok(result);
    try {
      Document doc = Document.parse(result);
      return ok(doc);
    }
    catch (JsonParseException | BsonInvalidOperationException ignored) {
      return ok(result);
    }
  }
}

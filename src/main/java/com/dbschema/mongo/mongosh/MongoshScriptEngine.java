package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoScriptEngine;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.mongodb.mongosh.MongoShell;
import com.mongodb.mongosh.result.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.ok;

/**
 * @author Liudmila Kornilova
 **/
public class MongoshScriptEngine implements MongoScriptEngine {
  private static final Pattern CLEAR_CONTEXT = Pattern.compile("clearContext\\s*\\(\\s*\\);?");
  private final MongoConnection connection;
  private MongoShell repl;

  public MongoshScriptEngine(@NotNull MongoConnection connection) {
    this.connection = connection;
  }

  private MongoShell getRepl() {
    if (repl == null) {
      repl = new MongoShell(connection.getService().getMongoClient());
      repl.eval("use " + connection.getSchema());
    }
    return repl;
  }

  @Nullable
  @Override
  public ResultSet execute(@NotNull String query, int fetchSize) throws SQLException {
    try {
      if (CLEAR_CONTEXT.matcher(query.trim()).matches()) {
        repl = null;
        return null;
      }
      MongoShell repl = getRepl();
      MongoShellResult<?> result = repl.eval(query);
      if (result instanceof CursorResult) {
        Cursor<?> cursor = ((CursorResult<?>) result).getValue();
        if (fetchSize > 1 && cursor instanceof FindCursor) {
          ((FindCursor<?>) cursor).batchSize(fetchSize);
        }
        return new ResultSetIterator(cursor);
      }
      MongoShellResult<?> db = getRepl().eval("db");
      if (db instanceof StringResult) connection.setSchema(((StringResult) db).getValue());
      return result instanceof VoidResult || result instanceof BulkWriteResult || result instanceof InsertOneResult ||
                 result instanceof InsertManyResult || result instanceof MongoShellUpdateResult
             ? null
             : ok(result.getValue());
    }
    catch (Exception e) {
      throw new SQLException(e);
    }
  }
}

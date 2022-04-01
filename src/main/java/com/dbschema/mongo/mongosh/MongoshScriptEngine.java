package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoScriptEngine;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.mongodb.mongosh.MongoShell;
import com.mongodb.mongosh.result.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbschema.mongo.Util.ok;
import static com.dbschema.mongo.Util.trimEnd;

/**
 * @author Liudmila Kornilova
 **/
public class MongoshScriptEngine implements MongoScriptEngine {
  private static final Pattern USE_DATABASE = Pattern.compile("use\\s+(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern CLEAR_CONTEXT = Pattern.compile("clearContext\\s*\\(\\s*\\)\\s*;?");
  private final MongoConnection connection;
  private final ShellHolder shellHolder;

  public MongoshScriptEngine(@NotNull MongoConnection connection, @NotNull ShellHolder holder) {
    this.connection = connection;
    shellHolder = holder;
  }

  @Nullable
  @Override
  public ResultSet execute(@Language("js") @NotNull String query, int fetchSize) throws SQLException {
    try {
      if (CLEAR_CONTEXT.matcher(query.trim()).matches()) {
        shellHolder.recreateShell();
        return null;
      }
      Matcher useCommand = USE_DATABASE.matcher(trimEnd(query.trim(), ';').trim());
      if (useCommand.matches()) {
        String db = useCommand.group(1);
        if ((db.startsWith("\"") && db.endsWith("\"")) || (db.startsWith("'") && db.endsWith("'"))) {
          db = db.substring(1, db.length() - 1);
        }
        query = "use " + db.trim();
      }
      MongoShell repl = shellHolder.getShell(connection);
      MongoShellResult<?> result = repl.eval(query);
      if (result instanceof CursorResult) {
        Cursor<?> cursor = ((CursorResult<?>) result).getValue();
        if (fetchSize > 1 && cursor instanceof FindCursor) {
          ((FindCursor<?>) cursor).batchSize(fetchSize);
        }
        return new ResultSetIterator(cursor);
      }
      MongoShellResult<?> db = shellHolder.getShell(connection).eval("db");
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

  @Override
  public void close() {
    shellHolder.close();
  }
}

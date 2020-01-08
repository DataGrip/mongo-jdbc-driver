package com.dbschema.mongo.mongosh;

import com.dbschema.mongo.MongoConnection;
import com.dbschema.mongo.MongoScriptEngine;
import com.dbschema.mongo.resultSet.ResultSetIterator;
import com.github.korniloval.mongojshell.*;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import static com.dbschema.mongo.Util.ok;

/**
 * @author Liudmila Kornilova
 **/
public class MongoshScriptEngine implements MongoScriptEngine {
  private final MongoConnection connection;
  private MongoRepl repl;

  public MongoshScriptEngine(@NotNull MongoConnection connection) {
    this.connection = connection;
  }

  private MongoRepl getRepl() {
    if (repl == null) {
      repl = new MongoRepl(connection.getService().getMongoClient());
    }
    return repl;
  }

  @Nullable
  @Override
  public ResultSet execute(@NotNull String query, int fetchSize) throws SQLException {
    MongoRepl repl = getRepl();
    try {
      Result result = repl.eval(query).get();
      if (result instanceof CursorResult) {
        Cursor cursor = ((CursorResult) result).getValue();
        if (fetchSize > 1) {
          cursor.batchSize(fetchSize);
        }
        return new ResultSetIterator(cursor);
      }
      return result instanceof BooleanResult ? ok(((BooleanResult) result).getValue()) :
             result instanceof NullResult ? ok(null) :
             result instanceof StringResult ? ok(((StringResult) result).getValue()) :
             result instanceof IntResult ? ok(((IntResult) result).getValue()) :
             result instanceof FloatResult ? ok(((FloatResult) result).getValue()) :
             result instanceof DoubleResult ? ok(((DoubleResult) result).getValue()) :
             result instanceof InsertOneResult ? ok(new Document(((InsertOneResult) result).toMap())) :
             result instanceof DeleteResult ? ok(new Document(((DeleteResult) result).toMap())) :
             null;
    }
    catch (InterruptedException | ExecutionException e) {
      throw new SQLException(e);
    }
  }
}

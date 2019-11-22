package com.dbschema.mongo.nashorn;

import com.dbschema.mongo.nashorn.MemberFunction.ObjectKind;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Collation;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MapProcessors.COLLATION;
import static com.dbschema.mongo.nashorn.MemberFunction.func;
import static com.dbschema.mongo.nashorn.MemberFunction.notImplemented;


public class JFindIterable extends AbstractJSObject implements Iterable<Document> {
  private final FindIterable<Document> iterable;
  private final MongoJSObject delegate;

  public JFindIterable(FindIterable<Document> iterable) {
    this.iterable = iterable;
    delegate = new MongoJSObject(Arrays.asList(
        notImplemented("addOption",       ObjectKind.CURSOR),
        func("batchSize",                 n -> { iterable.batchSize(n.intValue()); return this; }, Number.class),
        notImplemented("close",           ObjectKind.CURSOR),
        notImplemented("isClosed",        ObjectKind.CURSOR),
        func("collation",                 m -> { iterable.collation(MapProcessor.runProcessors(Collation.builder(), COLLATION, m).build()); return this; }, Map.class),
        func("comment",                   s -> { iterable.comment(s); return this; }, String.class),
        notImplemented("count",           ObjectKind.CURSOR),
        notImplemented("explain",         ObjectKind.CURSOR),
        notImplemented("forEach",         ObjectKind.CURSOR),
        notImplemented("hasNext",         ObjectKind.CURSOR),
        func("hint",                      m -> { iterable.hint(toBson(m)); return this; }, Map.class),
        notImplemented("isExhausted",     ObjectKind.CURSOR),
        notImplemented("itcount",         ObjectKind.CURSOR),
        func("limit",                     n -> { iterable.limit(n.intValue()); return this;}, Number.class),
        notImplemented("map",             ObjectKind.CURSOR),
        func("max",                       m -> { iterable.max(toBson(m)); return this; }, Map.class),
        func("maxTimeMS",                 n -> { iterable.maxTime(n.longValue(), TimeUnit.MILLISECONDS); return this; }, Number.class),
        func("min",                       m -> { iterable.min(toBson(m)); return this; }, Map.class),
        notImplemented("next",            ObjectKind.CURSOR),
        func("noCursorTimeout",           b -> { iterable.noCursorTimeout(b); return this; }, Boolean.class),
        notImplemented("objsLeftInBatch", ObjectKind.CURSOR),
        notImplemented("pretty",          ObjectKind.CURSOR),
        notImplemented("readConcern",     ObjectKind.CURSOR),
        notImplemented("readPref",        ObjectKind.CURSOR),
        func("returnKey",                 b -> { iterable.returnKey(b); return this; }, Boolean.class),
        func("showRecordId",              () -> { iterable.showRecordId(true); return this; }),
        notImplemented("size",            ObjectKind.CURSOR),
        func("skip",                      n -> { iterable.skip(n.intValue()); return this; }, Number.class),
        func("sort",                      m -> { iterable.sort(toBson(m)); return this; }, Map.class),
        notImplemented("tailable",        ObjectKind.CURSOR),
        notImplemented("toArray",         ObjectKind.CURSOR)
    ));
  }

  @Override
  public boolean hasMember(String name) {
    return delegate.hasMember(name);
  }

  @Override
  public Object getMember(String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : new AbstractJSObject() {
      @Override
      public Object call(Object thiz, Object... args) {
        throw new UnsupportedOperationException("Method not found: cursor." + name);
      }
    };
  }

  public void batchSize(int v) {
    iterable.batchSize(v);
  }

  public JFindIterable skip(int i) {
    iterable.skip(i);
    return this;
  }

  @NotNull
  public MongoCursor<Document> iterator() {
    return iterable.iterator();
  }
}

package com.dbschema.mongo.nashorn;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MemberFunction.func;
import static com.dbschema.mongo.nashorn.MemberFunction.notImplemented;


public class JAggregateIterable extends AbstractJSObject implements Iterable<Document> {
  private final AggregateIterable<Document> iterable;
  private final MongoJSObject delegate;

  public JAggregateIterable(AggregateIterable<Document> iterable) {
    this.iterable = iterable;
    delegate = new MongoJSObject(Arrays.asList(
        notImplemented("close",           MemberFunction.ObjectKind.CURSOR),
        notImplemented("isClosed",        MemberFunction.ObjectKind.CURSOR),
        notImplemented("forEach",         MemberFunction.ObjectKind.CURSOR),
        notImplemented("hasNext",         MemberFunction.ObjectKind.CURSOR),
        func("hint",                      m -> { iterable.hint(toBson(m)); return this; }, Map.class),
        notImplemented("isExhausted",     MemberFunction.ObjectKind.CURSOR),
        notImplemented("itcount",         MemberFunction.ObjectKind.CURSOR),
        notImplemented("next",            MemberFunction.ObjectKind.CURSOR),
        notImplemented("objsLeftInBatch", MemberFunction.ObjectKind.CURSOR),
        notImplemented("pretty",          MemberFunction.ObjectKind.CURSOR),
        notImplemented("toArray",         MemberFunction.ObjectKind.CURSOR)
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

  @NotNull
  public MongoCursor<Document> iterator() {
    return iterable.iterator();
  }
}

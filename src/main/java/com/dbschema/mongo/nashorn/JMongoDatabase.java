package com.dbschema.mongo.nashorn;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.*;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.dbschema.mongo.Util.toDocument;
import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MemberFunction.*;


// https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions
// http://sites.psu.edu/robertbcolton/2015/07/31/java-8-nashorn-script-engine/
public class JMongoDatabase extends AbstractJSObject {
  private final MongoDatabase mongoDatabase;
  private final MongoJSObject delegate;
  @SuppressWarnings("deprecation")
  private final List<MapProcessor<CreateCollectionOptions, ?>> collectionOptionsProcessors = Arrays.asList(
      new MapProcessor<>("capped", Boolean.class, (v, o) -> o.capped(v)),
      new MapProcessor<>("autoIndexId", Boolean.class, (v, o) -> o.autoIndex(v)),
      new MapProcessor<>("size", Number.class, (v, o) -> o.sizeInBytes(v.longValue())),
      new MapProcessor<>("max", Number.class, (v, o) -> o.maxDocuments(v.longValue())),
      new MapProcessor<>("storageEngine", Map.class, (v, o) -> o), // todo
      new MapProcessor<>("validator", Map.class, (v, o) -> o), // todo
      new MapProcessor<>("validationLevel", String.class, (v, o) -> o), // todo
      new MapProcessor<>("validationAction", String.class, (v, o) -> o), // todo
      new MapProcessor<>("indexOptionDefaults", Map.class, (v, o) -> o), // todo
      new MapProcessor<>("viewOn", String.class, (v, o) -> o), // todo
      new MapProcessor<>("pipeline", String.class, (v, o) -> o), // todo
      new MapProcessor<>("collation", Map.class, (v, o) -> o), // todo
      new MapProcessor<>("writeConcern", Map.class, (v, o) -> o) // todo
  );
  private final List<MapProcessor<Collation.Builder, ?>> collationProcessors = Arrays.asList(
      new MapProcessor<>("locale", String.class, (v, o) -> o.locale(v)),
      new MapProcessor<>("caseLevel", Boolean.class, (v, o) -> o.caseLevel(v)),
      new MapProcessor<>("caseFirst", String.class, (v, o) -> o.collationCaseFirst(CollationCaseFirst.fromString(v))),
      new MapProcessor<>("strength", Number.class, (v, o) -> o.collationStrength(CollationStrength.fromInt(v.intValue()))),
      new MapProcessor<>("numericOrdering", Boolean.class, (v, o) -> o.numericOrdering(v)),
      new MapProcessor<>("alternate", String.class, (v, o) -> o.collationAlternate(CollationAlternate.fromString(v))),
      new MapProcessor<>("maxVariable", String.class, (v, o) -> o.collationMaxVariable(CollationMaxVariable.fromString(v))),
      new MapProcessor<>("backwards", Boolean.class, (v, o) -> o.backwards(v))
  );
  private final List<MapProcessor<CreateViewOptions, ?>> viewOptionsProcessors = Collections.singletonList(
      new MapProcessor<>("collation", Map.class, (map, o) -> {
        Collation.Builder collation = Collation.builder();
        if (map != null) collation = MapProcessor.runProcessors(collation, collationProcessors, map);
        return o.collation(collation.build());
      })
  );

  public JMongoDatabase(@NotNull MongoDatabase mongoDatabase, @NotNull MongoClient mongoClient) {
    this.mongoDatabase = mongoDatabase;
    delegate = new MongoJSObject(Arrays.asList(
        oneDoc("adminCommand",                  m -> new JMongoDatabase(mongoClient.getDatabase("admin"), mongoClient).runCommand(m), Map.class),
        oneDoc("adminCommand",                  s -> new JMongoDatabase(mongoClient.getDatabase("admin"), mongoClient).runCommand(s), String.class),
        oneDoc("aggregate",                     this::aggregate,                List.class),
        oneDoc("aggregate",                     this::aggregate,                List.class, Map.class),
        oneDoc("cloneCollection",               this::cloneCollection,          String.class, String.class),
        oneDoc("cloneCollection",               this::cloneCollection,          String.class, String.class, Map.class),
        igoreParams("cloneDatabase",            () -> "Not implemented"), // no such command
        func("commandHelp",                     this::commandHelp, String.class),
        igoreParams("copyDatabase",             () -> "Not implemented"), // no such command
        voidFunc("createCollection",            mongoDatabase::createCollection,String.class),
        voidFunc("createCollection",            this::createCollection,         String.class, Map.class),
        voidFunc("createView",                  this::createView,               String.class, String.class, List.class),
        voidFunc("createView",                  this::createView,               String.class, String.class, List.class, Map.class),
        oneDoc("currentOp",                     () -> runCommand("currentOp")),
        voidFunc("dropDatabase",                mongoDatabase::drop),
        oneDoc("fsyncLock",                     () -> runCommand("fsync")),
        oneDoc("fsyncUnlock",                   () -> runCommand("fsyncUnlock")),
        func("getCollection",                   this::getCollection,            String.class),
        igoreParams("getCollectionInfos",       () -> "Not implemented"), // no such command
        func("getCollectionNames",              this::listCollectionNames),
        oneDoc("getLastError",                  () -> runCommand("getLastError")),
        oneDoc("getLastError",                  w -> runCommand(new Document("getLastError", 1).append("w", w.intValue())), Number.class),
        oneDoc("getLastError",                  w -> runCommand(new Document("getLastError", 1).append("w", w)), String.class),
        oneDoc("getLastError",                  (w, t) -> runCommand(new Document("getLastError", 1).append("w", w.intValue()).append("wtimeout", t.intValue())), Number.class, Number.class),
        oneDoc("getLastError",                  (w, t) -> runCommand(new Document("getLastError", 1).append("w", w).append("wtimeout", t.intValue())), String.class, Number.class),
        igoreParams("getLastErrorObj",          () -> "Not implemented"), // no such command
        igoreParams("getLogComponents",         () -> "Not implemented"), // no such command
        igoreParams("getMongo",                 () -> "Not implemented"), // no such command
        func("getName",                         mongoDatabase::getName),
        oneDoc("getPrevError",                  () -> runCommand("getPrevError")),
        igoreParams("getProfilingLevel",        () -> "Not implemented"), // no such command
        igoreParams("getProfilingStatus",       () -> "Not implemented"), // no such command
        igoreParams("getReplicationInfo",       () -> "Not implemented"), // no such command
        igoreParams("getSiblingDB",             () -> "Not implemented"), // no such command
        igoreParams("help",                     () -> "Not implemented"), // no such command
        oneDoc("hostInfo",                      () -> runCommand("hostInfo")),
        oneDoc("isMaster",                      () -> runCommand("isMaster")),
        oneDoc("killOp",                        id -> runCommand(new Document("killOp", 1).append("op", id.intValue())), Number.class),
        oneDoc("listCommands",                  () -> runCommand("listCommands")),
        oneDoc("logout",                        () -> runCommand("logout")),
        igoreParams("printCollectionStats",     () -> "Not implemented"), // no such command
        igoreParams("printReplicationInfo",     () -> "Not implemented"), // no such command
        igoreParams("printShardingStatus",      () -> "Not implemented"), // no such command
        igoreParams("printSlaveReplicationInfo",() -> "Not implemented"), // no such command
        oneDoc("resetError",                    () -> runCommand("resetError")),
        oneDoc("runCommand",                    this::runCommand,               Map.class),
        oneDoc("runCommand",                    this::runCommand,               String.class),
        oneDoc("serverBuildInfo",               () -> runCommand("buildinfo")),
        igoreParams("serverCmdLineOpts",        () -> runCommand("getCmdLineOpts")),
        oneDoc("serverStatus",                  () -> runCommand("serverStatus")),
        oneDoc("serverStatus",                  m -> runCommand(appendOptions(new Document("serverStatus", 1), m)), Map.class),
        igoreParams("setLogLevel",              () -> "Not implemented"), // no such command
        igoreParams("setProfilingLevel",        () -> "Not implemented"), // no such command
        oneDoc("shutdownServer",                () -> runCommand("shutdown")),
        oneDoc("stats",                         () -> runCommand("dbStats")),
        oneDoc("stats",                         n -> runCommand(new Document("dbStats", 1).append("scale", n)), Number.class),
        func("version",                         this::version),
        igoreParams("watch",                    () -> "Not implemented"))); // no such command
  }

  public String getName() {
    return mongoDatabase.getName();
  }

  public Object commandHelp(String commandName) {
    try {
      Document commands = mongoDatabase.runCommand(new Document("listCommands", null));
      String msg = "Help for command " + commandName + " not found";
      if (!commands.containsKey("commands")) return msg;
      Object c = commands.get("commands");
      if (!(c instanceof Document)) return msg;
      if (!((Document) c).containsKey(commandName)) return msg;
      Object commandDoc = ((Document) c).get(commandName);
      if (!(commandDoc instanceof Document)) return msg;
      return ((Document) commandDoc).containsKey("help") ? (String) ((Document) commandDoc).get("help") : msg;
    }
    catch (MongoCommandException e) {
      return Collections.singletonList(toDocument(e.getResponse()));
    }
  }

  public Document aggregate(List<?> pipeline) {
    return aggregate(pipeline, null);
  }

  public Document aggregate(List<?> pipeline, @Nullable Map<?, ?> options) {
    List<Bson> convertedPipe = new ArrayList<>(pipeline.size());
    for (Object pipe : pipeline) {
      if (pipe instanceof Map<?, ?>) convertedPipe.add(toBson((Map<?, ?>) pipe));
    }
    Document command = new Document("aggregate", 1).append("pipeline", convertedPipe);
    appendOptions(command, options);
    return runCommand(command);
  }

  private Document appendOptions(Document command, @Nullable Map<?, ?> options) {
    if (options == null) return command;
    Document op = toBson(options);
    for (String key : op.keySet()) {
      command.put(key, op.get(key));
    }
    return command;
  }

  public Document cloneCollection(String from, String collection) {
    return cloneCollection(from, collection, null);
  }

  public Document cloneCollection(String from, String collection, @Nullable Map<?, ?> query) {
    Document command = new Document("cloneCollection", collection).append("from", from);
    if (query != null) command.append("query", toBson(query));
    return runCommand(command);
  }

  public void createCollection(String name, Map<?, ?> map) {
    mongoDatabase.createCollection(name, extractCollectionOptions(map));
  }

  public void createView(String viewName, String collectionName, List<?> pipeline) {
    createView(viewName, collectionName, pipeline, null);
  }

  public void createView(String viewName, String collectionName, List<?> pipeline, @Nullable Map<?, ?> options) {
    List<Bson> convertedPipe = new ArrayList<>(pipeline.size());
    for (Object pipe : pipeline) {
      if (pipe instanceof Map<?, ?>) convertedPipe.add(toBson((Map<?, ?>) pipe));
    }
    if (options == null) mongoDatabase.createView(viewName, collectionName, convertedPipe);
    else {
      CreateViewOptions createViewOptions = new CreateViewOptions();
      MapProcessor.runProcessors(createViewOptions, viewOptionsProcessors, options);
      mongoDatabase.createView(viewName, collectionName, convertedPipe, createViewOptions);
    }
  }

  @NotNull
  private CreateCollectionOptions extractCollectionOptions(@Nullable Map<?, ?> map) {
    CreateCollectionOptions options = new CreateCollectionOptions();
    return map == null ? options : MapProcessor.runProcessors(options, collectionOptionsProcessors, map);
  }

  public JMongoCollection getCollection(String s) {
    return new JMongoCollection(mongoDatabase.getCollection(s), s, mongoDatabase);
  }

  public Document runCommand(Map<?, ?> map) {
    try {
      return mongoDatabase.runCommand(toBson(map));
    }
    catch (MongoCommandException e) {
      return toDocument(e.getResponse());
    }
  }

  private Document runCommand(Document map) {
    try {
      return mongoDatabase.runCommand(map);
    }
    catch (MongoCommandException e) {
      return toDocument(e.getResponse());
    }
  }

  public Document runCommand(String command) {
    try {
      return mongoDatabase.runCommand(new Document(command, null));
    }
    catch (MongoCommandException e) {
      return toDocument(e.getResponse());
    }
  }

  public MongoIterable<String> listCollectionNames() {
    return mongoDatabase.listCollectionNames();
  }

  private Iterable<String> version() {
    Document info = mongoDatabase.runCommand(new Document("buildinfo", null));
    String v = info.getString("version");
    return v == null ? null : Collections.singletonList(v);
  }

  public void createCollection(String s) {
    mongoDatabase.createCollection(s);
  }

  /**
   * I overwrite this methods to make possible to call database.collection.....
   * To perform like a collection is a member variable of the database.
   * Only getCollection(), drop(), runCommand() functions continue to work on this object.
   */
  @Override
  public boolean hasMember(String name) {
    return delegate.hasMember(name);
  }

  @Override
  public Object getMember(final String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : getCollection(name);
  }
}

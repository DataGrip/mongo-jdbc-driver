package com.dbschema.mongo.nashorn;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import jdk.nashorn.api.scripting.AbstractJSObject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.dbschema.mongo.Util.toDocument;
import static com.dbschema.mongo.nashorn.JMongoUtil.toBson;
import static com.dbschema.mongo.nashorn.MapProcessor.runProcessors;
import static com.dbschema.mongo.nashorn.MapProcessors.CREATE_COLLECTION_OPTIONS;
import static com.dbschema.mongo.nashorn.MapProcessors.CREATE_VIEW_OPTIONS;
import static com.dbschema.mongo.nashorn.MemberFunction.*;


// https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions
// http://sites.psu.edu/robertbcolton/2015/07/31/java-8-nashorn-script-engine/
public class JMongoDatabase extends AbstractJSObject {
  private final MongoDatabase mongoDatabase;
  private final MongoJSObject delegate;

  public JMongoDatabase(@NotNull MongoDatabase mongoDatabase, @NotNull MongoClient mongoClient) {
    this.mongoDatabase = mongoDatabase;
    delegate = new MongoJSObject(Arrays.asList(
        func("adminCommand",                    m -> new JMongoDatabase(mongoClient.getDatabase("admin"), mongoClient).runCommand(m), Map.class),
        func("adminCommand",                    s -> new JMongoDatabase(mongoClient.getDatabase("admin"), mongoClient).runCommand(s), String.class),
        func("aggregate",                       this::aggregate,                List.class),
        func("aggregate",                       this::aggregate,                List.class, Map.class),
        func("cloneCollection",                 this::cloneCollection,          String.class, String.class),
        func("cloneCollection",                 this::cloneCollection,          String.class, String.class, Map.class),
        notImplemented("cloneDatabase",         ObjectKind.DATABASE), // no such command
        func("commandHelp",                     this::commandHelp, String.class),
        notImplemented("copyDatabase",          ObjectKind.DATABASE), // no such command
        voidFunc("createCollection",            mongoDatabase::createCollection,String.class),
        voidFunc("createCollection",            this::createCollection,         String.class, Map.class),
        voidFunc("createView",                  this::createView,               String.class, String.class, List.class),
        voidFunc("createView",                  this::createView,               String.class, String.class, List.class, Map.class),
        func("currentOp",                       () -> runCommand("currentOp")),
        voidFunc("dropDatabase",                mongoDatabase::drop),
        func("fsyncLock",                       () -> runCommand("fsync")),
        func("fsyncUnlock",                     () -> runCommand("fsyncUnlock")),
        func("getCollection",                   this::getCollection,            String.class),
        notImplemented("getCollectionInfos",    ObjectKind.DATABASE), // no such command
        func("getCollectionNames",              this::listCollectionNames),
        func("getLastError",                    () -> runCommand("getLastError")),
        func("getLastError",                    w -> runCommand(new Document("getLastError", 1).append("w", w.intValue())), Number.class),
        func("getLastError",                    w -> runCommand(new Document("getLastError", 1).append("w", w)), String.class),
        func("getLastError",                    (w, t) -> runCommand(new Document("getLastError", 1).append("w", w.intValue()).append("wtimeout", t.intValue())), Number.class, Number.class),
        func("getLastError",                    (w, t) -> runCommand(new Document("getLastError", 1).append("w", w).append("wtimeout", t.intValue())), String.class, Number.class),
        notImplemented("getLastErrorObj",       ObjectKind.DATABASE), // no such command
        notImplemented("getLogComponents",      ObjectKind.DATABASE), // no such command
        notImplemented("getMongo",              ObjectKind.DATABASE), // no such command
        func("getName",                         mongoDatabase::getName),
        func("getPrevError",                    () -> runCommand("getPrevError")),
        notImplemented("getProfilingLevel",     ObjectKind.DATABASE), // no such command
        notImplemented("getProfilingStatus",    ObjectKind.DATABASE), // no such command
        notImplemented("getReplicationInfo",    ObjectKind.DATABASE), // no such command
        notImplemented("getSiblingDB",          ObjectKind.DATABASE), // no such command
        notImplemented("help",                  ObjectKind.DATABASE), // no such command
        func("hostInfo",                        () -> runCommand("hostInfo")),
        func("isMaster",                        () -> runCommand("isMaster")),
        func("killOp",                          id -> runCommand(new Document("killOp", 1).append("op", id.intValue())), Number.class),
        func("listCommands",                    () -> runCommand("listCommands")),
        func("logout",                          () -> runCommand("logout")),
        notImplemented("printCollectionStats",  ObjectKind.DATABASE), // no such command
        notImplemented("printReplicationInfo",  ObjectKind.DATABASE), // no such command
        notImplemented("printShardingStatus",   ObjectKind.DATABASE), // no such command
        notImplemented("printSlaveReplicationInfo", ObjectKind.DATABASE), // no such command
        func("resetError",                      () -> runCommand("resetError")),
        func("runCommand",                      this::runCommand,               Map.class),
        func("runCommand",                      this::runCommand,               String.class),
        func("serverBuildInfo",                 () -> runCommand("buildinfo")),
        func("serverCmdLineOpts",               () -> runCommand("getCmdLineOpts")),
        func("serverStatus",                    () -> runCommand("serverStatus")),
        func("serverStatus",                    m -> runCommand(appendOptions(new Document("serverStatus", 1), m)), Map.class),
        notImplemented("setLogLevel",           ObjectKind.DATABASE), // no such command
        notImplemented("setProfilingLevel",     ObjectKind.DATABASE), // no such command
        func("shutdownServer",                  () -> runCommand("shutdown")),
        func("stats",                           () -> runCommand("dbStats")),
        func("stats",                           n -> runCommand(new Document("dbStats", 1).append("scale", n)), Number.class),
        func("version",                         this::version),
        notImplemented("watch",                 ObjectKind.DATABASE))); // no such command
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
      runProcessors(createViewOptions, CREATE_VIEW_OPTIONS, options);
      mongoDatabase.createView(viewName, collectionName, convertedPipe, createViewOptions);
    }
  }

  @NotNull
  private CreateCollectionOptions extractCollectionOptions(@Nullable Map<?, ?> map) {
    CreateCollectionOptions options = new CreateCollectionOptions();
    return map == null ? options : runProcessors(options, CREATE_COLLECTION_OPTIONS, map);
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
    return runCommand(mongoDatabase, map);
  }

  public Document runCommand(String command) {
    return runCommand(mongoDatabase, command);
  }

  public static Document runCommand(@NotNull MongoDatabase mongoDatabase, @NotNull String command) {
    try {
      return mongoDatabase.runCommand(new Document(command, 1));
    }
    catch (MongoCommandException e) {
      return toDocument(e.getResponse());
    }
  }

  public static Document runCommand(@NotNull MongoDatabase mongoDatabase, @NotNull Document map) {
    try {
      return mongoDatabase.runCommand(map);
    }
    catch (MongoCommandException e) {
      return toDocument(e.getResponse());
    }
  }

  public MongoIterable<String> listCollectionNames() {
    return mongoDatabase.listCollectionNames();
  }

  private Iterable<String> version() {
    Document info = mongoDatabase.runCommand(new Document("buildinfo", 1));
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
  public AbstractJSObject getMember(final String name) {
    AbstractJSObject member = delegate.getMember(name);
    return member != null ? member : getCollection(name);
  }
}

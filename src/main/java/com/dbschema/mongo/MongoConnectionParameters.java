package com.dbschema.mongo;

import com.mongodb.AuthenticationMechanism;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MongoConnectionParameters {
  public final String username;
  public final char[] password;
  public final String authSource;
  public final String database;
  public final AuthenticationMechanism mechanism;

  public MongoConnectionParameters(@Nullable String username,
                                   @Nullable char[] password,
                                   @NotNull String authSource,
                                   @Nullable String database,
                                   @Nullable AuthenticationMechanism mechanism) {
    this.username = username;
    this.password = password;
    this.authSource = authSource;
    this.database = database;
    this.mechanism = mechanism;
  }
}

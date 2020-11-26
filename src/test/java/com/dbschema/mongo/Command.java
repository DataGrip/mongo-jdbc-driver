package com.dbschema.mongo;

/**
 * @author Liudmila Kornilova
 **/
public class Command {
  public final String command;
  public final CommandOptions options;

  public Command(String command, CommandOptions options) {
    this.command = command;
    this.options = options;
  }

  public static class CommandOptions {
    final boolean dontCheckValue;

    public CommandOptions(boolean dontCheckValue) {
      this.dontCheckValue = dontCheckValue;
    }
  }
}

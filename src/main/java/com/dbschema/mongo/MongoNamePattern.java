package com.dbschema.mongo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class MongoNamePattern {
  private static final Set<Character> SPECIAL_REGEX_CHARS = new HashSet<>();

  private final String plain;
  private final Pattern pattern;

  static {
    SPECIAL_REGEX_CHARS.add('[');
    SPECIAL_REGEX_CHARS.add(']');
    SPECIAL_REGEX_CHARS.add('{');
    SPECIAL_REGEX_CHARS.add('}');
    SPECIAL_REGEX_CHARS.add('(');
    SPECIAL_REGEX_CHARS.add(')');
    SPECIAL_REGEX_CHARS.add('\\');
    SPECIAL_REGEX_CHARS.add('.');
    SPECIAL_REGEX_CHARS.add('*');
    SPECIAL_REGEX_CHARS.add('+');
    SPECIAL_REGEX_CHARS.add('?');
    SPECIAL_REGEX_CHARS.add('^');
    SPECIAL_REGEX_CHARS.add('$');
    SPECIAL_REGEX_CHARS.add('|');
  }

  private MongoNamePattern() {
    this.plain = null;
    this.pattern = null;
  }

  private MongoNamePattern(@NotNull Pattern pattern) {
    this.plain = null;
    this.pattern = pattern;
  }

  private MongoNamePattern(@NotNull String plain) {
    this.plain = plain;
    this.pattern = null;
  }

  @Nullable
  public String asPlain() {
    return plain;
  }

  public boolean matches(@NotNull String name) {
    return plain != null ? plain.equals(name) :
           pattern == null || pattern.matcher(name).matches();
  }

  /**
   * Given an inputPattern using SQL syntax (e.g. % for wildcard, and '_' for single character) generate a Java Pattern
   * that can be used to validate input.
   *
   * @param inputPattern The String representing the SQL pattern.
   * @return A suitable Pattern if the input has wildcard characters in it, or NULL if no pattern matching required.
   */
  @NotNull
  public static MongoNamePattern create(String inputPattern) throws IllegalArgumentException {
    if (inputPattern == null) return new MongoNamePattern();
    String plain = toPlain(inputPattern);
    if (plain != null) return new MongoNamePattern(plain);

    boolean escaped = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inputPattern.length(); i++) {
      char c = inputPattern.charAt(i);
      if (c == '\\') {
        if (escaped) sb.append("\\\\");
        escaped = !escaped;
      }
      else if (escaped) {
        escaped = false;
        if (c == '_' || c == '%') sb.append(c);
        else
          throw new IllegalArgumentException("Illegal char escape " + c + " at position " + i + " string: " + inputPattern);
      }
      else {
        if (c == '_') sb.append(".");
        else if (c == '%') sb.append(".*");
        else if (SPECIAL_REGEX_CHARS.contains(c)) sb.append('\\').append(c);
        else sb.append(c);
      }
    }

    String pattern = sb.toString();
    return pattern.equals(".*") ? new MongoNamePattern() : new MongoNamePattern(Pattern.compile(pattern));
  }

  @Nullable
  private static String toPlain(@NotNull String inputPattern) {
    boolean escaped = false;
    StringBuilder plain = new StringBuilder();
    for (int i = 0; i < inputPattern.length(); i++) {
      char c = inputPattern.charAt(i);
      if (c == '\\') {
        if (escaped) plain.append("\\");
        escaped = !escaped;
      }
      else if (escaped) {
        escaped = false;
        if (c == '_') plain.append("_");
        else if (c == '%') plain.append("%");
        else
          throw new IllegalArgumentException("Illegal char escape " + c + " at position " + i + " string: " + inputPattern);
      }
      else {
        if (c == '_' || c == '%') return null; // unescaped pattern entities
        plain.append(c);
      }
    }
    return plain.toString();
  }
}

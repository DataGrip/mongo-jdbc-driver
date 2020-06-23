package com.dbschema.mongo;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class PatternSupport {
  private static final Set<Character> SPECIAL_REGEX_CHARS = new HashSet<>();

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

  /**
   * Given an inputPattern using SQL syntax (e.g. % for wildcard, and '_' for single character) generate a Java Pattern
   * that can be used to validate input.
   *
   * @param inputPattern The String representing the SQL pattern.
   * @return A suitable Pattern if the input has wildcard characters in it, or NULL if no pattern matching required.
   */
  static Pattern getPattern(String inputPattern) throws IllegalArgumentException {
    if (inputPattern == null ||
        !inputPattern.contains("\\") && !inputPattern.contains("_") && !inputPattern.contains("%"))
      return null;

    boolean escaped = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inputPattern.length(); i++) {
      char c = inputPattern.charAt(i);
      if (c == '\\') {
        if (escaped) sb.append("\\\\");
        escaped = !escaped;
      }
      else if (escaped) {
        if (c == '_') sb.append("_");
        else if (c == '%') sb.append("%");
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

    try {
      return Pattern.compile(sb.toString());
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }
}

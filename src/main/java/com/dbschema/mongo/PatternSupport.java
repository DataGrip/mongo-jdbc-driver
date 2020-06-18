package com.dbschema.mongo;

import java.util.regex.Pattern;

public final class PatternSupport {

/**
 * Given an inputPattern using SQL syntax (e.g. % for wildcard, and '_' for single character) generate a Java Pattern
 * that can be used to validate input.
 * 
 * @param inputPattern The String representing the SQL pattern.
 * @return A suitable Pattern if the input has wildcard characters in it, or NULL if no pattern matching required.
 */
static Pattern getPattern(String inputPattern) {
    if (inputPattern == null)
      return null;

    String regExp = inputPattern.replaceAll("([^\\\\])%|^%", "$1.*").replaceAll("([^\\\\])_|^_", "$1.");

    if (regExp.equals(inputPattern))
      return null;

    try {
      return Pattern.compile(regExp);
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }
}

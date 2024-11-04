package com.dbschema.mongo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestDataReader {
  private static final Pattern HEADER_PATTERN = Pattern.compile("//\\s*(?<name>\\S+)(?<properties>(\\s+\\S+)+)?");

  public static void read(String text, List<SectionHandler> handlers) {
    SectionHandler currentHandler = null;
    Map<String, String> currentProperties = new HashMap<>();
    StringBuilder currentSection = new StringBuilder();
    for (String line : text.split("\n")) {
      line = line.trim();
      Matcher matcher = HEADER_PATTERN.matcher(line);
      if (matcher.matches()) {
        if (currentHandler != null) currentHandler.valueConsumer.accept(currentSection.toString(), currentProperties);
        currentSection.setLength(0);
        currentProperties = new HashMap<>();
        String headerName = matcher.group("name");
        String propsString = matcher.group("properties");
        if (propsString != null) {
          String[] props = propsString.trim().split("\\s+");
          for (String prop : props) {
            if (prop.isEmpty()) continue;
            int eq = prop.indexOf('=');
            if (eq == -1) currentProperties.put(prop, "true");
            else currentProperties.put(prop.substring(0, eq), prop.substring(eq + 1));
          }
        }
        currentHandler = Util.find(handlers, handler -> handler.sectionName.equals(headerName));
      }
      else {
        if (currentSection.length() != 0) currentSection.append("\n");
        currentSection.append(line);
      }
    }
    if (currentHandler != null) currentHandler.valueConsumer.accept(currentSection.toString(), currentProperties);
  }

  public static class SectionHandler {
    private final String sectionName;
    private final BiConsumer<String, Map<String, String>> valueConsumer;

    public SectionHandler(String sectionName, BiConsumer<String, Map<String, String>> valueConsumer) {
      this.sectionName = sectionName;
      this.valueConsumer = valueConsumer;
    }
  }
}

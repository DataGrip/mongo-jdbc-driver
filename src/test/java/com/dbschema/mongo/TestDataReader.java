package com.dbschema.mongo;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Liudmila Kornilova
 **/
public class TestDataReader {
  private static final Pattern HEADER_PATTERN = Pattern.compile("//\\s*(.*)");

  public static void read(String text, List<SectionHandler> handlers) {
    SectionHandler currentHandler = null;
    StringBuilder currentSection = new StringBuilder();
    for (String line : text.split("\n")) {
      line = line.trim();
      Matcher matcher = HEADER_PATTERN.matcher(line);
      if (matcher.matches()) {
        if (currentHandler != null) currentHandler.valueConsumer.accept(currentSection.toString());
        currentSection.setLength(0);
        String headerName = matcher.group(1);
        currentHandler = Util.find(handlers, handler -> handler.sectionName.equals(headerName));
      }
      else {
        if (currentSection.length() != 0) currentSection.append("\n");
        currentSection.append(line);
      }
    }
    if (currentHandler != null) currentHandler.valueConsumer.accept(currentSection.toString());
  }

  public static class SectionHandler {
    private final String sectionName;
    private final Consumer<String> valueConsumer;

    public SectionHandler(String sectionName, Consumer<String> valueConsumer) {
      this.sectionName = sectionName;
      this.valueConsumer = valueConsumer;
    }
  }
}

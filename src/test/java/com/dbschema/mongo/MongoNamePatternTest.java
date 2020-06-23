package com.dbschema.mongo;

import org.junit.Test;

import static org.junit.Assert.*;

public class MongoNamePatternTest {

  @Test
  public void testSimple() {
    MongoNamePattern p = MongoNamePattern.create("Hello");
    assertTrue(p.matches("Hello"));
  }

  @Test
  public void testUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("Hello_");
    assertTrue(p.matches("Hello!"));
    assertFalse(p.matches("Hello"));
  }

  @Test
  public void testMatchAll() {
    MongoNamePattern p = MongoNamePattern.create("%");
    assertTrue(p.matches("Hello"));
    assertTrue(p.matches(""));
  }

  @Test
  public void testSloshedUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("Hello\\_");
    assertTrue(p.matches("Hello_"));
  }

  @Test
  public void testPercent() {
    MongoNamePattern p = MongoNamePattern.create("Hello%");
    assertTrue(p.matches("Hello!"));
    assertTrue(p.matches("Hello"));
  }

  @Test
  public void testSloshedPercent() {
    MongoNamePattern p = MongoNamePattern.create("Hello\\%");
    assertTrue(p.matches("Hello%"));
    assertFalse(p.matches("Hello"));
  }

  @Test
  public void testMatchUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("Hello_");
    assertTrue(p.matches("Hello1"));
    assertTrue(p.matches("Hello1"));
    assertFalse(p.matches("Hello12"));
  }

  @Test
  public void testMatchLeadingUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("_Hello_");
    assertTrue(p.matches("1Hello1"));
    assertTrue(p.matches("_Hello_"));
    assertFalse(p.matches("Hello"));
  }

  @Test
  public void testMatchPercent() {
    MongoNamePattern p = MongoNamePattern.create("Hello%");
    assertTrue(p.matches("Hello"));
    assertTrue(p.matches("Hello1"));
    assertTrue(p.matches("Hello_"));
    assertTrue(p.matches("Hello%"));
    assertTrue(p.matches("Hello%%"));
    assertTrue(p.matches("Hello12"));
    assertFalse(p.matches("Hell"));
  }

  @Test
  public void testLeadingMatchPercent() {
    MongoNamePattern p = MongoNamePattern.create("%Hello%");
    assertTrue(p.matches("Hello"));
    assertTrue(p.matches("Hello1"));
    assertTrue(p.matches("Hello_"));
    assertTrue(p.matches("Hello%"));
    assertTrue(p.matches("Hello%%"));
    assertTrue(p.matches("Hello12"));
    assertTrue(p.matches("_Hello12"));
    assertTrue(p.matches("__Hello12"));
    assertFalse(p.matches("Hell"));
  }

  @Test
  public void testEscapedUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("\\_");
    assertTrue(p.matches("_"));
    assertFalse(p.matches("1"));
  }

  @Test
  public void testEscapedPercent() {
    MongoNamePattern p = MongoNamePattern.create("\\%");
    assertTrue(p.matches("%"));
    assertFalse(p.matches("1"));
  }

  @Test
  public void testEscapedSlashAndUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("\\\\\\\\\\_");
    assertTrue(p.matches("\\\\_"));
    assertFalse(p.matches("\\\\1"));
  }

  @Test
  public void testPatternWithDotAndUnderscore() {
    MongoNamePattern p = MongoNamePattern.create("hello.world_");
    assertTrue(p.matches("hello.world!"));
    assertFalse(p.matches("hello1world!"));
    assertFalse(p.matches("hello.world"));
  }

  @Test
  public void testIllegalEscape() {
    IllegalArgumentException exception = null;
    try {
      MongoNamePattern.create("Hello\\world");
    }
    catch (IllegalArgumentException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
}

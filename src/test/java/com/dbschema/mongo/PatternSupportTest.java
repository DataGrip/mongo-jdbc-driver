package com.dbschema.mongo;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class PatternSupportTest {

  @Test
  public void testSimple() {
    assertNull(PatternSupport.getPattern("Hello"));
  }

  @Test
  public void testUnderscore() {
    assertNotNull(PatternSupport.getPattern("Hello_"));
  }

  @Test
  public void testSloshedUnderscore() {
    Pattern p = PatternSupport.getPattern("Hello\\_");
    assertNotNull(p);
    assertTrue(p.matcher("Hello_").matches());
  }

  @Test
  public void testPercent() {
    assertNotNull(PatternSupport.getPattern("Hello%"));
  }

  @Test
  public void testSloshedPercent() {
    Pattern p = PatternSupport.getPattern("Hello\\%");
    assertNotNull(p);
    assertTrue(p.matcher("Hello%").matches());
    assertFalse(p.matcher("Hello").matches());
  }

  @Test
  public void testMatchUnderscore() {
    Pattern p = PatternSupport.getPattern("Hello_");
    assertTrue(p.matcher("Hello1").matches());
    assertTrue(p.matcher("Hello1").matches());
    assertFalse(p.matcher("Hello12").matches());
  }

  @Test
  public void testMatchLeadingUnderscore() {
    Pattern p = PatternSupport.getPattern("_Hello_");
    assertTrue(p.matcher("1Hello1").matches());
    assertTrue(p.matcher("_Hello_").matches());
    assertFalse(p.matcher("Hello").matches());
  }

  @Test
  public void testMatchPercent() {
    Pattern p = PatternSupport.getPattern("Hello%");
    assertTrue(p.matcher("Hello").matches());
    assertTrue(p.matcher("Hello1").matches());
    assertTrue(p.matcher("Hello_").matches());
    assertTrue(p.matcher("Hello%").matches());
    assertTrue(p.matcher("Hello%%").matches());
    assertTrue(p.matcher("Hello12").matches());
    assertFalse(p.matcher("Hell").matches());
  }

  @Test
  public void testLeadingMatchPercent() {
    Pattern p = PatternSupport.getPattern("%Hello%");
    assertTrue(p.matcher("Hello").matches());
    assertTrue(p.matcher("Hello1").matches());
    assertTrue(p.matcher("Hello_").matches());
    assertTrue(p.matcher("Hello%").matches());
    assertTrue(p.matcher("Hello%%").matches());
    assertTrue(p.matcher("Hello12").matches());
    assertTrue(p.matcher("_Hello12").matches());
    assertTrue(p.matcher("__Hello12").matches());
    assertFalse(p.matcher("Hell").matches());
  }

  @Test
  public void testEscapedUnderscore() {
    Pattern p = PatternSupport.getPattern("\\_");
    assertTrue(p.matcher("_").matches());
    assertFalse(p.matcher("1").matches());
  }

  @Test
  public void testEscapedPercent() {
    Pattern p = PatternSupport.getPattern("\\%");
    assertTrue(p.matcher("%").matches());
    assertFalse(p.matcher("1").matches());
  }

  @Test
  public void testEscapedSlashAndUnderscore() {
    Pattern p = PatternSupport.getPattern("\\\\\\\\\\_");
    assertTrue(p.matcher("\\\\_").matches());
    assertFalse(p.matcher("\\\\1").matches());
  }

  @Test
  public void testPatternWithDot() {
    assertNull(PatternSupport.getPattern("hello.world"));
  }

  @Test
  public void testPatternWithDotAndUnderscore() {
    Pattern p = PatternSupport.getPattern("hello.world_");
    assertNotNull(p);
    assertTrue(p.matcher("hello.world!").matches());
    assertFalse(p.matcher("hello1world!").matches());
    assertFalse(p.matcher("hello.world").matches());
  }

  @Test
  public void testIllegalEscape() {
    IllegalArgumentException exception = null;
    try {
      PatternSupport.getPattern("Hello\\world");
    }
    catch (IllegalArgumentException e) {
      exception = e;
    }
    assertNotNull(exception);
  }
}

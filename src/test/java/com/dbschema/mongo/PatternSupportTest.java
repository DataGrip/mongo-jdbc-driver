package com.dbschema.mongo;

import java.util.regex.Pattern;

import org.junit.Test;

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
    assertNull(PatternSupport.getPattern("Hello\\_"));
  }

  @Test
  public void testPercent() {
    assertNotNull(PatternSupport.getPattern("Hello%"));
  }

  @Test
  public void testSloshedPercent() {
    assertNull(PatternSupport.getPattern("Hello\\%"));
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

  // Note - this valid case is not supported - @Test
  public void NotSupported() {
    Pattern p = PatternSupport.getPattern("\\_");
    assertTrue(p.matcher("\\1").matches());
  }
}

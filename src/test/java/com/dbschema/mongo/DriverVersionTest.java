package com.dbschema.mongo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The major and the minor version reported over JDBC are derived from the single DRIVER_VERSION literal that
 * the 'verifyDriverVersion' Gradle task keeps in sync with the Gradle version -- they used to be maintained
 * by hand next to it, and the minor had drifted (21 against 1.23).
 */
public class DriverVersionTest {
    private final MongoDatabaseMetaData metaData = new MongoDatabaseMetaData(null);

    @Test
    public void theMajorAndMinorAreDerivedFromTheVersionLiteral() {
        String[] parts = MongoDatabaseMetaData.DRIVER_VERSION.split("\\.");

        assertEquals(MongoDatabaseMetaData.DRIVER_VERSION, metaData.getDriverVersion());
        assertEquals(Integer.parseInt(parts[0]), metaData.getDriverMajorVersion());
        assertEquals(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, metaData.getDriverMinorVersion());
    }

    /**
     * The derivation runs in a static initializer, so a literal its parse choked on would take down every
     * call on this class with an ExceptionInInitializerError rather than just misreport the version. Reaching
     * a getter at all is what this asserts; 'verifyDriverVersion' rejects such a literal at build time.
     */
    @Test
    public void theVersionLiteralIsDottedNumericAndParsesWithoutThrowing() {
        assertTrue(MongoDatabaseMetaData.DRIVER_VERSION,
            MongoDatabaseMetaData.DRIVER_VERSION.matches("\\d+(\\.\\d+)*"));
        assertTrue(metaData.getDriverMajorVersion() > 0);
        assertTrue(metaData.getDriverMinorVersion() >= 0);
    }
}

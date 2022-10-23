package cn.powernukkitx.libdeflate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LibdeflateJavaUtilsTest {
    @Test
    void testSaneCheckInBounds() {
        assertDoesNotThrow(() -> LibdeflateJavaUtils.checkBounds(1, 0, 1));
    }

    @Test
    void testZeroLengthSane() {
        assertDoesNotThrow(() -> LibdeflateJavaUtils.checkBounds(0, 0, 0));
    }

    @Test
    void testZeroOffsetEndSane() {
        assertDoesNotThrow(() -> LibdeflateJavaUtils.checkBounds(2, 2, 0));
    }

    @Test
    void testNonZeroOffsetAndLengthSane() {
        assertDoesNotThrow(() -> LibdeflateJavaUtils.checkBounds(40, 2, 20));
    }

    @Test
    void testNegativeOffset() {
        assertThrows(IndexOutOfBoundsException.class, () -> LibdeflateJavaUtils.checkBounds(0, -1, 0));
    }

    @Test
    void testNegativeLen() {
        assertThrows(IndexOutOfBoundsException.class, () -> LibdeflateJavaUtils.checkBounds(0, 0, -1));
    }

    @Test
    void testTooBigOffset() {
        assertThrows(IndexOutOfBoundsException.class, () -> LibdeflateJavaUtils.checkBounds(0, 1, 0));
    }

    @Test
    void testTooBigLen() {
        assertThrows(IndexOutOfBoundsException.class, () -> LibdeflateJavaUtils.checkBounds(0, 0, 1));
    }

    @Test
    void testTooSmallOffsetAndLen() {
        assertThrows(IndexOutOfBoundsException.class, () -> LibdeflateJavaUtils.checkBounds(200, 50, 300));
    }
}

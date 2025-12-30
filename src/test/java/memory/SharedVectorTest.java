package memory;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorTest {
    
    private SharedVector rowVector;
    private SharedVector colVector;
    
    @BeforeEach
    public void setUp() {
        rowVector = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        colVector = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
    }
    
    // ========== VALID CASES ==========
    
    @Test
    public void testLength_ValidVector() {
        assertEquals(3, rowVector.length());
        assertEquals(3, colVector.length());
    }
    
    @Test
    public void testGet_ValidIndices() {
        assertEquals(1.0, rowVector.get(0), 0.001);
        assertEquals(2.0, rowVector.get(1), 0.001);
        assertEquals(3.0, rowVector.get(2), 0.001);
    }
    
    @Test
    public void testGetOrientation_Valid() {
        assertEquals(VectorOrientation.ROW_MAJOR, rowVector.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, colVector.getOrientation());
    }
    
    @Test
    public void testNegate_ValidVector() {
        rowVector.writeLock();
        try {
            rowVector.negate();
        } finally {
            rowVector.writeUnlock();
        }
        
        assertEquals(-1.0, rowVector.get(0), 0.001);
        assertEquals(-2.0, rowVector.get(1), 0.001);
        assertEquals(-3.0, rowVector.get(2), 0.001);
    }
    
    @Test
    public void testAdd_ValidVectors_SameLength() {
        SharedVector v1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.ROW_MAJOR);
        
        v1.writeLock();
        v2.readLock();
        try {
            v1.add(v2);
        } finally {
            v2.readUnlock();
            v1.writeUnlock();
        }
        
        assertEquals(5.0, v1.get(0), 0.001);
        assertEquals(7.0, v1.get(1), 0.001);
        assertEquals(9.0, v1.get(2), 0.001);
    }
    
    @Test
    public void testTranspose_RowToColumn() {
        rowVector.writeLock();
        try {
            rowVector.transpose();
            assertEquals(VectorOrientation.COLUMN_MAJOR, rowVector.getOrientation());
        } finally {
            rowVector.writeUnlock();
        }
    }
    
    @Test
    public void testTranspose_ColumnToRow() {
        colVector.writeLock();
        try {
            colVector.transpose();
            assertEquals(VectorOrientation.ROW_MAJOR, colVector.getOrientation());
        } finally {
            colVector.writeUnlock();
        }
    }
    
    @Test
    public void testTranspose_DoubleTranspose() {
        rowVector.writeLock();
        try {
            rowVector.transpose();
            rowVector.transpose();
            assertEquals(VectorOrientation.ROW_MAJOR, rowVector.getOrientation());
        } finally {
            rowVector.writeUnlock();
        }
    }
    
    @Test
    public void testDot_ValidRowAndColumn() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
        
        row.readLock();
        col.readLock();
        try {
            double result = row.dot(col);
            assertEquals(32.0, result, 0.001); // 1*4 + 2*5 + 3*6 = 32
        } finally {
            col.readUnlock();
            row.readUnlock();
        }
    }
    
    // ========== INVALID CASES ==========
    
    @Test
    public void testGet_InvalidNegativeIndex() {
        assertThrows(RuntimeException.class, () -> rowVector.get(-1));
    }
    
    @Test
    public void testGet_InvalidIndexOutOfBounds() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> rowVector.get(10));
    }
    
    @Test
    public void testAdd_DifferentLengths() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        
        v1.writeLock();
        v2.readLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
        } finally {
            v2.readUnlock();
            v1.writeUnlock();
        }
    }
    
    @Test
    public void testAdd_NullVector() {
        rowVector.writeLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> rowVector.add(null));
        } finally {
            rowVector.writeUnlock();
        }
    }
    
    @Test
    public void testDot_SameOrientation_BothRow() {
        SharedVector row1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector row2 = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.ROW_MAJOR);
        
        row1.readLock();
        row2.readLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> row1.dot(row2));
        } finally {
            row2.readUnlock();
            row1.readUnlock();
        }
    }
    
    @Test
    public void testDot_SameOrientation_BothColumn() {
        SharedVector col1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
        SharedVector col2 = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
        
        col1.readLock();
        col2.readLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> col1.dot(col2));
        } finally {
            col2.readUnlock();
            col1.readUnlock();
        }
    }
    
    @Test
    public void testDot_DifferentLengths() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
        
        row.readLock();
        col.readLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> row.dot(col));
        } finally {
            col.readUnlock();
            row.readUnlock();
        }
    }
    
    @Test
    public void testVecMatMul_ColumnVector_ShouldFail() {
        SharedVector col = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
        
        col.writeLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> col.vecMatMul(matrix));
        } finally {
            col.writeUnlock();
        }
    }
    
    @Test
    public void testVecMatMul_EmptyMatrix() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix emptyMatrix = new SharedMatrix(new double[0][0]);
        
        row.writeLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> row.vecMatMul(emptyMatrix));
        } finally {
            row.writeUnlock();
        }
    }
    
    @Test
    public void testVecMatMul_DimensionMismatch() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 2}, {3, 4}}); // 2x2
        
        row.writeLock();
        try {
            assertThrows(IllegalArgumentException.class, () -> row.vecMatMul(matrix));
        } finally {
            row.writeUnlock();
        }
    }
    
    // ========== EDGE CASES ==========
    
    @Test
    public void testSingleElementVector() {
        SharedVector single = new SharedVector(new double[]{42}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(1, single.length());
        assertEquals(42.0, single.get(0), 0.001);
    }
    
    @Test
    public void testSingleElementVector_Negate() {
        SharedVector single = new SharedVector(new double[]{42}, VectorOrientation.ROW_MAJOR);
        
        single.writeLock();
        try {
            single.negate();
            assertEquals(-42.0, single.get(0), 0.001);
        } finally {
            single.writeUnlock();
        }
    }
    
    @Test
    public void testSingleElementVector_Add() {
        SharedVector v1 = new SharedVector(new double[]{10}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
        
        v1.writeLock();
        v2.readLock();
        try {
            v1.add(v2);
            assertEquals(15.0, v1.get(0), 0.001);
        } finally {
            v2.readUnlock();
            v1.writeUnlock();
        }
    }
    
    @Test
    public void testZeroVector() {
        SharedVector zero = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(0.0, zero.get(0), 0.001);
        assertEquals(0.0, zero.get(1), 0.001);
        assertEquals(0.0, zero.get(2), 0.001);
    }
    
    @Test
    public void testZeroVector_Negate() {
        SharedVector zero = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);
        
        zero.writeLock();
        try {
            zero.negate();
            assertEquals(0.0, zero.get(0), 0.001);
            assertEquals(0.0, zero.get(1), 0.001);
            assertEquals(0.0, zero.get(2), 0.001);
        } finally {
            zero.writeUnlock();
        }
    }
    
    @Test
    public void testNegativeValues() {
        SharedVector negative = new SharedVector(new double[]{-1, -2, -3}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(-1.0, negative.get(0), 0.001);
        assertEquals(-2.0, negative.get(1), 0.001);
        assertEquals(-3.0, negative.get(2), 0.001);
    }
    
    @Test
    public void testNegativeValues_Negate() {
        SharedVector negative = new SharedVector(new double[]{-1, -2, -3}, VectorOrientation.ROW_MAJOR);
        
        negative.writeLock();
        try {
            negative.negate();
            assertEquals(1.0, negative.get(0), 0.001);
            assertEquals(2.0, negative.get(1), 0.001);
            assertEquals(3.0, negative.get(2), 0.001);
        } finally {
            negative.writeUnlock();
        }
    }
    
    @Test
    public void testLargeVector() {
        double[] data = new double[1000];
        for (int i = 0; i < 1000; i++) {
            data[i] = i;
        }
        SharedVector large = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        
        assertEquals(1000, large.length());
        assertEquals(0.0, large.get(0), 0.001);
        assertEquals(999.0, large.get(999), 0.001);
    }
    
    @Test
    public void testDecimalValues() {
        SharedVector decimals = new SharedVector(new double[]{1.5, 2.7, 3.14159}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(1.5, decimals.get(0), 0.001);
        assertEquals(2.7, decimals.get(1), 0.001);
        assertEquals(3.14159, decimals.get(2), 0.001);
    }
    
    @Test
    public void testVeryLargeValues() {
        SharedVector large = new SharedVector(new double[]{1e10, 1e15, 1e20}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(1e10, large.get(0), 1e5);
        assertEquals(1e15, large.get(1), 1e10);
        assertEquals(1e20, large.get(2), 1e15);
    }
    
    @Test
    public void testVerySmallValues() {
        SharedVector small = new SharedVector(new double[]{1e-10, 1e-15, 1e-20}, VectorOrientation.ROW_MAJOR);
        
        assertEquals(1e-10, small.get(0), 1e-15);
        assertEquals(1e-15, small.get(1), 1e-20);
        assertEquals(1e-20, small.get(2), 1e-25);
    }
}
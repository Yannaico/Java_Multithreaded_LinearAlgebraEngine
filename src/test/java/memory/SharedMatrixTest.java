package memory;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {
    
    private double[][] testData;
    private SharedMatrix matrix;
    
    @BeforeEach
    public void setUp() {
        testData = new double[][]{
            {1, 2, 3},
            {4, 5, 6}
        };
        matrix = new SharedMatrix();
    }
    
    // ========== VALID CASES ==========
    
    @Test
    public void testLoadRowMajor_ValidMatrix() {
        matrix.loadRowMajor(testData);
        
        assertEquals(2, matrix.length());
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.get(0).getOrientation());
        
        assertEquals(1.0, matrix.get(0).get(0), 0.001);
        assertEquals(2.0, matrix.get(0).get(1), 0.001);
        assertEquals(3.0, matrix.get(0).get(2), 0.001);
        assertEquals(4.0, matrix.get(1).get(0), 0.001);
    }
    
    @Test
    public void testLoadColumnMajor_ValidMatrix() {
        matrix.loadColumnMajor(testData);
        
        assertEquals(3, matrix.length()); // 3 columns
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.get(0).getOrientation());
        
        // First column: [1, 4]
        assertEquals(1.0, matrix.get(0).get(0), 0.001);
        assertEquals(4.0, matrix.get(0).get(1), 0.001);
        
        // Second column: [2, 5]
        assertEquals(2.0, matrix.get(1).get(0), 0.001);
        assertEquals(5.0, matrix.get(1).get(1), 0.001);
    }
    
    @Test
    public void testReadRowMajor_AfterLoadRowMajor() {
        matrix.loadRowMajor(testData);
        double[][] result = matrix.readRowMajor();
        
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        
        assertArrayEquals(testData[0], result[0], 0.001);
        assertArrayEquals(testData[1], result[1], 0.001);
    }
    
    @Test
    public void testReadRowMajor_AfterLoadColumnMajor() {
        matrix.loadColumnMajor(testData);
        double[][] result = matrix.readRowMajor();
        
        // Should reconstruct original matrix
        assertEquals(2, result.length);
        assertEquals(3, result[0].length);
        
        assertArrayEquals(testData[0], result[0], 0.001);
        assertArrayEquals(testData[1], result[1], 0.001);
    }
    
    @Test
    public void testConstructorWithMatrix() {
        SharedMatrix m = new SharedMatrix(testData);
        
        assertEquals(2, m.length());
        assertEquals(1.0, m.get(0).get(0), 0.001);
        assertEquals(6.0, m.get(1).get(2), 0.001);
    }
    
    @Test
    public void testGetOrientation_RowMajor() {
        matrix.loadRowMajor(testData);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
    }
    
    @Test
    public void testGetOrientation_ColumnMajor() {
        matrix.loadColumnMajor(testData);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }
    
    // ========== INVALID CASES ==========
    
    @Test
    public void testLoadRowMajor_NullMatrix() {
        matrix.loadRowMajor(null);
        assertEquals(0, matrix.length());
    }
    
    @Test
    public void testLoadColumnMajor_NullMatrix() {
        matrix.loadColumnMajor(null);
        assertEquals(0, matrix.length());
    }
    
    @Test
    public void testGetOrientation_EmptyMatrix() {
        // With empty matrix, getOrientation should handle gracefully
        assertThrows(IllegalArgumentException.class, () -> matrix.getOrientation());
    }
    
    // ========== EDGE CASES ==========
    
    @Test
    public void testEmptyMatrix() {
        matrix.loadRowMajor(new double[0][0]);
        assertEquals(0, matrix.length());
        
        double[][] result = matrix.readRowMajor();
        assertEquals(0, result.length);
    }
    
    @Test
    public void testSingleElementMatrix_1x1() {
        double[][] single = {{42.0}};
        matrix.loadRowMajor(single);
        
        assertEquals(1, matrix.length());
        assertEquals(1, matrix.get(0).length());
        assertEquals(42.0, matrix.get(0).get(0), 0.001);
    }
    
    @Test
    public void testSingleElementMatrix_ReadBack() {
        double[][] single = {{42.0}};
        matrix.loadRowMajor(single);
        
        double[][] result = matrix.readRowMajor();
        assertEquals(1, result.length);
        assertEquals(1, result[0].length);
        assertEquals(42.0, result[0][0], 0.001);
    }
    
    @Test
    public void testSingleRowMatrix() {
        double[][] singleRow = {{1, 2, 3, 4, 5}};
        matrix.loadRowMajor(singleRow);
        
        assertEquals(1, matrix.length());
        assertEquals(5, matrix.get(0).length());
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(singleRow[0], result[0], 0.001);
    }
    
    @Test
    public void testSingleColumnMatrix() {
        double[][] singleCol = {{1}, {2}, {3}, {4}, {5}};
        matrix.loadRowMajor(singleCol);
        
        assertEquals(5, matrix.length());
        assertEquals(1, matrix.get(0).length());
        
        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < 5; i++) {
            assertEquals(singleCol[i][0], result[i][0], 0.001);
        }
    }
    
    @Test
    public void testSquareMatrix_2x2() {
        double[][] square = {{1, 2}, {3, 4}};
        matrix.loadRowMajor(square);
        
        assertEquals(2, matrix.length());
        assertEquals(2, matrix.get(0).length());
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(square[0], result[0], 0.001);
        assertArrayEquals(square[1], result[1], 0.001);
    }
    
    @Test
    public void testSquareMatrix_10x10() {
        double[][] large = new double[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                large[i][j] = i * 10 + j;
            }
        }
        
        matrix.loadRowMajor(large);
        assertEquals(10, matrix.length());
        assertEquals(10, matrix.get(0).length());
        
        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < 10; i++) {
            assertArrayEquals(large[i], result[i], 0.001);
        }
    }
    
    @Test
    public void testRectangularMatrix_TallAndThin() {
        double[][] tall = new double[100][2];
        for (int i = 0; i < 100; i++) {
            tall[i][0] = i;
            tall[i][1] = i * 2;
        }
        
        matrix.loadRowMajor(tall);
        assertEquals(100, matrix.length());
        assertEquals(2, matrix.get(0).length());
    }
    
    @Test
    public void testRectangularMatrix_ShortAndWide() {
        double[][] wide = new double[2][100];
        for (int j = 0; j < 100; j++) {
            wide[0][j] = j;
            wide[1][j] = j * 2;
        }
        
        matrix.loadRowMajor(wide);
        assertEquals(2, matrix.length());
        assertEquals(100, matrix.get(0).length());
    }
    
    @Test
    public void testMatrixWithZeros() {
        double[][] zeros = {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
        matrix.loadRowMajor(zeros);
        
        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(0.0, result[i][j], 0.001);
            }
        }
    }
    
    @Test
    public void testMatrixWithNegativeValues() {
        double[][] negative = {{-1, -2, -3}, {-4, -5, -6}};
        matrix.loadRowMajor(negative);
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(negative[0], result[0], 0.001);
        assertArrayEquals(negative[1], result[1], 0.001);
    }
    
    @Test
    public void testMatrixWithDecimalValues() {
        double[][] decimals = {{1.5, 2.7, 3.14}, {4.9, 5.1, 6.28}};
        matrix.loadRowMajor(decimals);
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(decimals[0], result[0], 0.001);
        assertArrayEquals(decimals[1], result[1], 0.001);
    }
    
    @Test
    public void testReloadMatrix_OverwritesPreviousData() {
        double[][] first = {{1, 2}, {3, 4}};
        double[][] second = {{5, 6, 7}, {8, 9, 10}};
        
        matrix.loadRowMajor(first);
        assertEquals(2, matrix.length());
        
        matrix.loadRowMajor(second);
        assertEquals(2, matrix.length());
        assertEquals(3, matrix.get(0).length());
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(second[0], result[0], 0.001);
        assertArrayEquals(second[1], result[1], 0.001);
    }
    
    @Test
    public void testLoadRowMajor_ThenColumnMajor_ThenReadBack() {
        double[][] original = {{1, 2, 3}, {4, 5, 6}};
        
        matrix.loadRowMajor(original);
        matrix.loadColumnMajor(original);
        
        double[][] result = matrix.readRowMajor();
        assertArrayEquals(original[0], result[0], 0.001);
        assertArrayEquals(original[1], result[1], 0.001);
    }
}
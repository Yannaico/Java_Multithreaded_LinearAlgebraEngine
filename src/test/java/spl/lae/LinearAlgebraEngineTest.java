package spl.lae;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraEngineTest {

    private LinearAlgebraEngine engine;
    // Default thread count for tests
    private static final int THREAD_COUNT = 4;

    @BeforeEach
    public void setUp() {
        engine = new LinearAlgebraEngine(THREAD_COUNT);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (engine != null) {
            engine.shutdown();
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Creates a leaf node containing a matrix.
     */
    private ComputationNode createMatrixNode(double[][] data) {
        return new ComputationNode(data);
    }

    /**
     * Creates an operation node.
     */
    private ComputationNode createOpNode(String op, ComputationNode... children) {
        return new ComputationNode(op, Arrays.asList(children));
    }

    // ========== VALID CASES ==========

    @Test
    @Timeout(5)
    public void testSimpleAddition_2x2() {
        double[][] dataA = {{1, 2}, {3, 4}};
        double[][] dataB = {{5, 6}, {7, 8}};
        
        ComputationNode nodeA = createMatrixNode(dataA);
        ComputationNode nodeB = createMatrixNode(dataB);
        ComputationNode root = createOpNode("+", nodeA, nodeB);

        ComputationNode resultNode = engine.run(root);
        double[][] result = resultNode.getMatrix();

        // Expected: {{6, 8}, {10, 12}}
        assertEquals(6.0, result[0][0], 0.001);
        assertEquals(8.0, result[0][1], 0.001);
        assertEquals(10.0, result[1][0], 0.001);
        assertEquals(12.0, result[1][1], 0.001);
    }

    @Test
    @Timeout(5)
    public void testSimpleMultiplication_RowByCol() {
        // A is 2x3, B is 3x2 -> Result is 2x2
        double[][] dataA = {
            {1, 2, 3},
            {4, 5, 6}
        };
        double[][] dataB = {
            {7, 8},
            {9, 1},
            {2, 3}
        };

        ComputationNode nodeA = createMatrixNode(dataA);
        ComputationNode nodeB = createMatrixNode(dataB);
        ComputationNode root = createOpNode("*", nodeA, nodeB);

        ComputationNode resultNode = engine.run(root);
        double[][] result = resultNode.getMatrix();

        // Math check:
        // C[0][0] = 1*7 + 2*9 + 3*2 = 7 + 18 + 6 = 31
        // C[0][1] = 1*8 + 2*1 + 3*3 = 8 + 2 + 9 = 19
        // C[1][0] = 4*7 + 5*9 + 6*2 = 28 + 45 + 12 = 85
        // C[1][1] = 4*8 + 5*1 + 6*3 = 32 + 5 + 18 = 55

        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(31.0, result[0][0], 0.001);
        assertEquals(19.0, result[0][1], 0.001);
        assertEquals(85.0, result[1][0], 0.001);
        assertEquals(55.0, result[1][1], 0.001);
    }

    @Test
    @Timeout(5)
    public void testNegateOperation() {
        double[][] data = {{1, -2}, {0, 5}};
        ComputationNode root = createOpNode("-", createMatrixNode(data));

        ComputationNode resultNode = engine.run(root);
        double[][] result = resultNode.getMatrix();

        assertEquals(-1.0, result[0][0], 0.001);
        assertEquals(2.0, result[0][1], 0.001);
        assertEquals(0.0, result[1][0], 0.001);
        assertEquals(-5.0, result[1][1], 0.001);
    }

    @Test
    @Timeout(5)
    public void testTransposeOperation() {
        double[][] data = {
            {1, 2, 3},
            {4, 5, 6}
        }; // 2x3
        ComputationNode root = createOpNode("T", createMatrixNode(data));

        ComputationNode resultNode = engine.run(root);
        double[][] result = resultNode.getMatrix();

        // Expect 3x2
        assertEquals(3, result.length);
        assertEquals(2, result[0].length);
        
        assertEquals(1.0, result[0][0], 0.001);
        assertEquals(4.0, result[0][1], 0.001);
        assertEquals(3.0, result[2][0], 0.001);
        assertEquals(6.0, result[2][1], 0.001);
    }

    
    @Test
    @Timeout(5)
    public void testMixedOperations_Integration() {
        // Calculation: (A + B)^T
        double[][] A = {{1, 2}};
        double[][] B = {{3, 4}};
        
        ComputationNode sumNode = createOpNode("+", createMatrixNode(A), createMatrixNode(B));
        ComputationNode root = createOpNode("T", sumNode);

        ComputationNode resultNode = engine.run(root);
        double[][] result = resultNode.getMatrix();

        // A+B = {{4, 6}} (1x2)
        // (A+B)^T = {{4}, {6}} (2x1)
        assertEquals(2, result.length);
        assertEquals(1, result[0].length);
        assertEquals(4.0, result[0][0], 0.001);
        assertEquals(6.0, result[1][0], 0.001);
    }

    @Test
    @Timeout(5)
    public void testReuseEngine_MultipleRuns() {
        // Ensure the engine is not "one-shot" and clears internal state properly
        double[][] data = {{1}};
        
        // Run 1: Addition
        ComputationNode root1 = createOpNode("+", createMatrixNode(data), createMatrixNode(data));
        engine.run(root1); // 1 + 1 = 2
        assertEquals(2.0, root1.getMatrix()[0][0], 0.001);

        // Run 2: Multiplication
        ComputationNode root2 = createOpNode("*", createMatrixNode(data), createMatrixNode(data));
        engine.run(root2); // 1 * 1 = 1
        assertEquals(1.0, root2.getMatrix()[0][0], 0.001);
    }

    // ========== INVALID CASES ==========

    @Test
    public void testRun_NullRoot_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> engine.run(null));
    }

    @Test
    public void testAddition_DimensionMismatch() {
        double[][] A = {{1, 2}}; // 1x2
        double[][] B = {{1, 2, 3}}; // 1x3
        
        ComputationNode root = createOpNode("+", createMatrixNode(A), createMatrixNode(B));
        
        // The implementation calls createAddTasks which checks dimensions
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    public void testMultiplication_DimensionMismatch() {
        double[][] A = {{1, 2}}; // 1x2
        double[][] B = {{1, 2}}; // 1x2 (Rows of B != Cols of A)
        
        ComputationNode root = createOpNode("*", createMatrixNode(A), createMatrixNode(B));
        
        // SharedVector.vecMatMul checks dimensions
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    public void testUnaryOperator_WithTwoOperands_ThrowsException() {
        double[][] A = {{1}};
        // Transpose should have 1 child, giving 2
        ComputationNode root = createOpNode("T", createMatrixNode(A), createMatrixNode(A));
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    public void testBinaryOperator_WithOneOperand_ThrowsException() {
        double[][] A = {{1}};
        // Add should have >= 2 children
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, Arrays.asList(createMatrixNode(A)));
        
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    // ========== EDGE CASES ==========

    @Test
    @Timeout(5)
    public void testSingleElementMatrix() {
        // 1x1 Matrix operations
        double[][] A = {{10.0}};
        double[][] B = {{2.0}};
        
        ComputationNode root = createOpNode("*", createMatrixNode(A), createMatrixNode(B));
        ComputationNode result = engine.run(root);
        
        assertEquals(20.0, result.getMatrix()[0][0], 0.001);
    }

    @Test
    @Timeout(5)
    public void testIdentityMatrixMultiplication() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] I = {{1, 0}, {0, 1}};
        
        ComputationNode root = createOpNode("*", createMatrixNode(A), createMatrixNode(I));
        ComputationNode result = engine.run(root);
        
        double[][] res = result.getMatrix();
        assertArrayEquals(A[0], res[0], 0.001);
        assertArrayEquals(A[1], res[1], 0.001);
    }

    @Test
    @Timeout(5)
    public void testZeroMatrixAddition() {
        double[][] A = {{5, 5}, {5, 5}};
        double[][] Z = {{0, 0}, {0, 0}};
        
        ComputationNode root = createOpNode("+", createMatrixNode(A), createMatrixNode(Z));
        ComputationNode result = engine.run(root);
        
        double[][] res = result.getMatrix();
        assertArrayEquals(A[0], res[0], 0.001);
        assertArrayEquals(A[1], res[1], 0.001);
    }

    @Test
    @Timeout(5)
    public void testRootIsAlreadyMatrix() {
        // Case where input JSON is just a matrix, no operators
        double[][] A = {{1, 2}};
        ComputationNode root = createMatrixNode(A);
        
        ComputationNode result = engine.run(root);
        
        // Should return immediately without using executor
        assertSame(root, result);
        assertArrayEquals(A[0], result.getMatrix()[0], 0.001);
    }

    // ========== CONCURRENCY / STRESS ==========

    @Test
    @Timeout(10)
    public void testLargeComputation() {
        // Not "Big Data" large, but enough to ensure threads actually context switch
        int size = 50;
        double[][] A = new double[size][size];
        double[][] B = new double[size][size];
        
        for(int i=0; i<size; i++) {
            for(int j=0; j<size; j++) {
                A[i][j] = 1.0;
                B[i][j] = 1.0;
            }
        }
        
        ComputationNode root = createOpNode("+", createMatrixNode(A), createMatrixNode(B));
        ComputationNode result = engine.run(root);
        
        assertEquals(2.0, result.getMatrix()[0][0], 0.001);
        assertEquals(2.0, result.getMatrix()[size-1][size-1], 0.001);
    }

    @Test
    public void testConstructor_ZeroThreads_ThrowsException() {
        // TiredExecutor inside LAE throws IllegalArgumentException for <= 0 threads
        assertThrows(IllegalArgumentException.class, () -> new LinearAlgebraEngine(0));
    }
}
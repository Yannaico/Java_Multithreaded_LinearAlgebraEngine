package memory;

import java.util.concurrent.locks.ReadWriteLock;
public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        readLock();
        try{
            if(index < 0)
                throw new RuntimeException("Index not valid");
            return vector[index];
        }
        finally{
            readUnlock();
        }
    }

    public int length() {
       return vector.length;
    }

    public VectorOrientation getOrientation() {
       return orientation;
    }

    public void writeLock() {
        this.lock.writeLock().lock();;
    }

    public void writeUnlock() {
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        this.lock.readLock().unlock();
    }

    //Assumes caller holds write lock.
    public void transpose() {
    if(this.orientation == VectorOrientation.ROW_MAJOR)
        this.orientation = VectorOrientation.COLUMN_MAJOR;
    else
        this.orientation = VectorOrientation.ROW_MAJOR;
}
    //Assumes caller holds write lock on this and read lock on other.
    public void add(SharedVector other) {
        if(other == null){
            throw new IllegalArgumentException("Other vector is null");
        }
       /*  if (this.orientation != other.getOrientation()) {
            throw new IllegalArgumentException("Cannot add vectors with different orientations");
        }*/
        if (this.vector.length != other.vector.length) {
             throw new IllegalArgumentException("Vector lengths do not match");
        }
        
        for(int i=0;i<vector.length;i++){
            this.vector[i]+=other.get(i); 
        }
    }

    public void negate() {
        for(int i=0;i<vector.length;i++)
        {
            vector[i]*=-1;
        }
    }

    //Assumes caller holds read locks on both vectors.
    public double dot(SharedVector other) {
        double sum=0;

        if (this.orientation == other.getOrientation()) 
        {
            throw new IllegalArgumentException("Cannot Multiply vectors with same orientations");
        }
        if(this.length()!= other.length())
        {
             throw new IllegalArgumentException("Vector lengths do not match");
        }

        for (int i = 0; i < this.vector.length; i++) {
            sum += this.vector[i] * other.vector[i];
        }
        return sum;
    }

    public void vecMatMul(SharedMatrix matrix) {
        /*
        [x y z]    [a b c] [x]
        [a b c]            [a]
        [d e f]            [d]

        
        
        */
        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("Vector must be in ROW_MAJOR orientation for vec-mat multiplication");
        }

        int matrixRows = matrix.length();

        if (matrixRows == 0) {
            throw new IllegalArgumentException("Matrix is empty");
        }
        if(vector.length != matrixRows) {
            throw new IllegalArgumentException("Vector length must match matrix row count");
        }

        int matrixCols = matrix.get(0).length();
        double[] result = new double[matrixCols];

        for(int i=0;i<matrixCols;i++){
            double sum=0;
            for(int j=0;j<matrixRows;j++){
                sum+= this.vector[j] * matrix.get(j).get(i);
            }
            result[i]=sum;
        }
        this.vector = result;
    }
}
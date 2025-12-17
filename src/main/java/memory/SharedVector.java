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
            readLock();
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

    public void transpose() {
        writeLock();
        try{
            if(this.orientation == VectorOrientation.ROW_MAJOR)
                this.orientation = VectorOrientation.COLUMN_MAJOR;
            else
                this.orientation = VectorOrientation.ROW_MAJOR;
        }
        finally{
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        if (this.orientation != other.getOrientation()) {
            throw new IllegalArgumentException("Cannot add vectors with different orientations");
        }
        if (this.vector.length != other.vector.length) {
             throw new IllegalArgumentException("Vector lengths do not match");
        }
        
        writeLock();
        try{
            other.readLock();
            try{
                for(int i =0;i<vector.length;i++)
                {
                    vector[i]+=other.get(i);
                }
            }
            finally{
                other.readUnlock();
            }
        }
        finally{
            writeUnlock();
        }
    }

    public void negate() {
        writeLock();
        try{
            for(int i =0;i<vector.length;i++)
            {
                vector[i]*=-1;
            }
        }
        finally{
            writeUnlock();
        }
    }

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
        
        for(int i=0;i<this.vector.length;i++)
        {
            sum+=this.get(i) * other.get(i);


        }
        return sum;
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (this.orientation == matrix.getOrientation()) 
        {
            throw new IllegalArgumentException("Cannot Multiply vectors with same orientations");
        }
        if(matrix.length()==0 || this.length()!= matrix.get(0).length())
        {
              throw new IllegalArgumentException("Matrix length is not compatible with the vector");

        }

        writeLock();

        try{
            matrix.acquireAllVectorReadLocks();
            for(int i=0 ;i< matrix.length();i++)
            {
                SharedVector current = matrix.get(i);
                vector[i] = dot(current);
            }
        }
        finally{
            matrix.r
        }
        


    }
}

package util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class IntegerArrayList implements IntegerList, Cloneable, Serializable {

    /**
     * Default initial capacity.
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private static final long serialVersionUID = -4385441882485047565L;

    private transient int[] data;

    /**
     * The size of this ArrayList.
     */
    private int size;

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public IntegerArrayList() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    public IntegerArrayList(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        this.data = new int[initialCapacity];
    }

    private void ensureCapacity(int minCapacity) {
        int oldCapacity = data.length;
        if(minCapacity > oldCapacity) {
            int newCapacity = oldCapacity + (oldCapacity >> 1); //grow rate
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity - MAX_ARRAY_SIZE > 0)
                newCapacity = hugeCapacity(minCapacity);
            // minCapacity is usually close to size, so this is a win:
            data = Arrays.copyOf(data, newCapacity);
        }
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError("IntegerArrayList too big");
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Appends the specified value to the end of this list.
     *
     * @param value value to be appended to this list
     */
    @Override
    public void add(int value) {
        ensureCapacity(size + 1);
        data[size++] = value;
    }

    /**
     * Inserts the specified value at the specified position in this
     * list. Shifts the value currently at that position (if any) and
     * any subsequent values to the right (adds one to their indices).
     *
     * @param index index at which the specified value is to be inserted
     * @param value value to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public void add(int index, int value) {
        rangeCheckForAdd(index);

        ensureCapacity(size + 1);  // Increments modCount!!
        System.arraycopy(data, index, data, index + 1, size - index);
        data[index] = value;
        size++;
    }

    @Override
    public int get(int index) {
        rangeCheck(index);

        return data[index];
    }

    @Override
    public int set(int index, int value) {
        rangeCheck(index);

        int oldValue = data[index];
        data[index] = value;
        return oldValue;
    }

    /**
     * Removes the value at the specified position in this list.
     * Shifts any subsequent values to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the value to be removed
     * @return the value that was removed from the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    @Override
    public int remove(int index) {
        rangeCheck(index);

        int oldValue = data[index];

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(data, index + 1, data, index, numMoved);
        size--;
        return oldValue;
    }

    public void trimToSize(int size) {
        data = Arrays.copyOf(data, size);
        this.size = size;
    }

    private void rangeCheck(int index) {
        if(index >= size)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
    }

    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerArrayList that = (IntegerArrayList) o;
        if (size != that.size) return false;
        if (data == that.data) return true;
        if (data == null || that.data == null) return false;
        //Don't compare lengths, because they can be different but with same number of elements according to size
        assert(size <= data.length);
        assert(size <= that.data.length);
        for (int i=0; i<size; i++)
            if (data[i] != that.data[i])
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(size);
        //result = 31 * result + Arrays.hashCode(data);
        result = 31 * result;
        int tempResult = 0;
        if(data != null){
            tempResult = 1;
            for (int i = 0; i < size; i++)
                tempResult = 31 * tempResult + data[i];
        }
        return result + tempResult;
    }

    /**
     * Returns a deep copy of this <tt>IntegerArrayList</tt> instance.
     *
     * @return a clone of this <tt>IntegerArrayList</tt> instance
     */
    @Override
    public IntegerArrayList clone() {
        try {
            IntegerArrayList copy = (IntegerArrayList) super.clone();
            copy.data = Arrays.copyOf(data, size);
            return copy;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    @Override
    public String toString() {
        return "IntegerArrayList{" +
                "data=" + Arrays.toString(data) +
                ", size=" + size +
                '}';
    }

    /**
     * Serialize this {@code IntegerArrayList} instance.
     *
     * @serialData The size of the list (the number of values
     * it contains) is emitted ({@code int}), followed by all of
     * its values (each a {@code int}), in the proper
     * sequence.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        //Write out element count, and any hidden stuff
        s.defaultWriteObject();

        //Write out size as capacity for behavioural compatibility with clone()
        s.writeInt(size);

        //Write out all elements in the proper order.
        for(int i=0; i<size; i++)
            s.writeInt(data[i]);
    }

    /**
     * Reconstitute the <tt>IntegerArrayList</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        data = new int[DEFAULT_CAPACITY];

        //Read in size, and any hidden stuff
        s.defaultReadObject();

        //Read in capacity
        s.readInt(); //ignored

        if (size > 0) {
            //be like clone(), allocate array based upon size not capacity
            ensureCapacity(size);

            int[] a = data;
            //Read in all elements in the proper order.
            for (int i=0; i<size; i++) {
                a[i] = s.readInt();
            }
        }
    }
}

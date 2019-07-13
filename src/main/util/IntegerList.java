package util;

public interface IntegerList {

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this list
     */
    int size();

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    boolean isEmpty();

    /**
     * Appends the specified element to the end of this list.
     *
     * @param value element to be appended to this list
     */
    void add(int value);

    /**
     * Inserts the specified value at the specified position in this list.
     *
     * @param index index at which the specified value is to be inserted
     * @param value value to be inserted
     * @throws IndexOutOfBoundsException if index exceed this list size
     */
    void add(int index, int value);

    /**
     * Returns the value at the specified position in this list.
     *
     * @param index index of the value to return
     * @return the value at the specified position in this list
     * @throws IndexOutOfBoundsException if index exceed this list size
     */
    int get(int index);

    /**
     * Replaces the value at the specified position in this list with the
     * provided value.
     *
     * @param index index of the value to replace
     * @param value value to be stored at the specified position
     * @return the value previously at the specified position
     * @throws IndexOutOfBoundsException if index exceed this list size
     */
    int set(int index, int value);

    /**
     * Removes the value at the specified position from this list.
     *
     * @param index value at the index position to be removed from this list.
     * @return the value that was removed from this list.
     * @throws IndexOutOfBoundsException if index exceed this list size
     */
    int remove(int index);

    void trimToSize(int size);

}

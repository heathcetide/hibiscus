package hibiscus.cetide.app.util;

import java.io.Serializable;
import java.util.Arrays;

public class HibiscusArrayList<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Object[] EMPTY_ELEMENTDATA = {};

    private static final Object[] DEFAULT_EMPTY_ELEMENTDATA = {};

    private Object[] elementData;

    private int size;

    protected transient int modCount = 0;


    private static final int DEFAULT_CAPACITY = 10;

    public HibiscusArrayList(int initialCapacity){
        if (initialCapacity > 0){
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0){
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
    }

    public HibiscusArrayList(){
        this.elementData = DEFAULT_EMPTY_ELEMENTDATA;
    }

    public boolean add(E e){
        modCount++;
        add(e, elementData, size);
        return true;
    }

    private void add(E e, Object[] elementData, int s){
        if (s == elementData.length){
            elementData = grow();
        }
        elementData[s] = e;
        size = s + 1;
    }

    private Object[] grow(){
        return grow(size + 1);
    }

    private Object[] grow(int minCapacity) {
        int oldCapacity = elementData.length;
        if (oldCapacity > 0 || elementData != DEFAULT_EMPTY_ELEMENTDATA){
            // 使用 Arrays.copyOf 扩容数组
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            return Arrays.copyOf(elementData, newCapacity);
        } else {
            return elementData = new Object[Math.max(DEFAULT_CAPACITY, minCapacity)];
        }
    }

    public static void main(String[] args) {
        HibiscusArrayList<String> list = new HibiscusArrayList<>();
        long startTime = System.nanoTime();
        list.add("test");
        System.out.println("Time taken: " + (System.nanoTime() - startTime) / 1e6 + " ms");
    }
}

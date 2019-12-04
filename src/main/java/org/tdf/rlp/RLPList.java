package org.tdf.rlp;

import lombok.NonNull;
import lombok.experimental.Delegate;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.tdf.rlp.RLPConstants.*;

public final class RLPList implements RLPElement, List<RLPElement> {
    public static RLPList of(RLPElement... elements){
        return new RLPList(Arrays.asList(elements));
    }

    public static RLPList fromElements(Collection<RLPElement> elements){
        return new RLPList(elements.stream().collect(Collectors.toList()));
    }

    public static RLPList createEmpty(){
        return new RLPList();
    }

    public static RLPList createEmpty(int cap){
        return new RLPList(new ArrayList<>(cap));
    }

    public List<RLPElement> elements = new ArrayList<>();

    private RLPList(){}

    RLPList(List<RLPElement> elements){
        this.elements = elements;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public RLPList getAsList() {
        return this;
    }

    @Override
    public RLPItem getAsItem() {
        throw new RuntimeException("not a rlp item");
    }

    @Override
    public byte[] getEncoded() {
        return encodeList(stream().map(RLPElement::getEncoded).collect(Collectors.toList()));
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public static byte[] encodeList(@NonNull Collection<byte[]> elements) {
        int totalLength = 0;
        for (byte[] element1 : elements) {
            totalLength += element1.length;
        }

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (byte[] element : elements) {
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Iterator<RLPElement> iterator() {
        return elements.iterator();
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return elements.toArray(a);
    }

    @Override
    public boolean add(RLPElement rlpElement) {
        return elements.add(rlpElement);
    }

    @Override
    public boolean remove(Object o) {
        return elements.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends RLPElement> c) {
        return elements.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends RLPElement> c) {
        return elements.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return elements.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return elements.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<RLPElement> operator) {
        elements.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super RLPElement> c) {
        elements.sort(c);
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public boolean equals(Object o) {
        return elements.equals(o);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public RLPElement get(int index) {
        return elements.get(index);
    }

    @Override
    public RLPElement set(int index, RLPElement element) {
        return elements.set(index, element);
    }

    @Override
    public void add(int index, RLPElement element) {
        elements.add(index, element);
    }

    @Override
    public RLPElement remove(int index) {
        return elements.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return elements.lastIndexOf(o);
    }

    @Override
    public ListIterator<RLPElement> listIterator() {
        return elements.listIterator();
    }

    @Override
    public ListIterator<RLPElement> listIterator(int index) {
        return elements.listIterator(index);
    }

    @Override
    public List<RLPElement> subList(int fromIndex, int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<RLPElement> spliterator() {
        return elements.spliterator();
    }
}

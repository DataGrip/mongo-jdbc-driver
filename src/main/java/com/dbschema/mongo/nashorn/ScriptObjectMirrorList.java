package com.dbschema.mongo.nashorn;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Liudmila Kornilova
 **/
public class ScriptObjectMirrorList implements List<Object> {
  private final ScriptObjectMirror objectMirror;

  public ScriptObjectMirrorList(@NotNull ScriptObjectMirror objectMirror) {
    assert objectMirror.isArray();
    this.objectMirror = objectMirror;
  }

  @Override
  public int size() {
    return objectMirror.size();
  }

  @Override
  public boolean isEmpty() {
    return objectMirror.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return objectMirror.containsValue(o);
  }

  @NotNull
  @Override
  public Iterator<Object> iterator() {
    int[] current = new int[1];

    return new Iterator<Object>() {
      @Override
      public boolean hasNext() {
        return current[0] < objectMirror.size();
      }

      @Override
      public Object next() {
        return get(current[0]++);
      }
    };
  }

  @NotNull
  @Override
  public Object[] toArray() {
    Object[] array = new Object[objectMirror.size()];
    for (int i = 0; i < objectMirror.size(); i++) {
      array[i] = objectMirror.get(Integer.toString(i));
    }
    return array;
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] a) {
    //noinspection unchecked
    return (T[]) toArray();
  }

  @Override
  public boolean add(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return objectMirror.hashCode();
  }

  @Override
  public Object get(int index) {
    String key = Integer.toString(index);
    if (!objectMirror.containsKey(key)) throw new IndexOutOfBoundsException(key);
    return objectMirror.get(key);
  }

  @Override
  public Object set(int index, Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public ListIterator<Object> listIterator() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public ListIterator<Object> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}

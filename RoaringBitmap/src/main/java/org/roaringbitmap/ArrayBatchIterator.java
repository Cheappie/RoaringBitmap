package org.roaringbitmap;


public final class ArrayBatchIterator implements ContainerBatchIterator {

  private int index = 0;
  private ArrayContainer array;

  public ArrayBatchIterator(ArrayContainer array) {
    wrap(array);
  }

  @Override
  public int next(int key, int[] buffer) {
    int consumed = 0;
    char[] data = array.content;
    while (consumed < buffer.length && index < array.getCardinality()) {
      buffer[consumed++] = key + (data[index++]);
    }
    return consumed;
  }

  @Override
  public boolean hasNext() {
    return index < array.getCardinality();
  }

  @Override
  public ContainerBatchIterator clone() {
    try {
      return (ContainerBatchIterator) super.clone();
    } catch (CloneNotSupportedException e) {
      // won't happen
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void releaseContainer() {
    array = null;
  }

  void wrap(ArrayContainer array) {
    this.array = array;
    this.index = 0;
  }

  /**
   * Advance iterator such that next value will be greater or equal to minVal
   *
   * @param minVal - expected minimal value
   */
  public void advanceIfNeeded(char minVal) {
    int newIndex = Util.advanceUntil(array.content, index - 1, array.getCardinality(), minVal);
    if (index < newIndex) {
      index = newIndex;
    }
  }
}

package org.roaringbitmap;

public final class RoaringBatchIterator implements BatchIterator {

  private ContainerPointer containerPointer;
  private int key;

  private ContainerBatchIterator iterator;
  private ArrayBatchIterator arrayBatchIterator = null;
  private BitmapBatchIterator bitmapBatchIterator = null;
  private RunBatchIterator runBatchIterator = null;

  public RoaringBatchIterator(RoaringArray highLowContainer) {
    this.containerPointer = highLowContainer.getContainerPointer();
    nextContainer();
  }

  @Override
  public int nextBatch(int[] buffer) {
    if (!hasNext()) {
      return 0;
    }

    int consumed = 0;
    if (iterator.hasNext()) {
      consumed += iterator.next(key, buffer);
      if (consumed > 0) {
        return consumed;
      }
    }

    nextIterator();
    if (null != iterator) {
      return nextBatch(buffer);
    }
    return consumed;
  }

  @Override
  public boolean hasNext() {
    return null != iterator;
  }

  @Override
  public BatchIterator clone() {
    try {
      RoaringBatchIterator it = (RoaringBatchIterator) super.clone();
      if (null != iterator) {
        it.containerPointer = containerPointer.clone();
        it.iterator = iterator.clone();
      }
      return it;
    } catch (CloneNotSupportedException e) {
      // won't happen
      throw new IllegalStateException();
    }
  }

  private void nextIterator() {
    if (null != iterator) {
      iterator.releaseContainer();
    }

    containerPointer.advance();
    nextContainer();
  }

  private void nextContainer() {
    Container container = containerPointer.getContainer();
    if (container != null) {
      if (container instanceof ArrayContainer) {
        nextIterator((ArrayContainer) container);
      } else if (container instanceof BitmapContainer) {
        nextIterator((BitmapContainer) container);
      } else if (container instanceof RunContainer) {
        nextIterator((RunContainer) container);
      }

      key = containerPointer.key() << 16;
    } else {
      iterator = null;
    }
  }

  private void nextIterator(ArrayContainer array) {
    if (null == arrayBatchIterator) {
      arrayBatchIterator = new ArrayBatchIterator(array);
    } else {
      arrayBatchIterator.wrap(array);
    }
    iterator = arrayBatchIterator;
  }

  private void nextIterator(BitmapContainer bitmap) {
    if (null == bitmapBatchIterator) {
      bitmapBatchIterator = new BitmapBatchIterator(bitmap);
    } else {
      bitmapBatchIterator.wrap(bitmap);
    }
    iterator = bitmapBatchIterator;
  }

  private void nextIterator(RunContainer run) {
    if (null == runBatchIterator) {
      runBatchIterator = new RunBatchIterator(run);
    } else {
      runBatchIterator.wrap(run);
    }
    iterator = runBatchIterator;
  }

  /**
   * Advance iterator such that next value will be greater or equal to minval if iterator wasn't
   * exhausted.
   *
   * @param minval - expected minimal value
   */
  public void advanceIfNeeded(int minval) {
    while (hasNext() && ((key >>> 16) < (minval >>> 16))) {
      nextIterator();
    }

    if (hasNext() && ((key >>> 16) == (minval >>> 16))) {
      advanceIteratorIfNeeded(minval);
    }
  }

  private void advanceIteratorIfNeeded(int minval) {
    if (iterator instanceof BitmapBatchIterator) {
      ((BitmapBatchIterator) iterator).advanceIfNeeded(Util.lowbits(minval));
    } else if (iterator instanceof ArrayBatchIterator) {
      ((ArrayBatchIterator) iterator).advanceIfNeeded(Util.lowbits(minval));
    } else if (iterator instanceof RunBatchIterator) {
      ((RunBatchIterator) iterator).advanceIfNeeded(Util.lowbits(minval));
    } else {
      throw new IllegalArgumentException("Unsupported container type");
    }
  }
}

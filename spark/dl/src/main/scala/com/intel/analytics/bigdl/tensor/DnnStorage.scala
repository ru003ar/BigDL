/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.bigdl.tensor

import com.intel.analytics.bigdl.mkl.Memory

import scala.reflect._

/**
 * Represent a native array which is needed by mkl-dnn
 * @param size Storage size
 * @tparam T data type, only support float now
 */
private[tensor] class DnnStorage[T: ClassTag](size: Int) extends Storage[T] {

  require(classTag[T] == ClassTag.Float, "DnnStorage only support float")

  private var _isReleased: Boolean = false

  // Hold the address of the native array
  val ptr: Pointer = new Pointer(allocate(size))

  override def length(): Int = size

  override def apply(index: Int): T =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  /**
   * Set the element at position index in the storage. Valid range of index is 1 to length()
   *
   * @param index
   * @param value
   */
  override def update(index: Int, value: T): Unit =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  override def copy(source: Storage[T], offset: Int, sourceOffset: Int, length: Int)
  : this.type = {
    source match {
      case s: ArrayStorage[T] =>
        Memory.CopyArray2Ptr(s.array().asInstanceOf[Array[Float]], sourceOffset,
          ptr.address, offset, length, DnnStorage.FLOAT_BYTES)
      case s: DnnStorage[T] =>
        Memory.CopyPtr2Ptr(s.ptr.address, sourceOffset, ptr.address, offset, length,
          DnnStorage.FLOAT_BYTES)
      case _ =>
        throw new UnsupportedOperationException("Only support copy from ArrayStorage or DnnStorage")
    }
    this
  }

  override def fill(value: T, offset: Int, length: Int): DnnStorage.this.type =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  override def resize(size: Long): DnnStorage.this.type =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  override def array(): Array[T] =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  override def set(other: Storage[T]): DnnStorage.this.type =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  override def iterator: Iterator[T] =
    throw new UnsupportedOperationException("Not support this operation in DnnStorage")

  /**
   * Release the native array, the storage object is useless
   */
  def release(): Unit = {
    Memory.AlignedFree(ptr.address)
    _isReleased = true
  }

  def isReleased(): Boolean = _isReleased


  private def allocate(capacity: Int): Long = {
    require(capacity > 0, s"capacity should not be larger than 0")
    val ptr = Memory.AlignedMalloc(capacity * DnnStorage.FLOAT_BYTES, DnnStorage.CACHE_LINE_SIZE)
    require(ptr != 0L, s"allocate native aligned memory failed")
    ptr
  }
}

/**
 * Represent a native point
 * @param address
 */
private[bigdl] class Pointer(val address: Long)

object DnnStorage {
  private[tensor] val CACHE_LINE_SIZE = System.getProperty("bigdl.cache.line", "64").toInt
  private[tensor] val FLOAT_BYTES: Int = 4
}

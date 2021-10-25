/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/** Simple util to create a serialized object of the PayloadClassLoader class. */
class PayloadClassLoaderSerializer {
  public static void main(String[] args) {
    try {
      ObjectOutputStream objectOutputStream =
          new ObjectOutputStream(new FileOutputStream("com/bdjb/PayloadClassLoader.ser"));
      objectOutputStream.writeObject(new PayloadClassLoader());
      objectOutputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

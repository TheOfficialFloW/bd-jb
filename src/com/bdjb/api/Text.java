/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public class Text extends Buffer {
  private String text;

  public Text(String text) {
    super(text.length() + 1);
    this.text = text;
    api.strcpy(address(), text);
  }

  public String toString() {
    return text;
  }
}

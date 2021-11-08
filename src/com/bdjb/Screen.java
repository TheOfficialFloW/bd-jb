/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public class Screen extends Container {
  private static final long serialVersionUID = 0x4141414141414141L;

  private static final Font FONT = new Font(null, Font.PLAIN, 40);

  private static final ArrayList messages = new ArrayList();

  private static final Screen instance = new Screen();

  public static Screen getInstance() {
    return instance;
  }

  public static void println(String msg) {
    messages.add(msg);
    instance.repaint();
  }

  public void paint(Graphics g) {
    g.setFont(FONT);
    g.setColor(Color.WHITE);

    int x = 80;
    int y = 80;
    int height = g.getFontMetrics().getHeight();
    for (int i = 0; i < messages.size(); i++) {
      String msg = (String) messages.get(i);
      g.drawString(msg, x, y);
      y += height;
    }
  }
}

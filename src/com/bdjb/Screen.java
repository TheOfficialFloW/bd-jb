/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.lang.reflect.Method;

public class Screen extends Container {
  private static final long serialVersionUID = 0x4141414141414141L;

  private static final int MAX_LINES = 32;

  private static final Font FONT = new Font(null, Font.PLAIN, 32);

  private static final String[] lines = new String[MAX_LINES];

  private static final Screen instance = new Screen();

  private static Method remoteScreenPrintln = null;

  private static int currentLine = 0;

  public static Screen getInstance() {
    return instance;
  }

  public static void setRemotePrintln(Method screenPrintln) {
    remoteScreenPrintln = screenPrintln;
  }

  public static synchronized void println(String msg) {
    if (remoteScreenPrintln != null) {
      try {
        remoteScreenPrintln.invoke(null, new Object[] {msg});
      } catch (Exception e) {
        // Ignore.
      }
    } else {
      lines[currentLine] = msg;
      currentLine = (currentLine + 1) % MAX_LINES;
      instance.repaint();
    }
  }

  public void paint(Graphics g) {
    g.setFont(FONT);
    g.setColor(Color.WHITE);
    g.clearRect(0, 0, getWidth(), getHeight());

    int x = 64;
    int y = 64;
    int height = g.getFontMetrics().getHeight();
    for (int i = 0; i < lines.length; i++) {
      g.drawString(lines[i], x, y + i * height);
    }
  }
}

/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

public class LoaderXlet implements Xlet {
  private HScene scene;
  private Screen screen;

  public void initXlet(XletContext context) {
    screen = Screen.getInstance();
    screen.setSize(1920, 1080); // BD screen size

    scene = HSceneFactory.getInstance().getDefaultHScene();
    scene.add(screen, BorderLayout.CENTER);
    scene.validate();
  }

  public void startXlet() {
    screen.setVisible(true);
    scene.setVisible(true);
    Loader.startJarLoader();
  }

  public void pauseXlet() {
    screen.setVisible(false);
  }

  public void destroyXlet(boolean unconditional) {
    scene.remove(screen);
    scene = null;
  }
}

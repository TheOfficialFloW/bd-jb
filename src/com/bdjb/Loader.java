/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.lang.reflect.Method;
import org.dvb.lang.DVBClassLoader;

class Loader implements Runnable {
  private static final String MNT_ADA_JAR_FILE = "/OS/HDD/download0/mnt_ada/00000.jar";

  private static final String EXPLOIT_CLASS_NAME = "com.bdjb.Exploit";
  private static final String MAIN_METHOD_NAME = "main";
  private static final String PRINTLN_METHOD_NAME = "println";

  static void startJarLoader() {
    new Thread(new Loader()).start();
  }

  public void run() {
    Screen.println("[+] bd-jb by theflow");

    while (true) {
      Screen.println("[*] Listening for remote JAR on port 9025...");

      try {
        ServerSocket serverSocket = new ServerSocket(9025);
        Socket socket = serverSocket.accept();
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = new FileOutputStream(MNT_ADA_JAR_FILE);

        byte[] buf = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buf)) > 0) {
          outputStream.write(buf, 0, read);
          total += read;
        }

        outputStream.close();
        inputStream.close();

        serverSocket.close();

        Screen.println("[+] Received " + total + " bytes");

        Screen.println("[+] Launching JAR...");

        DVBClassLoader dvbClassLoader =
            DVBClassLoader.newInstance(new URL[] {new URL("file://" + MNT_ADA_JAR_FILE)});
        Class exploitClass = dvbClassLoader.loadClass(EXPLOIT_CLASS_NAME);
        Method main = exploitClass.getMethod(MAIN_METHOD_NAME, new Class[] {Method.class});
        Method screenPrintln =
            Screen.class.getMethod(PRINTLN_METHOD_NAME, new Class[] {String.class});
        main.invoke(null, new Object[] {screenPrintln});

        Screen.println("[+] JAR exited");
      } catch (Exception e) {
        Screen.println("[-] Error: " + e.getMessage());
      }
    }
  }
}

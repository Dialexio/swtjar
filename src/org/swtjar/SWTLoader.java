/*******************************************************************************
* Copyright (c) 2011-2012 mchr3k
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* mchr3k - initial API and implementation
*******************************************************************************/
package org.swtjar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.*;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.eclipse.jdt.internal.jarinjarloader.RsrcURLStreamHandlerFactory;

public class SWTLoader
{
  public static final String SWTJAR_MAIN_CLASS = "SwtJar-TargetMainClass";
  public static final String SWTJAR_VERSION = "SwtJar-SwtVersion";

  private static String sTargetMainClass = null;
  private static String sSwtVersion = null;

  public static void main(String[] args) throws Throwable
  {
    try
    {
      loadConfig();
      ClassLoader cl = getSWTClassloader();
      Thread.currentThread().setContextClassLoader(cl);
      try
      {
        try
        {
          //System.err.println("Launching UI ...");
          Class<?> c = Class.forName(sTargetMainClass, true, cl);
          Method main = c.getMethod("main", new Class[]{args.getClass()});
          main.invoke((Object)null, new Object[]{args});
        }
        catch (InvocationTargetException ex)
        {
          Throwable th = ex.getCause();
          if (th instanceof UnsatisfiedLinkError)
          {
            UnsatisfiedLinkError linkError = (UnsatisfiedLinkError)th;
            String errorMessage = "(UnsatisfiedLinkError: " + linkError.getMessage() + ')';

			switch (getArch()) {
				case "amd64":
				case "ppc64":
				case "x86_64":
					errorMessage += "\nTry adding '-d32' to your command line arguments";
					break;
	
				default:
					errorMessage += "\nTry adding '-d64' to your command line arguments";
					break;
    		}

            throw new SWTLoadFailed(errorMessage);
          }
          else if ((th.getMessage() != null) &&
                    th.getMessage().toLowerCase().contains("invalid thread access"))
          {
            String errorMessage = "(SWTException: Invalid thread access)";
            errorMessage += "\nTry adding '-XstartOnFirstThread' to your command line arguments";
            throw new SWTLoadFailed(errorMessage);
          }
          else
          {
            throw th;
          }
        }
      }
      catch (ClassNotFoundException ex)
      {
        throw new SWTLoadFailed("Failed to find main class: " + sTargetMainClass);
      }
      catch (NoSuchMethodException ex)
      {
        throw new SWTLoadFailed("Failed to find main method");
      }
    }
    catch (SWTLoadFailed ex)
    {
      String reason = ex.getMessage();
      System.err.println("Launch failed: " + reason);
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JOptionPane.showMessageDialog(null, reason, "Launching UI Failed", JOptionPane.ERROR_MESSAGE);
    }
  }

	private static Manifest getSWTLoaderManifest() throws IOException {
		Class<?> clazz = SWTLoader.class;
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();

		if (classPath.startsWith("jar")) {
			return new Manifest(new URL(classPath.substring(0, classPath.lastIndexOf("!") + 1) +
				"/META-INF/MANIFEST.MF").openStream());
		}

		else
			return null; // Class not from JAR
	}

  private static void loadConfig() throws SWTLoadFailed
  {
    try
    {
      Manifest m = getSWTLoaderManifest();
      if (m == null)
      {
        throw new SWTLoadFailed("Failed to find swtjar manifest");
      }

      Attributes mainAttributes = m.getMainAttributes();
      String mainClass = mainAttributes.getValue(SWTJAR_MAIN_CLASS);
      if (mainClass != null)
      {
        sTargetMainClass = mainClass;
      }

      String swtVer = mainAttributes.getValue(SWTJAR_VERSION);
      if (swtVer != null)
      {
        sSwtVersion = swtVer;
      }

      if ((sTargetMainClass == null) ||
          (sSwtVersion == null))
      {
        throw new SWTLoadFailed("Failed to load swtjar config from manifest");
      }
    }
    catch (IOException ex)
    {
      throw new SWTLoadFailed("Error when loading swtjar config: " + ex.getMessage());
    }
  }

	private static String getArch() {
		String arch = System.getProperty("os.arch");

		switch (arch) {
			case "i386":
			case "x86":
				return "x86";

			case "amd64":
			case "x86-64":
			case "x86_64":
				return "x86_64";

			default:
				return arch;
		}
	}

	private static String getOS() {
		String name = System.getProperty("os.name").toLowerCase();
		Matcher match = Pattern.compile("(win|mac|linux)").matcher(name);

		return (match.find()) ? match.group() : name;
	}

  private static String getSwtJarName() throws SWTLoadFailed
  {
    // If OS is unknown, throw an exception.
    if (getOS().equals("linux") == false && getOS().equals("mac") == false && getOS().equals("win") == false)
    {
      throw new SWTLoadFailed("Unknown OS name: " + getOS());
    }

    // Generate final filename
    String swtFileName = "swt-" +
                         getOS() +
                         getArch() +
                         "-" +
                         sSwtVersion +
                         ".jar";
    return swtFileName;
  }

  private static ClassLoader getSWTClassloader() throws SWTLoadFailed
  {
    String swtFileName = getSwtJarName();
    try
    {
      URLClassLoader cl = (URLClassLoader)SWTLoader.class.getClassLoader();
      URL.setURLStreamHandlerFactory(new RsrcURLStreamHandlerFactory(cl));
      Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
      addUrlMethod.setAccessible(true);

      URL swtFileUrl = new URL("rsrc:" + swtFileName);
      //System.err.println("Using SWT Jar: " + swtFileName);
      addUrlMethod.invoke(cl, swtFileUrl);

      return cl;
    }
    catch (Exception exx)
    {
      throw new SWTLoadFailed(exx.getClass().getSimpleName() + ": " + exx.getMessage());
    }
  }

  private static class SWTLoadFailed extends Exception
  {
    private static final long serialVersionUID = 1L;

    private SWTLoadFailed(String xiMessage)
    {
      super(xiMessage);
    }
  }
}

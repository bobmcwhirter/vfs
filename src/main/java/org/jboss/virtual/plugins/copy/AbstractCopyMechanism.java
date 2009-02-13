/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.virtual.plugins.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.util.file.JarUtils;
import org.jboss.util.id.GUID;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.plugins.context.temp.BasicTempInfo;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Copy mechanism to be used in VFSUtils.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractCopyMechanism implements CopyMechanism
{
   private static Logger log = Logger.getLogger(CopyMechanism.class);

   private static File tempDir;

   private static class GetTempDir implements PrivilegedAction<File>
   {
      public File run()
      {
         String tempDirKey = System.getProperty("vfs.temp.dir", "jboss.server.temp.dir");
         return new File(System.getProperty(tempDirKey, System.getProperty("java.io.tmpdir")));
      }
   }

   /**
    * Get temp directory.
    *
    * @return the temp directory
    */
   public synchronized static File getTempDirectory()
   {
      if (tempDir == null)
      {
         tempDir = AccessController.doPrivileged(new GetTempDir());
         log.info("VFS temp dir: " + tempDir);
      }
      return tempDir;
   }

   /**
    * Get mechanism type.
    *
    * @return the type
    */
   protected abstract String getType();

   /**
    * Is handler already modified.
    *
    * @param handler the handler
    * @return true if already modified
    * @throws IOException for any error
    */
   protected abstract boolean isAlreadyModified(VirtualFileHandler handler) throws IOException;

   /**
    * Should we replace old handler with new.
    *
    * @param parent the parent handler
    * @param oldHandler the old handler
    * @param newHandler the new handler
    * @return true if needs replacement
    * @throws IOException for any error
    */
   protected abstract boolean replaceOldHandler(VirtualFileHandler parent, VirtualFileHandler oldHandler, VirtualFileHandler newHandler) throws IOException;

   /**
    * Unwrap the handler from possible delegate handler.
    *
    * @param handler the handler to unwrap
    * @return unwrapped handler
    */
   protected static VirtualFileHandler unwrap(VirtualFileHandler handler)
   {
      if (handler instanceof DelegatingHandler)
         handler = ((DelegatingHandler)handler).getDelegate();
      return handler;
   }

   public VirtualFile copy(VirtualFile file, VirtualFileHandler handler) throws IOException, URISyntaxException
   {
      VirtualFileHandler unwrapped = unwrap(handler);
      // check modification on unwrapped
      if (isAlreadyModified(unwrapped))
      {
         if (log.isTraceEnabled())
            log.trace("Should already be " + getType() + ": " + unwrapped);
         return file;
      }

      //create guid dir
      File guidDir = createTempDirectory(getTempDirectory(), GUID.asString());
      // unpack handler
      File copy = copy(guidDir, handler);

      String path = handler.getPathName();
      VFSContext oldVFSContext = handler.getVFSContext();
      // create new handler
      FileSystemContext fileSystemContext = new TempContext(copy, oldVFSContext, path);

      // merge old options
      Map<String, String> newOptions = fileSystemContext.getOptions();
      if (newOptions != null) // shouldn't be null, but we check anyway
      {
         Map<String, String> oldOptions = oldVFSContext.getOptions();
         if (oldOptions != null && oldOptions.isEmpty() == false)
            newOptions.putAll(oldOptions);

         newOptions.put(VFSUtils.IS_TEMP_FILE, Boolean.TRUE.toString());
         // save old url
         URL handlerURL = handler.toVfsUrl();
         newOptions.put(VFSUtils.OLD_URL_STRING, handlerURL.toExternalForm());
      }

      // copy exception handler
      ExceptionHandler eh = oldVFSContext.getExceptionHandler();
      if (eh != null)
         fileSystemContext.setExceptionHandler(eh);

      VirtualFileHandler newHandler = fileSystemContext.getRoot();
      oldVFSContext.addTempInfo(new BasicTempInfo(path, copy, newHandler));

      VirtualFileHandler parent = handler.getParent();
      if (parent != null && replaceOldHandler(parent, handler, newHandler))
         parent.replaceChild(handler, newHandler);

      return newHandler.getVirtualFile();
   }

   /**
    * Copy handler.
    *
    * @param guidDir the guid directory
    * @param handler the handler to copy
    * @return handler's copy as file
    * @throws IOException for any error
    */
   protected File copy(File guidDir, VirtualFileHandler handler) throws IOException
   {
      File copy = createCopy(guidDir, handler);
      doCopy(copy, handler);
      return copy;
   }

   /**
    * Create copy destination.
    *
    * @param guidDir the guid dir
    * @param handler the handler to copy
    * @return copy's destination
    * @throws IOException for any error
    */
   protected File createCopy(File guidDir, VirtualFileHandler handler) throws IOException
   {
      return createTempDirectory(guidDir, handler.getName());
   }

   /**
    * Do copy.
    *
    * @param copy the copy destination
    * @param handler the handler
    * @throws IOException for any error
    */
   protected abstract void doCopy(File copy, VirtualFileHandler handler) throws IOException;

   /**
    * Create the temp directory.
    *
    * @param parent the parent
    * @param name the dir name
    * @return new directory
    */
   protected static File createTempDirectory(File parent, String name)
   {
      File file = new File(parent, name);
      if (file.mkdir() == false)
         throw new IllegalArgumentException("Cannot create directory: " + file);
      file.deleteOnExit();
      return file;
   }

   /**
    * Exact copy.
    *
    * @param copy the copy dest
    * @param root the handler to copy
    * @throws IOException for any error
    */
   protected static void exactCopy(File copy, VirtualFileHandler root) throws IOException
   {
      unpack(copy, root, COPY);   
   }

   /**
    * Explode the root into file.
    *
    * @param copy the copy dest
    * @param root the root
    * @throws IOException for any error
    */
   protected static void explode(File copy, VirtualFileHandler root) throws IOException
   {
      unpack(copy, root, EXPLODE);
   }

   /**
    * Unpack the root into file.
    * Repeat this on the root's children.
    *
    * @param copy the copy dest
    * @param root the root
    * @param checker do we write the root checker
    * @throws IOException for any error
    */
   protected static void unpack(File copy, VirtualFileHandler root, WriteRootChecker checker) throws IOException
   {
      // should we write the root
      boolean writeRoot = checker.writeRoot(root);

      if (writeRoot)
         rewrite(root, copy);

      if (writeRoot == false)
      {
         List<VirtualFileHandler> children = root.getChildren(true);
         if (children != null && children.isEmpty() == false)
         {
            for (VirtualFileHandler handler : children)
            {
               File next = new File(copy, handler.getName());
               if (checker.writeRoot(handler) == false && next.mkdir() == false)
                  throw new IllegalArgumentException("Problems creating new directory: " + next);
               next.deleteOnExit();

               unpack(next, handler, checker);
            }
         }
      }
   }

   /**
    * Check if we need to write the root.
    */
   private static interface WriteRootChecker
   {
      /**
       * Do we write the root.
       *
       * @param handler the handler
       * @return true if we write the root
       * @throws IOException for any error
       */
      boolean writeRoot(VirtualFileHandler handler) throws IOException;
   }

   private static WriteRootChecker COPY = new WriteRootChecker()
   {
      public boolean writeRoot(VirtualFileHandler handler) throws IOException
      {
         return handler.isArchive() || handler.isLeaf();
      }
   };

   private static WriteRootChecker EXPLODE = new WriteRootChecker()
   {
      public boolean writeRoot(VirtualFileHandler handler) throws IOException
      {
         return handler.isLeaf();
      }
   };

   /**
    * Unjar to copy parameter.
    *
    * @param copy the dest to unjar
    * @param handler the handler to unjar
    * @throws IOException for any error
    */
   protected static void unjar(File copy, VirtualFileHandler handler) throws IOException
   {
      InputStream in = handler.openStream();
      try
      {
         JarUtils.unjar(in, copy);
      }
      finally
      {
         in.close();
      }
   }

   /**
    * Rewrite contents of handler into file.
    *
    * @param handler the handler
    * @param file the file
    * @throws IOException for any error
    */
   protected static void rewrite(VirtualFileHandler handler, File file) throws IOException
   {
      OutputStream out = new FileOutputStream(file);
      InputStream in = handler.openStream();
      try
      {
         byte[] bytes = new byte[1024];
         while (in.available() > 0)
         {
            int length = in.read(bytes);
            if (length > 0)
               out.write(bytes, 0, length);
         }
      }
      finally
      {
         try
         {
            in.close();
         }
         catch (IOException ignored)
         {
         }
         try
         {
            out.close();
         }
         catch (IOException ignored)
         {
         }
      }
   }
}
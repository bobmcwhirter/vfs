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
package org.jboss.virtual.plugins.context.temp;

import java.io.File;
import java.io.IOException;

import org.jboss.util.file.Files;
import org.jboss.virtual.spi.TempInfo;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * BasicTempInfo
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class BasicTempInfo implements TempInfo
{
   private final String path;
   private volatile File file;
   private volatile VirtualFileHandler handler;

   public BasicTempInfo(String path, File file, VirtualFileHandler handler)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");
      if (file == null && handler == null)
         throw new IllegalArgumentException("Both, file and handler, are null");

      this.path = path;
      // file and handler can even be null
      this.file = file;
      this.handler = handler;
   }

   public String getPath()
   {
      return path;
   }

   public File getTempFile()
   {
      return file;
   }

   public void cleanup()
   {
      if (handler != null)
      {
         handler.cleanup();
      }
      else if (file != null && file.exists())
      {
         Files.delete(file);
      }
      // release
      handler = null;
      file = null;
   }

   public VirtualFileHandler getHandler()
   {
      return handler;
   }

   public boolean isValid()
   {
      try
      {
         return handler != null && handler.exists() && file != null && file.exists();
      }
      catch (IOException e)
      {
         return false;
      }
   }

   @Override
   public String toString()
   {
      return getPath() + " / " + getTempFile().getName();
   }
}
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
package org.jboss.virtual.plugins.context.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

/**
 * ZipEntryWrapper - for abstracted access to in-memory entry
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class ZipEntryWrapper extends ZipBytesWrapper
{
   private static final EmptyEnumeration emptyEnumeration = new EmptyEnumeration();

   /**
    * ZipStreamWrapper is not aware of actual zip source so it can not detect
    * if it's been modified, like ZipFileWrapper does.
    *
    * @param zipStream the current zip input stream
    * @param name the name
    * @param lastModified passed by zip stream provider - constant value
    * @throws java.io.IOException for any error
    */
   ZipEntryWrapper(InputStream zipStream, String name, long lastModified) throws IOException
   {
      super(zipStream, name, lastModified);
   }

   InputStream openStream(ZipEntry ent) throws IOException
   {
      return getRootAsStream();
   }

   Enumeration<? extends ZipEntry> entries() throws IOException
   {
      return emptyEnumeration;
   }

   /**
    * Zip stream enumeration.
    */
   private static class EmptyEnumeration implements Enumeration<ZipEntry>
   {
      public boolean hasMoreElements()
      {
         return false;
      }

      public ZipEntry nextElement()
      {
         return null;
      }
   }
}
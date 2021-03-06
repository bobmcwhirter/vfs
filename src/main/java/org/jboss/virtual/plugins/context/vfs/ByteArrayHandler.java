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
package org.jboss.virtual.plugins.context.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * ByteArrayHandler.
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
@Assembled
public class ByteArrayHandler extends AbstractVirtualFileHandler
{
   private byte[] bytes;
   private final long lastModified;

   public ByteArrayHandler(VFSContext context, VirtualFileHandler parent, String name, byte[] bytes) throws IOException
   {
      super(context, parent, name);
      this.bytes = bytes;
      lastModified = System.currentTimeMillis();
      String path = getPathName();
      if (path.startsWith("/") == false)
         path = "/" + path;
      if (path.endsWith("/") == false)
         path += "/";
      setVfsUrl(new URL("vfs", context.getName(), -1, path, new AssembledUrlStreamHandler(context)));
   }

   @Override
   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return getVfsUrl();
   }

   public URI toURI() throws URISyntaxException
   {
      return getVfsUrl().toURI();
   }

   public long getLastModified() throws IOException
   {
      return lastModified;
   }

   public long getSize() throws IOException
   {
      return bytes.length;
   }

   public boolean exists() throws IOException
   {
      return true;
   }

   public boolean isLeaf() throws IOException
   {
      return true;
   }

   public boolean isHidden() throws IOException
   {
      return false;
   }

   public boolean isNested() throws IOException
   {
      return false;
   }

   public InputStream openStream() throws IOException
   {
      return new ByteArrayInputStream(bytes);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      throw new IOException("File cannot have children");
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      throw new IOException("File cannot have children");
   }

   public boolean removeChild(String name)
   {
      return false;
   }
}

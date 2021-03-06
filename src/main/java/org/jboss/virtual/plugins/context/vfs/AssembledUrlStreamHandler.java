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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Used when creating VFS urls so we don't have to go through the handlers all the time
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class AssembledUrlStreamHandler extends URLStreamHandler
{
   private final VFSContext context;

   public AssembledUrlStreamHandler(VFSContext context)
   {
      this.context = context;
   }

   protected URLConnection openConnection(URL url) throws IOException
   {
      String path = url.getPath();
      VirtualFileHandler vf = context.getRoot().getChild(path);
      if (vf == null)
         throw new IOException(path + " was not found in Assembled VFS context " + context);

      return new VirtualFileURLConnection(url, vf.getVirtualFile());
   }
}

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.virtual.plugins.context.AbstractContextFactory;
import org.jboss.virtual.spi.VFSContext;

/**
 * ContextFactory that keeps track of ZipEntryContexts
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipEntryContextFactory extends AbstractContextFactory
{
   /** singleton */
   private static ZipEntryContextFactory instance = new ZipEntryContextFactory();

   /**
    * ZipEntryContextFactory registers two url protocols: <em>zip</em> and <em>vfszip</em>
    */
   public ZipEntryContextFactory()
   {
      super("zip", "vfszip");  // "jar", "vfsjar",  
   }

   /**
    * Get a <tt>VFSContext</tt> for a given <tt>URL</tt>
    */
   public VFSContext getVFS(URI rootURI) throws IOException
   {
      return getVFS(rootURI.toURL());
   }

   /**
    * Create a new <tt>ZipEntryContext</tt>.
    *
    * @param rootURL the root url
    * @return new zip context
    * @throws IOException for any error
    */
   public VFSContext getVFS(URL rootURL) throws IOException
   {
      try
      {
         return new ZipEntryContext(rootURL);
      }
      catch(URISyntaxException ex)
      {
         MalformedURLException e = new MalformedURLException("Failed to convert URL to URI: " + rootURL);
         e.initCause(ex);
         throw e;
      }
   }

   /**
    * Get a reference to <tt>ZipEntryContextFactory</tt>.
    */
   public static ZipEntryContextFactory getInstance()
   {
      return instance;
   }
}

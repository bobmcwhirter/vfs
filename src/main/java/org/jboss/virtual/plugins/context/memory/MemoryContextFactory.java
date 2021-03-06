/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
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
package org.jboss.virtual.plugins.context.memory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;

/**
 * Singelton implementation of a MemoryContextFactory.
 * The roots are indexed as the 'host' part of the URLs they are stored under 
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MemoryContextFactory implements VFSContextFactory
{
   private static final String[] PROTOCOLS = {"vfsmemory"};
   
   private static MemoryContextFactory instance = new MemoryContextFactory();
   private Map<String, MemoryContext> registry = new ConcurrentHashMap<String, MemoryContext>();
   
   private MemoryContextFactory()
   {
   }
   
   /**
    * Gets the singleton instance
    * @return The singleton instance 
    */
   public static MemoryContextFactory getInstance()
   {
      return instance;
   }
   
   public String[] getProtocols()
   {
      return PROTOCOLS;
   }

   public VFSContext getVFS(URL rootURL) throws IOException
   {
      return createRoot(rootURL);
   }

   public VFSContext getVFS(URI rootURI) throws IOException
   {
      return createRoot(rootURI.toURL());
   }

   /**
    * Gets hold of a root MemoryContext
    *
    * @param host The name of the root
    * @return the found root VFS, or null if none exists for the name 
    */
   public VFS find(String host)
   {
      MemoryContext context = registry.get(host);
      return context != null ? context.getVFS() : null;
   }

   /**
    * Is url valid root url.
    *
    * @param url the url to check
    * @return true if url is root url
    */
   protected static boolean isValidRootURL(URL url)
   {
      String path = url.getPath();
      return path == null || path.length() == 0 || "/".equals(path);
   }

   /**
    * Creates a new root MemoryContext, or returns an already exixting one of one already 
    * exists for the name
    * @param url The url of the root, we use the 'host' part of the name for indexing the context  
    * @return The found or created context
    * @throws IllegalArgumentException If the url parameter contains a path
    */
   public VFSContext createRoot(URL url)
   {
      try
      {
         if (isValidRootURL(url) == false)
            throw new IllegalArgumentException("Root url cannot contain path - " + url);

         String rootName = url.getHost();
         MemoryContext ctx = registry.get(rootName);
         if (ctx == null)
         {
            URL ctxURL = new URL("vfsmemory://" + rootName);
            ctx = new MemoryContext(ctxURL);
            registry.put(rootName, ctx);
         }
         return ctx;
      }
      catch(MalformedURLException e)
      {
         throw new RuntimeException(e);
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Creates a 'directory' within the context determined by the url host part
    * @param url The url of the directory we want to create
    * @return The created directory
    * @throws IllegalArgumentException if there is no root matching the host part of the url 
    */
   public VirtualFile createDirectory(URL url)
   {
      String rootName = url.getHost();
      MemoryContext ctx = registry.get(rootName);
      if (ctx == null)
         throw new IllegalArgumentException("No MemoryContext exists for " + rootName);

      return ctx.createDirectory(url);
   }
   
   /**
    * Creates a 'file' within the context determined by the url host part
    * @param url The url of the directory we want to create
    * @param contents The contents of the file
    * @return The created file
    * @throws IllegalArgumentException if there is no root matching the host part of the url 
    */
   public VirtualFile putFile(URL url, byte[] contents)
   {
      String rootName = url.getHost();
      MemoryContext ctx = registry.get(rootName);
      if (ctx == null)
         throw new IllegalArgumentException("No MemoryContext exists for " + rootName);

      return ctx.putFile(url, contents);
   }
   
   /**
    * Deletes a root MemoryContext 
    * @param url of the root context we want to delete
    * @return true if we deleted a root MemoryContext, false otherwise
    * @throws IllegalArgumentException If the url parameter contains a path
    */
   public boolean deleteRoot(URL url)
   {
      if (isValidRootURL(url) == false)
         throw new IllegalArgumentException("Root cannot contain path - " + url);

      String rootName = url.getHost();
      return (registry.remove(rootName) != null);
   }

   /**
    * Deletes a 'file' or a 'directory' 
    * @param url of the 'file' or 'directory' we want to delete 
    * @return true if we deleted a 'file' or 'directory', false otherwise
    */
   public boolean delete(URL url)
   {
      try
      {
         String path = url.getPath();
         if (path == null || path.length() == 0)
            return deleteRoot(url);

         String rootName = url.getHost();
         MemoryContext ctx = registry.get(rootName);
         if (ctx != null)
         {
            MemoryContextHandler child = (MemoryContextHandler)ctx.getChild(ctx.getRoot(), path);
            MemoryContextHandler parent = (MemoryContextHandler)child.getParent();
            return parent.deleteChild(child);
         }
         return false;
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }
}

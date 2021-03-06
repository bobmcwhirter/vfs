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
package org.jboss.test.virtual.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import junit.framework.Test;
import org.jboss.util.id.GUID;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.MemoryFileFactory;
import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;
import org.jboss.virtual.plugins.context.memory.MemoryContextHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Memory vfs tests.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class MemoryTestCase extends AbstractVFSTest
{
   public MemoryTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(MemoryTestCase.class);
   }

   public void testRootCreationBad() throws Exception
   {
      URL dynamicClassRoot = new URL("vfsmemory", new GUID().toString(), "");
      MemoryFileFactory.createRoot(dynamicClassRoot);
      URL classesURL = new URL(dynamicClassRoot, "classes");
      VirtualFile classes = MemoryFileFactory.createDirectory(classesURL);
      URL url = classes.toURL();
      try
      {
         VFS.getRoot(url);
         fail("Should not be here");
      }
      catch (Exception e)
      {
         assertInstanceOf(e, IllegalArgumentException.class);
      }      
   }

   public void testRootCreationGood() throws Exception
   {
      URL dynamicClassRoot = new URL("vfsmemory", GUID.asString(), "");
      VirtualFile root = MemoryFileFactory.createRoot(dynamicClassRoot).getRoot();
      assertEquals(root, VFS.getRoot(dynamicClassRoot));
      VirtualFile file = MemoryFileFactory.putFile(new URL(dynamicClassRoot + "/classes/somename"), new byte[0]);
      assertNotNull(file);
      System.out.println(file.toURL());
   }

   public void testSerializable() throws Exception
   {
      URI uri = new URI("vfsmemory://aopdomain");
      URL root = new URL("vfsmemory://aopdomain");
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(uri);
      VFSContext ctx = factory.getVFS(uri);
      MemoryContextHandler parent = new MemoryContextHandler(ctx, null, root, "aopdomain");

      URI uri2 = new URI("vfsmemory://aopdomain/child");
      URL root2 = new URL("vfsmemory://aopdomain/child");
      VFSContextFactory factory2 = VFSContextFactoryLocator.getFactory(uri2);
      VFSContext ctx2 = factory2.getVFS(uri);
      MemoryContextHandler child = new MemoryContextHandler(ctx2, parent, root2, "child");

      serializeDeserialize(child, MemoryContextHandler.class);

      byte[] bytes = serialize(parent);
      Object deserializedObject = deserialize(bytes);
      assertInstanceOf(deserializedObject, MemoryContextHandler.class);
      MemoryContextHandler desParent = (MemoryContextHandler)deserializedObject;

      List<VirtualFileHandler> list = desParent.getChildren(true);
      assertNotNull(list);
      assertFalse(list.isEmpty());
      assertNotNull(desParent.getChild("child"));
   }

   public void testContextFactory()throws Exception
   {
      URI uri = new URI("vfsmemory://aopdomain");
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(uri);
      assertNotNull(factory);
   }
   
   public void testContext() throws Exception
   {
      URI uri = new URI("vfsmemory://aopdomain");
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(uri);
      VFSContext ctx = factory.getVFS(uri);
      assertNotNull(ctx);
      
      MemoryContextFactory mfactory = MemoryContextFactory.getInstance();
      assertNotNull(mfactory);
      assertSame(factory, mfactory);
      
      VFSContext mctx = mfactory.createRoot(uri.toURL());
      assertNotNull(mctx);
      assertSame(ctx, mctx);
   }
   
   public void testWriteAndReadData() throws Exception
   {
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         long now = System.currentTimeMillis();
         URL url = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         MemoryFileFactory.putFile(url,  new byte[] {'a', 'b', 'c'});
         
         String read = readURL(url);
         assertEquals("abc", read);

         VirtualFile classFile = VFS.getVirtualFile(new URL("vfsmemory://aopdomain"), "org/acme/test/Test.class");
         InputStream bis = classFile.openStream();
         read = readIS(bis);
         assertEquals("abc", read);
         assertEquals(3, classFile.getSize());
         assertTrue(classFile.exists());
         assertTrue(classFile.isLeaf());
         assertTrue(classFile.getLastModified() >= now);

         assertTrue(MemoryFileFactory.delete(url));
         try
         {
            InputStream is = url.openStream();
            fail("Should not have found file");
         }
         catch(Exception expected)
         {
         }
         
         VFS ctx = MemoryFileFactory.find("aopdomain");
         assertNotNull(ctx);
         
         assertTrue(MemoryFileFactory.deleteRoot(root));
         ctx = MemoryFileFactory.find("aopdomain");
         assertNull(ctx);
      }
      finally
      {
         MemoryFileFactory.deleteRoot(root);
      }
   }
   
   public void testMultipleFiles() throws Exception
   {
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         VFS ctx = MemoryFileFactory.createRoot(root);

         URL urlA = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         MemoryFileFactory.putFile(urlA,  new byte[] {'a', 'b', 'c'});
         
         URL urlB = new URL("vfsmemory://aopdomain/org/foo/test/Test.class");
         MemoryFileFactory.putFile(urlB,  new byte[] {'d', 'e', 'f'});
         
         String readA = readURL(urlA);
         assertEquals("abc", readA);
         
         String readB = readURL(urlB);
         assertEquals("def", readB);
      }
      finally
      {
         MemoryFileFactory.deleteRoot(root);
      }
   }

   public void testNavigate() throws Exception
   {
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         MemoryFileFactory.createRoot(root);

         URL url = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         MemoryFileFactory.putFile(url,  new byte[] {'a', 'b', 'c'});
         URL url2 = new URL("vfsmemory://aopdomain/org/acme/test/Test2.class");
         MemoryFileFactory.putFile(url2,  new byte[] {'a', 'b', 'c'});
         URL url3 = new URL("vfsmemory://aopdomain/org/acme/test/Test3.class");
         MemoryFileFactory.putFile(url3,  new byte[] {'a', 'b', 'c'});
         
         VFS vfs = MemoryFileFactory.createRoot(root);
         VirtualFile file = vfs.getVirtualFile(root, "/org/acme/test/Test.class");
         assertNotNull(file);
         
         VirtualFile file2 = vfs.getVirtualFile(root, "/org");
         assertNotNull(file2);
         VirtualFile test = file2.findChild("/acme/test/Test.class");
         assertNotNull(test);
         assertEquals(file, test);
         
         //acme
         List<VirtualFile> children = file2.getChildren();
         assertEquals(1,children.size());
         VirtualFile child = children.get(0);
         //test
         children = child.getChildren();
         assertEquals(1,children.size());
         child = children.get(0);
         //test/*.class
         children = child.getChildren();
         assertEquals(3,children.size());
         HashMap<String, VirtualFile> childMap = new HashMap<String, VirtualFile>();  
         for (VirtualFile cur : children)
         {
            childMap.put(cur.getName(), cur);
         }
         assertNotNull(childMap.get("Test.class"));
         assertNotNull(childMap.get("Test2.class"));
         assertNotNull(childMap.get("Test3.class"));
      }
      finally
      {
         MemoryFileFactory.deleteRoot(root);
      }
   }

   public void testLeaf() throws Exception
   {
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         VFS ctx = MemoryFileFactory.createRoot(root);
         URL url = new URL("vfsmemory://aopdomain/org/acme/leaf");
         MemoryFileFactory.putFile(url,  new byte[] {'a', 'b', 'c'});

         URL url2 = new URL("vfsmemory://aopdomain/org/acme/leaf/shouldnotwork");
         try
         {
            MemoryFileFactory.putFile(url2,  new byte[] {'d', 'e', 'f'});
            fail("It should not have been possible to add a child to a leaf node");
         }
         catch(Exception e)
         {
         }
         
         VirtualFile classFile = VFS.getVirtualFile(new URL("vfsmemory://aopdomain"), "org/acme/leaf");
         assertNotNull(classFile);
         try
         {
            VirtualFile classFile2 = VFS.getVirtualFile(new URL("vfsmemory://aopdomain"), "org/acme/leaf/shouldnotwork");
            fail("It should not have been possible to find a child of a leaf node");
         }
         catch (Exception expected)
         {
         }
         
         
         try
         {
            URL url3 = new URL("vfsmemory://aopdomain/org/acme");
            MemoryFileFactory.putFile(url3, new byte[] {'1', '2', '3'});
            fail("Should not have been possible to set contents for a non-leaf node");
         }
         catch (Exception expected)
         {
         }
         
         try
         {
            URL url4 = new URL("vfsmemory://aopdomain/org");
            MemoryFileFactory.putFile(url4, new byte[] {'1', '2', '3'});
            fail("Should not have been possible to set contents for a non-leaf node");
         }
         catch (Exception expected)
         {
         }
      }
      finally
      {
         MemoryFileFactory.deleteRoot(root);
      }
   }
   
   public void testUrlHandling()throws Exception
   {
      String guid = GUID.asString();
      URL rootA = new URL("vfsmemory://" + guid);
      URL rootB = new URL("vfsmemory", guid, "");
      assertEquals(rootA, rootB);

      URL rootC = new URL("vfsmemory://" + guid + "/classes");
      URL rootD = new URL("vfsmemory", guid, "/classes");
      assertEquals(rootC, rootD);
   }
   
   protected void setUp() throws Exception
   {
      super.setUp();
      VFS.init();
      getLog().info("java.protocol.handler.pkgs: " + System.getProperty("java.protocol.handler.pkgs"));
   }

   private String readURL(URL url) throws IOException
   {
      InputStream is = url.openStream();
      return readIS(is);
   }

   private String readIS(InputStream is)
      throws IOException
   {
      try
      {
         StringBuffer sb = new StringBuffer();
         while (is.available() != 0)
         {
            sb.append((char)is.read());
         }
         return sb.toString();
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch(Exception ignore)
            {
            }
         }
      }      
   }
}

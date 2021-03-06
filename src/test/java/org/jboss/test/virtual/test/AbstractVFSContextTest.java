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
package org.jboss.test.virtual.test;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.test.virtual.support.MockVirtualFileHandlerVisitor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractVFSContextTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVFSContextTest extends AbstractVFSTest
{
   public AbstractVFSContextTest(String name)
   {
      super(name);
   }
   
   protected abstract VFSContext getVFSContext(String name) throws Exception;

   protected abstract VFSContext getParentVFSContext() throws Exception;

   protected abstract String getSuffix();

   /* TODO URI testing
   public void testRootURI() throws Exception
   {
   }
   */
   
   public void testGetVFS() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      URI rootURI = context.getRootURI();
      VFS vfs = context.getVFS();
      VirtualFile rootFile = vfs.getRoot();

      URI uri = new URI("vfs" + rootURI);
      URI rfUri = rootFile.toURI();
      assertEquals(uri.getPath(), rfUri.getPath());
   }
   
   public void testGetRoot() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      URI rootURI = context.getRootURI();
      VirtualFileHandler rootHandler = context.getRoot(); 
      VFS vfs = context.getVFS();
      VirtualFile rootFile = vfs.getRoot();
      
      assertEquals(rootURI, rootHandler.toURI());
      assertEquals(rootHandler.getVirtualFile(), rootFile);
   }
   
   /* TODO getOptions
   public void testGetOptions() throws Exception
   {
   }
   */
   
   public void testGetChildren() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      List<VirtualFileHandler> children = context.getChildren(root, false);
      
      Set<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");

      Set<String> actual = new HashSet<String>();
      for (VirtualFileHandler child : children)
      {
         if (child.getName().startsWith("META-INF") == false && child.getName().equals(".svn") == false)
            actual.add(child.getName());
      }
      
      assertEquals(expected, actual);
   }

   public void testGetChildrenNullFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      try
      {
         context.getChildren(null, false);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildRoot() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "");
      assertEquals(root, found);
   }

   public void testFindChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "child");
      assertEquals("child", found.getPathName());
   }

   public void testFindChildSubFolder() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "subfolder");
      assertEquals("subfolder", found.getPathName());
   }

   public void testFindChildSubChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = context.getChild(root, "subfolder/subchild");
      assertEquals("subfolder/subchild", found.getPathName());
   }

   public void testFindChildDoesNotExist() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         assertNull(context.getChild(root, "doesnotexist"));
      }
      catch (Throwable t)
      {
         checkThrowableTemp(IOException.class, t);
      }
   }

   public void testFindChildNullFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      try
      {
         context.getChild(null, "");
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildNullPath() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         context.getChild(root, null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testSpecialTokensOnLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler leaf = root.getChild("child");
      assertTrue(leaf.isLeaf());
      assertNotNull(leaf.getChild(".."));
      assertNotNull(leaf.getChild("."));
      leaf = root.getChild("subfolder/subchild");
      assertTrue(leaf.isLeaf());
      assertNotNull(leaf.getChild(".."));
      assertNotNull(leaf.getChild("."));
   }

   public void testSimpleReversePath() throws Exception
   {
      checkSpecialPath("simple" + getSuffix() + "/../complex" + getSuffix() + "/subfolder/subsubfolder/../subchild", "subchild");
   }

   public void testComplexReversePath() throws Exception
   {
      checkSpecialPath("complex" + getSuffix() + "/../simple" + getSuffix() + "/child", "child");
   }

   public void testDirectOverTheTop() throws Exception
   {
      checkOverTheTop("..");
   }

   public void testMiddleOverTheTop() throws Exception
   {
      checkOverTheTop("complex" + getSuffix() + "/subfolder/../../../complex" + getSuffix() + "/subfolder");
   }

   protected void checkOverTheTop(String path) throws Exception
   {
      try
      {
         checkSpecialPath(path, null);
         fail("Should not be here.");
      }
      catch(Exception e)
      {
         checkThrowable(IOException.class, e);
      }
   }

   public void testCurrentAtTheStart() throws Exception
   {
      checkSpecialPath("./simple" + getSuffix() + "/child", "child");
      checkSpecialPath("./complex" + getSuffix() + "/subfolder/subchild", "subchild");
   }

   public void testCurrentInTheMiddle() throws Exception
   {
      checkSpecialPath("simple" + getSuffix() + "/./child", "child");
      checkSpecialPath("complex" + getSuffix() + "/./subfolder/subchild", "subchild");
   }

   public void testConcurrentCurrent() throws Exception
   {
      checkSpecialPath("././simple" + getSuffix() + "/././child", "child");
      checkSpecialPath("././complex" + getSuffix() + "/././subfolder/subchild", "subchild");
   }

   protected void checkSpecialPath(String path, String fileName) throws Exception
   {
      VFSContext context = getParentVFSContext();
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.getChild(root, path);
      assertNotNull(child);
      assertTrue(child.isLeaf());
      assertEquals(fileName, child.getName());
   }

   public void testVisit() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      MockVirtualFileHandlerVisitor visitor = new MockVirtualFileHandlerVisitor();
      context.visit(root, visitor);
      
      Set<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");

      Set<String> actual = new HashSet<String>();
      for (VirtualFileHandler child : visitor.getVisited())
      {
         if (child.getName().startsWith("META-INF") == false && child.getName().equals(".svn") == false)
            actual.add(child.getName());
      }
      
      assertEquals(expected, actual);
   }

   public void testVisitNullHandler() throws Exception
   {
      VFSContext context = getVFSContext("children");
      MockVirtualFileHandlerVisitor visitor = new MockVirtualFileHandlerVisitor();
      try
      {
         context.visit(null, visitor);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testVisitNullVisitor() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      try
      {
         context.visit(root, null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }
}

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

import junit.framework.Test;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VFSUtils;

/**
 * Unpack tests.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class TempTestCase extends DetachedCopyTest
{
   public TempTestCase(String s)
   {
      super(s);
   }

   public static Test suite()
   {
      return suite(TempTestCase.class);
   }

   protected VirtualFile modify(VirtualFile file) throws Exception
   {
      return VFSUtils.temp(file);
   }

   protected boolean isExploded() throws Exception
   {
      return false;
   }

   protected boolean isSame(VirtualFile original) throws Exception
   {
      // should almost never be same
      // except if file was already nested jar copy
      return false;
   }
}
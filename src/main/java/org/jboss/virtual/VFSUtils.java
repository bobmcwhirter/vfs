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
package org.jboss.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.virtual.plugins.copy.CopyMechanism;
import org.jboss.virtual.plugins.copy.ExplodedCopyMechanism;
import org.jboss.virtual.plugins.copy.TempCopyMechanism;
import org.jboss.virtual.plugins.copy.UnpackCopyMechanism;
import org.jboss.virtual.spi.LinkInfo;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * VFS Utilities
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VFSUtils
{
   /** The log */
   private static final Logger log = Logger.getLogger(VFSUtils.class);
   /** The link */
   public static final String VFS_LINK_PREFIX = ".vfslink";
   /** The link name */
   public static final String VFS_LINK_NAME = "vfs.link.name";
   /** The link target */
   public static final String VFS_LINK_TARGET = "vfs.link.target";

   /**
    * The system no force copy key / query
    */
   public static final String FORCE_COPY_KEY = "jboss.vfs.forceCopy";
   public static final String USE_COPY_QUERY = "useCopyJarHandler";

   /**
    * Key used to force fallback from vfszip (default) to vfsjar
    */
   public static final String FORCE_VFS_JAR_KEY = "jboss.vfs.forceVfsJar";

   /**
    * Key used to turn off reaper mode in vfszip - forcing synchronous (slower) handling of files
    */
   public static final String FORCE_NO_REAPER_KEY = "jboss.vfs.forceNoReaper";
   public static final String NO_REAPER_QUERY = "noReaper";

   /**
    * Get the paths string for a collection of virtual files
    * 
    * @param paths the paths
    * @return the string
    * @throws IllegalArgumentException for null paths
    */
   public static String getPathsString(Collection<VirtualFile> paths)
   {
      StringBuilder buffer = new StringBuilder();
      boolean first = true;
      for (VirtualFile path : paths)
      {
         if (path == null)
            throw new IllegalArgumentException("Null path in " + paths);
         if (first == false)
            buffer.append(':');
         else
            first = false;
         buffer.append(path.getPathName());
      }
      
      if (first == true)
         buffer.append("<empty>");
      
      return buffer.toString();
   }
   
   /**
    * Add manifest paths
    * 
    * @param file the file
    * @param paths the paths to add to
    * @throws IOException if there is an error reading the manifest or the
    *         virtual file is closed
    * @throws IllegalStateException if the file has no parent
    * @throws IllegalArgumentException for a null file or paths
    */
   public static void addManifestLocations(VirtualFile file, List<VirtualFile> paths) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (paths == null)
         throw new IllegalArgumentException("Null paths");
      
      Manifest manifest = getManifest(file);
      if (manifest == null)
         return;

      Attributes mainAttributes = manifest.getMainAttributes();
      String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
      
      if (classPath == null)
      {
         if (log.isTraceEnabled())
            log.trace("Manifest has no Class-Path for " + file.getPathName());
         return;
      }
      
      VirtualFile parent = file.getParent();
      if (parent == null)
         throw new IllegalStateException(file + " has no parent.");

      URL parentURL;
      URL vfsRootURL;
      int rootPathLength;
      try
      {
         parentURL = parent.toURL();
         vfsRootURL = file.getVFS().getRoot().toURL();
         rootPathLength = vfsRootURL.getPath().length();
      }
      catch(URISyntaxException e)
      {
         IOException ioe = new IOException("Failed to get parent URL for " + file);
         ioe.initCause(e);
         throw ioe;
      }

      StringTokenizer tokenizer = new StringTokenizer(classPath);
      while (tokenizer.hasMoreTokens())
      {
         String path = tokenizer.nextToken();
         try
         {
            String parentPath = parentURL.toString();
            if(parentPath.endsWith("/") == false)
               parentPath += "/";
            URL libURL = new URL(parentPath + path);
            String libPath = libURL.getPath();
            // TODO, this occurs for inner jars. Doubtful that such a mf cp is valid
            if( rootPathLength > libPath.length() )
               throw new IOException("Invalid rootPath: "+vfsRootURL+", libPath: "+libPath);

            String vfsLibPath = libPath.substring(rootPathLength);
            VirtualFile vf = file.getVFS().getChild(vfsLibPath);
            if(vf != null)
            {
               if(paths.contains(vf) == false)
               {
                  paths.add(vf);
                  // Recursively process the jar
                  addManifestLocations(vf, paths);
               }
            }
         }
         catch (IOException e)
         {
            log.debug("Manifest Class-Path entry " + path + " ignored for " + file.getPathName() + " reason=" + e);
         }
      }
   }

   /**
    * Get a manifest from a virtual file,
    * assuming the virtual file is the root of an archive
    * 
    * @param archive the root the archive
    * @return the manifest or null if not found
    * @throws IOException if there is an error reading the manifest or the
    *         virtual file is closed
    * @throws IllegalArgumentException for a null archive
    */
   public static Manifest getManifest(VirtualFile archive) throws IOException
   {
      if (archive == null)
         throw new IllegalArgumentException("Null archive");
      
      VirtualFile manifest;
      try
      {
         manifest = archive.findChild(JarFile.MANIFEST_NAME);
      }
      catch (IOException ignored)
      {
         if (log.isTraceEnabled())
            log.trace("Can't find manifest for " + archive.getPathName());
         return null;
      }
      return readManifest(manifest);
   }

   /**
    * Read the manifest from given manifest VirtualFile.
    *
    * @param manifest the VF to read from
    * @return JAR's manifest
    * @throws IOException if problems while opening VF stream occur
    */
   public static Manifest readManifest(VirtualFile manifest) throws IOException
   {
      InputStream stream = manifest.openStream();
      try
      {
         return new Manifest(stream);
      }
      finally
      {
         try
         {
            stream.close();
         }
         catch (IOException ignored)
         {
         }
      }
   }

   /**
     * Get a manifest from a virtual file system,
     * assuming the root of the VFS is the root of an archive
     *
     * @param archive the vfs
     * @return the manifest or null if not found
     * @throws IOException if there is an error reading the manifest
     * @throws IllegalArgumentException for a null archive
     */
    public static Manifest getManifest(VFS archive) throws IOException
    {
       VirtualFile root = archive.getRoot();
       return getManifest(root);
    }
   
   /**
    * Fix a name (removes any trailing slash)
    * 
    * @param name the name to fix
    * @return the fixed name
    * @throws IllegalArgumentException for a null name
    */
   public static String fixName(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");
      
      int length = name.length();
      if (length <= 1)
         return name;
      if (name.charAt(length-1) == '/')
         return name.substring(0, length-1);
      return name;
   }

   /**
    * Get the name.
    *
    * @param uri the uri
    * @return name from uri's path
    */
   public static String getName(URI uri)
   {
      String name = uri.getPath();
      if( name != null )
      {
         // TODO: Not correct for certain uris like jar:...!/ 
         int lastSlash = name.lastIndexOf('/');
         if( lastSlash > 0 )
            name = name.substring(lastSlash+1);
      }
      return name;
   }

   /**
    * Take a URL.getQuery string and parse it into name=value pairs
    * 
    * @param query Possibly empty/null url query string
    * @return String[] for the name/value pairs in the query. May be empty but never null.
    */
   public static Map<String, String> parseURLQuery(String query)
   {
	   Map<String, String> pairsMap = new HashMap<String, String>();
      if( query != null )
      {
   	   StringTokenizer tokenizer = new StringTokenizer(query, "=&");
   	   while( tokenizer.hasMoreTokens() )
   	   {
   		   String name = tokenizer.nextToken();
   		   String value = tokenizer.nextToken();
   		   pairsMap.put(name, value);
   	   }
      }
	   return pairsMap;
   }

   /**
    * Does a vf name contain the VFS link prefix
    * @param name - the name portion of a virtual file
    * @return true if the name starts with VFS_LINK_PREFIX, false otherwise
    */
   public static boolean isLink(String name)
   {
      return name.indexOf(VFS_LINK_PREFIX) >= 0;
   }

   /**
    * Read the link information from the stream based on the type as determined
    * from the name suffix.
    * 
    * @param is - input stream to the link file contents
    * @param name - the name of the virtual file representing the link
    * @param props the propertes 
    * @return a list of the links read from the stream
    * @throws IOException on failure to read/parse the stream
    * @throws URISyntaxException for an error parsing a URI
    */
   public static List<LinkInfo> readLinkInfo(InputStream is, String name, Properties props)
      throws IOException, URISyntaxException
   {
      List<LinkInfo> info = new ArrayList<LinkInfo>();
      if( name.endsWith(".properties") )
         parseLinkProperties(is, info, props);
      else
         throw new UnsupportedEncodingException("Unknown link format: "+name);
      return info;
   }

   /**
    * Parse a properties link file
    * 
    * @param is - input stream to the link file contents
    * @param info the link infos
    * @param props the propertes 
    * @throws IOException on failure to read/parse the stream
    * @throws URISyntaxException for an error parsing a URI
    */
   public static void parseLinkProperties(InputStream is, List<LinkInfo> info, Properties props)
      throws IOException, URISyntaxException
   {
      props.load(is);
      // Iterate over the property tuples
      for(int n = 0; ; n ++)
      {
         String nameKey = VFS_LINK_NAME + "." + n;
         String name = props.getProperty(nameKey);
         String uriKey = VFS_LINK_TARGET + "." + n;
         String uri = props.getProperty(uriKey);
         // End when the value is null since a link may not have a name
         if (uri == null)
         {
            break;
         }
         // Replace any system property references
         uri = StringPropertyReplacer.replaceProperties(uri);
         LinkInfo link = new LinkInfo(name, new URI(uri));
         info.add(link);
      }
   }

   /**
    * Deal with urls that may include spaces.
    * 
    * @param url the url
    * @return uri the uri
    * @throws URISyntaxException for any error
    */
   public static URI toURI(URL url) throws URISyntaxException
   {
      String urispec = url.toExternalForm();
      // Escape any spaces
      urispec = urispec.replaceAll(" ", "%20");
      return new URI(urispec);
   }

   /**
    * Get the options for this file.
    *
    * @param file the file
    * @return options map
    */
   private static Map<String, String> getOptions(VirtualFile file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      VirtualFileHandler handler = file.getHandler();
      VFSContext context = handler.getVFSContext();
      return context.getOptions();
   }

   /**
    * Get the option.
    *
    * @param file the file
    * @param key the option key
    * @return key's option
    */
   public static String getOption(VirtualFile file, String key)
   {
      Map<String, String> options = getOptions(file);
      return options != null ? options.get(key) : null;
   }

   /**
    * Enable copy for file param.
    *
    * @param file the file
    */
   public static void enableCopy(VirtualFile file)
   {
      Map<String, String> options = getOptions(file);
      if (options == null)
         throw new IllegalArgumentException("Cannot enable copy on null options: " + file);

      options.put(USE_COPY_QUERY, Boolean.TRUE.toString());
   }

   /**
    * Disable copy for file param.
    *
    * @param file the file
    */
   public static void disableCopy(VirtualFile file)
   {
      Map<String, String> options = getOptions(file);
      if (options == null)
         throw new IllegalArgumentException("Cannot disable copy on null options: " + file);

      options.remove(USE_COPY_QUERY);
   }

   /**
    * Unpack the nested artifact under file param.
    *
    * @param file the file to unpack
    * @return unpacked file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile unpack(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, UnpackCopyMechanism.INSTANCE);
   }

   /**
    * Force explode.
    * Explode archives or nested entries.
    *
    * @param file the file to explode
    * @return exploded file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile explode(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, ExplodedCopyMechanism.INSTANCE);
   }

   /**
    * Create temp.
    *
    * @param file the file to temp
    * @return temp file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   public static VirtualFile temp(VirtualFile file) throws IOException, URISyntaxException
   {
      return copy(file, TempCopyMechanism.INSTANCE);
   }

   /**
    * Create temp.
    *
    * @param file the file to unpack/explode
    * @param mechanism the copy mechanism
    * @return temp file
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   protected static VirtualFile copy(VirtualFile file, CopyMechanism mechanism) throws IOException, URISyntaxException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (mechanism == null)
         throw new IllegalArgumentException("Null copy mechanism");

      return mechanism.copy(file, file.getHandler());
   }

   /**
    * Is file handle nested.
    *
    * @param file the file handle to check
    * @return true if file/dir is nested otherwise false
    * @throws IOException for any error
    */
   public static boolean isNestedFile(VirtualFile file) throws IOException
   {
      VirtualFileHandler handler = file.getHandler();
      return handler.isNested();
   }
}

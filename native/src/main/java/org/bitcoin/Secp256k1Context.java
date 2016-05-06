/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitcoin;

import org.scijava.nativelib.DefaultJniExtractor;
import org.scijava.nativelib.JniExtractor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the context reference used in native methods 
   to handle ECDSA operations.
 */
public class Secp256k1Context {
    private static final Logger log = LoggerFactory.getLogger(Secp256k1Context.class);

    private static final boolean enabled; //true if the library is loaded
    private static final long context; //ref to pointer to context obj

    static { //static initializer
      boolean isEnabled = true;
      long contextRef = -1;
      try {
          loadLibrary("secp256k1");
          contextRef = secp256k1_init_context();
      } catch (UnsatisfiedLinkError e) {
          log.warn("Could not load native crypto library: {}", e);
          isEnabled = false;
      }
      enabled = isEnabled;
      context = contextRef;
  }

  public static boolean isEnabled() {
     return enabled;
  }

  public static long getContext() {
     if (!enabled) return -1; //sanity check
     return context;
  }

  private static native long secp256k1_init_context();


    @SuppressWarnings("unchecked")
    public static void loadLibrary(String libName) {
        try {
            //first try if the library is on the configured library path
            System.loadLibrary(libName);
        } catch (Throwable e) {
            // or get it from the file system or the jar
            try {
                final File libFile = getLibFile(libName);
                System.load(libFile.getAbsolutePath());
            } catch (final Throwable t) {
                throw e instanceof RuntimeException ?
                        (RuntimeException) t : new RuntimeException(t);
            }
        }
    }

    private static File getLibFile(String libName) throws IOException {
        final String mapped = System.mapLibraryName(libName);
        final String[] aols = getAOLs();
        final ClassLoader loader = Secp256k1Context.class.getClassLoader();
        final File unpackedLib = getUnpackedLib(loader, aols, libName, mapped);
        if (unpackedLib != null) {
            // available as plain file
            return unpackedLib;
        } else {
            // otherwise extract from the jar
            return unpackLib(libName, mapped, aols, loader);
        }
    }

    private static File unpackLib(String libName, String mapped, String[] aols, ClassLoader loader) throws IOException {
        final String libPath = getLibPath(loader, aols, mapped);
        final JniExtractor extractor = new DefaultJniExtractor(Secp256k1Context.class, System.getProperty("java.io.tmpdir"));
        return extractor.extractJni(libPath, libName);
    }

    private static String[] getAOLs() {
        final String ao = System.getProperty("os.arch") + "-" + System.getProperty("os.name").replaceAll(" ", "");

        // choose the list of known AOLs for the current platform
        if (ao.startsWith("i386-Linux")) {
            return new String[]{
                    "i386-Linux-ecpc", "i386-Linux-gpp", "i386-Linux-icc", "i386-Linux-ecc", "i386-Linux-icpc", "i386-Linux-linker", "i386-Linux-gcc"
            };
        } else if (ao.startsWith("x86-Windows")) {
            return new String[]{
                    "x86-Windows-linker", "x86-Windows-gpp", "x86-Windows-msvc", "x86-Windows-icl", "x86-Windows-gcc"
            };
        } else if (ao.startsWith("amd64-Linux")) {
            return new String[]{
                    "amd64-Linux-gpp", "amd64-Linux-icpc", "amd64-Linux-gcc", "amd64-Linux-linker"
            };
        } else if (ao.startsWith("amd64-Windows")) {
            return new String[]{
                    "amd64-Windows-gpp", "amd64-Windows-msvc", "amd64-Windows-icl", "amd64-Windows-linker", "amd64-Windows-gcc"
            };
        } else if (ao.startsWith("amd64-FreeBSD")) {
            return new String[]{
                    "amd64-FreeBSD-gpp", "amd64-FreeBSD-gcc", "amd64-FreeBSD-linker"
            };
        } else if (ao.startsWith("ppc-MacOSX")) {
            return new String[]{
                    "ppc-MacOSX-gpp", "ppc-MacOSX-linker", "ppc-MacOSX-gcc"
            };
        } else if (ao.startsWith("x86_64-MacOSX")) {
            return new String[]{
                    "x86_64-MacOSX-icc", "x86_64-MacOSX-icpc", "x86_64-MacOSX-gpp", "x86_64-MacOSX-linker", "x86_64-MacOSX-gcc"
            };
        } else if (ao.startsWith("ppc-AIX")) {
            return new String[]{
                    "ppc-AIX-gpp", "ppc-AIX-xlC", "ppc-AIX-gcc", "ppc-AIX-linker"
            };
        } else if (ao.startsWith("i386-FreeBSD")) {
            return new String[]{
                    "i386-FreeBSD-gpp", "i386-FreeBSD-gcc", "i386-FreeBSD-linker"
            };
        } else if (ao.startsWith("sparc-SunOS")) {
            return new String[]{
                    "sparc-SunOS-cc", "sparc-SunOS-CC", "sparc-SunOS-linker"
            };
        } else if (ao.startsWith("arm-Linux")) {
            return new String[]{
                    "arm-Linux-gpp", "arm-Linux-linker", "arm-Linux-gcc"
            };
        } else if (ao.startsWith("x86-SunOS")) {
            return new String[]{
                    "x86-SunOS-g++", "x86-SunOS-linker"
            };
        } else if (ao.startsWith("i386-MacOSX")) {
            return new String[]{
                    "i386-MacOSX-gpp", "i386-MacOSX-gcc", "i386-MacOSX-linker"
            };
        } else {
            throw new RuntimeException("Unhandled architecture/OS: " + ao);
        }
    }

    private static File getUnpackedLib(final ClassLoader loader, final String[] aols, final String fileName, final String mapped) {
        final String classPath = Secp256k1Context.class.getName().replace('.', '/') + ".class";
        final URL url = loader.getResource(classPath);
        if (url == null || !"file".equals(url.getProtocol())) return null;
        final String path = url.getPath();
        final String prefix = path.substring(0, path.length() - classPath.length());
        for (final String aol : aols) {
            final File file = new File(prefix + aol + "/lib/" + mapped);
            if (file.isFile()) return file;
        }
        return null;
    }

    private static String getLibPath(final ClassLoader loader, final String[] aols, final String mapped) {
        for (final String aol : aols) {
            final String libPath = aol + "/lib/";
            if (loader.getResource(libPath + mapped) != null) return libPath;
        }
        throw new RuntimeException("Library '" + mapped + "' not found!");
    }


}

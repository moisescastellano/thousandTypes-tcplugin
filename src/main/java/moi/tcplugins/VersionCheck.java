package moi.tcplugins;

import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcclassloader.PluginClassLoader;

public class VersionCheck {

	final static Logger log = LoggerFactory.getLogger(VersionCheck.class);

	private static void throwVersionException (int[] minVersion) throws Exception {
		String msg = 
				"PluginClassLoader must be at least version " + minVersion[0] + "." + minVersion[1] + "." + minVersion[2] 
				+ ". If you have other java plugins with older versions,"
				+ " replace tc-classloader-x.x.jar in every [plugin]/javalib directory with the latest version.";
		throw new Exception(msg);		
	}
	
	private final static int[] VERSION_1_7_0 = {1,7,0}; // last 1.x version 
	private final static int[] VERSION_2_2_0 = {2,2,0}; // unique 2.x version without getVersionNumber

	public static String getPluginClassLoaderVersion() {
		return getPluginClassLoaderVersion(getPluginClassLoaderVersionNumber());
	}
	
	public static String getPluginClassLoaderVersion(int[] version) {
		return version[0] + "." + version[1] + "." + version[2] + ".";
	}

	public static int[] getPluginClassLoaderVersionNumber() {
		int[] version = {0,0,0};
		try {
			version = PluginClassLoader.getVersionNumber(); //	getVersionNumber was added in version 2.3		
		} catch (java.lang.NoSuchMethodError nsme) {
			try {
				PluginClassLoader.class.getMethod("addJar",JarInputStream.class); // method addJar was deleted in version 2.2
				version = VERSION_1_7_0;
			} catch (NoSuchMethodException e) {
				version = VERSION_2_2_0;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(VersionCheck.class + ".getPluginClassLoaderVersion(archiveData)" + getPluginClassLoaderVersion(version));
		}
		return version;		
	}

	public static void checkPluginClassLoaderVersion(int[] version) throws Exception {
		int[] pclVersion = getPluginClassLoaderVersionNumber();
		if (version[0] > pclVersion[0]) {
			throwVersionException(version);
		} else if (version[0] == pclVersion[0]) {
			if (version[1] > pclVersion[1]) {
				throwVersionException(version);
			} else if (version[1] == pclVersion[1]) {
				if (version[2] > pclVersion[2]) {
					throwVersionException(version);
				}
			} 
		}
	}

	
}

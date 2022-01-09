package moi.tcplugins.preview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import moi.tcplugins.VersionCheck;
import moi.tcplugins.preview.Config.Format;
import moi.tcplugins.preview.Config.LinesAsFiles;
import moi.tcplugins.preview.Config.ShowableHelpFile;
import moi.tcplugins.preview.Config.ShowableItem;
import moi.tcplugins.preview.Config.ShowableMaxSizeFile;
import moi.tcplugins.preview.Config.SpecificFormat;
import moi.tcplugins.preview.Config.SpecificMetadata;
import plugins.wcx.HeaderData;
import plugins.wcx.OpenArchiveData;
import plugins.wcx.WCXPluginAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Moises Castellano 2021-2022
 * https://github.com/moisescastellano/preview-plugin
 */


public class PreviewPlugin extends WCXPluginAdapter {
	
	final static Logger log = LoggerFactory.getLogger(PreviewPlugin.class);
	final static int[] pluginClassLoaderVersion = {2,3,1}; // min pluginClassLoader version 2.3.1
	private final static String CONFIG_FILE = "config.yaml";
	
	Config config;
	Format format;
	
	private enum ItemType {
		LINE, METADATA, PREVIEW, CONTENTS, HELPFILE, EXCEPTION
	}

	private class CatalogInfo {
		/**
		 * The name of the archive.
		 */
		private String arcName;
		
		public Throwable throwable;
		public Throwable nonBlockingthrowable;
		public int msgCount; 
		
		public int linesAsFilesCounter;
		public List<String> linesAsFiles;

		public int metadataCounter;
		public String[] metadataNames;
		public String[] metadataValues;
		public StringBuffer sbMetadata;
		
		public int specificMetadataCounter;
		
		public int helpFilesCounter;

		public boolean everythingRead;
		public String contents;
		
		public ItemType itemType;
		public ShowableItem showableItem;

	}
	
	@Override
	public Object openArchive(OpenArchiveData archiveData) {
		if (log.isDebugEnabled()) {
			log.debug(PreviewPlugin.class + ".openArchive(archiveData)");
		}
		File path = new File(archiveData.getArcName());
		CatalogInfo catalogInfo = new CatalogInfo();
		catalogInfo.arcName = archiveData.getArcName();			
		try {
			VersionCheck.checkPluginClassLoaderVersion(pluginClassLoaderVersion);
			readConfig(archiveData.getArcName());
			parse(catalogInfo, path, format.preview.maxSize);
		} catch (Throwable e) {
			JFrame frame = new JFrame();
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Error on openArchive", JOptionPane.ERROR_MESSAGE);
			catalogInfo.throwable = e;
		}
		
		return catalogInfo;
	}
	

	private void readConfig(String arcName) throws IOException, StreamReadException, DatabindException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		InputStream is = PreviewPlugin.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
		this.config = mapper.readValue(is, Config.class);
		this.format = config.defaultFormat;
		String extension = extension(arcName);
		if (extension.length() > 0) {
			List<SpecificFormat> formats = this.config.specificFormats;
			for (SpecificFormat format: formats) {
				if (extension.equals(format.extension)) {
					this.format = format; 
				}
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(PreviewPlugin.class.getName() + ": [" + config.defaultFormat.allMetadataDir.itemName + "]");
		}
	}
	
	private void parse(CatalogInfo catalogInfo, File path, int writeLimit) throws IOException, TikaException, SAXException {

	   	if (log.isDebugEnabled()) {
	   		log.debug("in parse: path=[" + path + "]");
	    }

		BodyContentHandler handler = new BodyContentHandler(writeLimit); // on opening read only up to 10000 bytes
		Metadata metadata = new Metadata();
		FileInputStream inputStream = null;
              
	    ParseContext pcontext = new ParseContext();
	    AbstractParser parser;
	    if ("pdf".equals(extension(path))) {
	    	parser = new PDFParser();
	    } else {
	    	parser = new AutoDetectParser();	    	
	    }
	    long before = System.currentTimeMillis();
	    try {
			inputStream = new FileInputStream(path);
			if (log.isDebugEnabled()) {
				log.debug("in parse: pcontext=[" + pcontext + "]; parser=[" + parser + "]; inputStream=[" + inputStream + "]; metadata=[" + metadata + "]");
			}
			parser.parse(inputStream, handler, metadata, pcontext);
			catalogInfo.everythingRead = true;
	    } catch (org.apache.tika.exception.WriteLimitReachedException e) {
	    	catalogInfo.everythingRead = false;
	    } finally {
	    	if (inputStream != null) {
		    	  inputStream.close();
	    	}
	    }
	    long after = System.currentTimeMillis();
	     
	    catalogInfo.contents = handler.toString();

		extractMetada(catalogInfo, metadata);

	    if (log.isDebugEnabled()) {
		      log.debug("time:" + (after - before) + " millisecs");
		      log.debug("Contents of the PDF :" + handler.toString().length() + " bytes length" );
	    }
	    		  
	}
   
   	private List<String> linesAsFiles(String contents, int minLineLength, int maxLineLength, int maxNumLines) {
   		List<String> linesAsFiles = new ArrayList<>();
   		String s = contents.replaceAll("[^a-zA-Z0-9 ]","-");
   	    StringTokenizer tokenizer = new StringTokenizer(s, " ,;\n");
   	    StringBuffer sb = new StringBuffer();
   	    while (tokenizer.hasMoreElements() && linesAsFiles.size() < maxNumLines) {
   	    	String next = tokenizer.nextToken();
   	    	insert(linesAsFiles, sb, next, minLineLength, maxLineLength, maxNumLines);
   	    }
   	    if (linesAsFiles.size() < maxNumLines) {
   	    	linesAsFiles.add(sb.toString());
   	    }
   	    return linesAsFiles;
   	}

   	private void insert (List<String> linesAsFiles, StringBuffer sb, String next, int minLineLength, int maxLineLength, int maxNumLines) {
	    if (log.isDebugEnabled()) {
		      log.debug("insert: linesAsFiles.size=" + linesAsFiles.size() + ";sb=[" + sb + "]; next=[" + next + "].");
	    }
	   	if (sb.length() + " ".length() + next.length() > maxLineLength) {
	   		if (sb.length() >= minLineLength) {
	   			linesAsFiles.add(sb.toString());
	   			if (linesAsFiles.size() < maxNumLines) {
		   			sb.delete(0, sb.length());
		   			// sb.append(twoDigits(linesAsFiles.size())+"-");
		   			insert(linesAsFiles, sb, next, minLineLength, maxLineLength, maxNumLines);
	   			}
	   		} else {
	   			int addNumChars = maxLineLength-sb.length()-" ".length();
	   			sb.append(" " + next.substring(0,addNumChars));
	   			linesAsFiles.add(sb.toString());
	   			if (linesAsFiles.size() < maxNumLines) {
		   			sb.delete(0, sb.length());
		   			// sb.append(twoDigits(linesAsFiles.size())+"-");
		   			insert(linesAsFiles, sb, next.substring(addNumChars,next.length()), minLineLength, maxLineLength, maxNumLines);
	   			}
	   		}
	   	} else {
	   		sb.append(" ").append(next);
	   	}
   	}
   	
   	private String nDigits(int number, int maxNumber) {
   		if (maxNumber < 10) {
   			return ""+number;
   		} else if (maxNumber < 100) {
   	   		return (number < 10) ? "0"+number : ""+number;
   		} else {
   	   		return (number < 10) ? "00"+number : (number < 100) ? "0"+number : ""+number;
   		}
   	}

	private void extractMetada(CatalogInfo catalogInfo, Metadata metadata) {
		String[] metadataNames = metadata.names();
		String[] values = new String[metadataNames.length];
		catalogInfo.metadataNames = metadataNames;
		catalogInfo.metadataValues = values;
		catalogInfo.sbMetadata = new StringBuffer();
		
		int i=0;
		for (String name: metadataNames) {
			String value = metadata.get(name);
			values[i++] = value;
			String ne = name + "=" + value;
			catalogInfo.sbMetadata.append(ne).append("\n");
		}
		
		if (log.isDebugEnabled()) {
		      log.debug("Metadata of the PDF:");
		      for(String name : metadataNames) {
		         log.debug(name+ " : " + metadata.get(name));
		      }
		}
	}
	
	@Override
	public int closeArchive(Object archiveData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".closeArchive(archiveData)");
		}
		return SUCCESS;
	}

	@Override
	public int processFile(Object archiveData, int operation, String destPath, String destName) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".processFile(archiveData, operation=["+operation+"], destPath=["+destPath+"];destName=["+destName+"]");
		}
		CatalogInfo catalogInfo = (CatalogInfo) archiveData;
		String fullDestName = (destPath==null?"":destPath) + destName;
		try {
			if (operation == PK_EXTRACT) {
				if (log.isDebugEnabled()) log.debug(this.getClass().getName() + ".processFile() EXTRACT from:[" + catalogInfo.arcName + "] to: [" + fullDestName + "]");
				if (catalogInfo.throwable != null) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.throwable, false);
				}
				if (catalogInfo.itemType == ItemType.EXCEPTION) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.nonBlockingthrowable, false);
				}
				if (catalogInfo.itemType == ItemType.METADATA) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.sbMetadata.toString(), false);
				}
				if (catalogInfo.itemType == ItemType.PREVIEW || catalogInfo.itemType == ItemType.LINE){
					return save(catalogInfo, new File(fullDestName), catalogInfo.contents, false);
				}
				if (catalogInfo.itemType == ItemType.HELPFILE) {
					Config.ShowableHelpFile shf = (Config.ShowableHelpFile) catalogInfo.showableItem;
					InputStream is = PreviewPlugin.class.getResourceAsStream("/" + shf.originFileName);
					return copyStream(is, new File(fullDestName), false);
				}
				if (catalogInfo.itemType == ItemType.CONTENTS){
					if  (catalogInfo.everythingRead) {
						return save(catalogInfo, new File(fullDestName), catalogInfo.contents, false);
					}
					File path = new File(catalogInfo.arcName);
					parse(catalogInfo, path, format.contents.maxSize);
					return save(catalogInfo, new File(fullDestName), catalogInfo.contents, false);
				}
				throw new Exception("Not expected case");
			} else if (operation == PK_TEST) {
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() TEST " + (destPath==null?"":destPath) + destName);
				}
				// return checkFile(new File(fullOriginName), headerData.getFileCRC());
			} else if (operation == PK_SKIP) {
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() SKIP " + (destPath==null?"":destPath) + destName);
				}
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			try {
				JFrame frame = new JFrame();
				JOptionPane.showMessageDialog(frame, t.getMessage(), "Error on processFile", JOptionPane.ERROR_MESSAGE);
				return save(catalogInfo, new File(fullDestName), t, false);
			} catch (Throwable t2) {
				log.error(t.getMessage(), t2);
				return E_EWRITE;
			}
		}
		return SUCCESS;
	}
	
	private String extension(File file) {
		return extension(file.getName());
	}
	
	private String extension(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}
		return extension;
	}

	@Override
	public int readHeader(Object archiveData, HeaderData headerData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".readHeader(archiveData, headerData)");
		}
		CatalogInfo catalogInfo = null;
		int previousMsgCount = 0;
		try {
			catalogInfo = (CatalogInfo) archiveData;
			previousMsgCount = catalogInfo.msgCount;
			headerData.setArcName(catalogInfo.arcName);
			if (catalogInfo.throwable != null) {
				Throwable t = catalogInfo.throwable;
				log.debug("Error parsing file", t);
				switch (catalogInfo.msgCount++) {
				case 0:
					headerData.setFileName(t.getMessage() + ".exception");
					log.debug("SUCCESS after throwable");
					return SUCCESS;
				default:
					return E_END_ARCHIVE;
				}
			}
			switch (catalogInfo.msgCount) {
			case 0:
				catalogInfo.msgCount++;
				if (getMainFile(catalogInfo, headerData)) {
					log.debug("SUCCESS on main file" + catalogInfo.msgCount);
					if (catalogInfo.everythingRead) {
						catalogInfo.msgCount++; // skip preview as everything has already read and shown
					}
					return SUCCESS;
				}
				// do not break, let it continue to next case
			case 1:
				catalogInfo.msgCount++;
				if (getPreviewFile(catalogInfo, headerData)) {
					return SUCCESS;
				}
				// do not break, let it continue to next case
			case 2:
				catalogInfo.msgCount++;
				if (getMetadataFile(catalogInfo, headerData)) {
					return SUCCESS;
				}
				// do not break, let it continue to next case
			case 3:
				if (getHelpFile(catalogInfo, headerData)) {
					if (log.isDebugEnabled()) log.debug("readHeader: SUCCESS getHelpFile");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			case 4:
				if (getSpecificMetadata(catalogInfo, headerData)) {
					if (log.isDebugEnabled()) log.debug("readHeader: SUCCESS getSpecificMetadata");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			case 5:
				if (getMetadataDir(catalogInfo, headerData)) {
					if (log.isDebugEnabled()) log.debug("readHeader: SUCCESS getMetadataDir");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			case 6:
				if (getLineAsFile(catalogInfo, headerData)) {
					if (log.isDebugEnabled()) log.debug("readHeader: SUCCESS getLineAsFile");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			default:
				return E_END_ARCHIVE;
			}

		} catch (Throwable e) {
			if (log.isErrorEnabled()) log.error(e.getMessage(), e);
			headerData.setFileName(e.getMessage() + ".exception");
			headerData.setUnpSize(e.getMessage().length());
			catalogInfo.nonBlockingthrowable = e;
			catalogInfo.msgCount = previousMsgCount + 1;
			catalogInfo.itemType = ItemType.EXCEPTION;
			if (log.isDebugEnabled()) log.debug("SUCCESS after throwable",e);
			return SUCCESS;
		}
	}

	private boolean getMainFile(CatalogInfo catalogInfo, HeaderData headerData) {
		if (log.isDebugEnabled()) {
			log.debug("getMainFile: arcName=" + catalogInfo.arcName);
		}
		ShowableItem item = this.format.contents;
		if (catalogInfo.everythingRead) {
			headerData.setUnpSize(catalogInfo.contents.length());
		} else {
			headerData.setUnpSize(new File(catalogInfo.arcName).length());
		}
		catalogInfo.itemType = ItemType.CONTENTS;
		return getFile(item, catalogInfo, headerData);
	}

	private boolean getPreviewFile(CatalogInfo catalogInfo, HeaderData headerData) {
		if (log.isDebugEnabled()) {
			log.debug("getPreviewFile: arcName=" + catalogInfo.arcName);
		}
		ShowableMaxSizeFile item = this.format.preview;
		headerData.setUnpSize(catalogInfo.contents.length());
		catalogInfo.itemType = ItemType.PREVIEW;
		return getFile(item, catalogInfo, headerData);
	}

	private boolean getMetadataFile(CatalogInfo catalogInfo, HeaderData headerData) {
		if (log.isDebugEnabled()) {
			log.debug("getMetadataFile: arcName=" + catalogInfo.arcName);
		}
		ShowableItem item = this.format.allMetadataFile;
		headerData.setUnpSize(catalogInfo.sbMetadata.length());
		catalogInfo.itemType = ItemType.METADATA;
		return getFile(item, catalogInfo, headerData);
	}
	
	private boolean getFile(ShowableItem item, CatalogInfo catalogInfo, HeaderData headerData) {
		if (!item.show) {
			return false;
		}
		File f = new File(catalogInfo.arcName);
		String itemName = replaceVariables(item, 1, 1, f.getName(), f.getName(), "");
		headerData.setFileName(itemName);
		return true;
	}

	private boolean getHelpFile(CatalogInfo catalogInfo, HeaderData headerData) {
		int contador = catalogInfo.helpFilesCounter++;
		if (contador < config.helpFiles.size()) {
			ShowableHelpFile shf = config.helpFiles.get(contador); 
			if (shf.show) {
				headerData.setFileName(shf.itemName);
				if (log.isDebugEnabled()) {
					log.debug("createHelpFile: name=" + shf.itemName);
				}
				catalogInfo.showableItem = shf;
				catalogInfo.itemType = ItemType.HELPFILE;
				return true;
			} else {
				return getHelpFile(catalogInfo, headerData);
			}
		} else {
			return false;
		}
	}

	private boolean getMetadataDir(CatalogInfo catalogInfo, HeaderData headerData) {
		ShowableItem item = this.format.allMetadataDir;
		if (!item.show) {
			return false;
		}
		String[] metadataNames = catalogInfo.metadataNames;
		int contador = catalogInfo.metadataCounter++;
		File f = new File(catalogInfo.arcName);
		String docName = f.getName();
		if (contador < metadataNames.length) {			
			String itemName = replaceVariables(item, contador, metadataNames.length, docName, metadataNames[contador], catalogInfo.metadataValues[contador]);
			headerData.setFileName(itemName);
			headerData.setUnpSize(catalogInfo.sbMetadata.length());
			catalogInfo.itemType = ItemType.METADATA;
			catalogInfo.showableItem = item;
			return true;
		} else {
			return false;
		}
	}
	
	private boolean getSpecificMetadata(CatalogInfo catalogInfo, HeaderData headerData) {
		if (!(this.format instanceof SpecificFormat)) {
			return false;
		}
		SpecificFormat format = (SpecificFormat) this.format;
		List<SpecificMetadata> specificMetadata = format.specificMetadata;
		int contador = catalogInfo.specificMetadataCounter++;
		File f = new File(catalogInfo.arcName);
		String docName = f.getName();
		if (contador < specificMetadata.size()) {
			SpecificMetadata metadata = specificMetadata.get(contador);
			if (metadata.show) {
				String name = metadata.metadataName;
				String value = getMetadataValue(catalogInfo, name);
				if (value == null) {
					value = "not found";
				}
				String itemName = replaceVariables(metadata, contador, specificMetadata.size(), docName, name, value);
				headerData.setFileName(itemName);
				if (log.isDebugEnabled()) {
					log.debug("getSpecificMetadata: itemName=" + itemName + ";");
				}
				headerData.setUnpSize(catalogInfo.sbMetadata.length());
				catalogInfo.itemType = ItemType.METADATA;
				catalogInfo.showableItem = metadata;
				return true;
			} else {
				return getSpecificMetadata(catalogInfo, headerData);
			}
		} else {
			return false;
		}
	}

	private boolean getLineAsFile(CatalogInfo catalogInfo, HeaderData headerData) {
		LinesAsFiles item = this.format.linesAsfiles;
		if (!item.show) {
			return false;
		}
		int contador = catalogInfo.linesAsFilesCounter++;
		if (contador == 0) {
			catalogInfo.linesAsFiles = linesAsFiles(catalogInfo.contents, item.minLength, item.maxLength, item.numberOfLines);
		}
		if (contador < catalogInfo.linesAsFiles.size()) {
			File f = new File(catalogInfo.arcName);
			String line = catalogInfo.linesAsFiles.get(contador);
			String itemName = replaceVariables(item, contador, catalogInfo.linesAsFiles.size(), f.getName(), line, "");
			headerData.setFileName(itemName);
			headerData.setUnpSize(line.length());
			catalogInfo.itemType = ItemType.LINE;
			return true;
		} else {
			return false;
		}
	}

	private String replaceVariables(ShowableItem item, int contador, int maxNumber, String docName, String varName, String value) {
		String itemName = item.itemName;
		int index = docName.lastIndexOf('.');
		String document;
		String extension;
		if (index > 0) {
			document = docName.substring(0,index);
		    extension = docName.substring(index+1);
		} else {
			document = docName;
			extension = "";
		}
		itemName = itemName.replace("%DOCUMENT%", document);
		itemName = itemName.replace("%EXTENSION%", extension);
		itemName = itemName.replace("%LINE%", varName.replaceAll("[^a-zA-Z0-9.]","-"));
		itemName = itemName.replace("%NAME%", varName.replaceAll("[^a-zA-Z0-9.]","-"));
		itemName = itemName.replace("%VALUE%", value.replaceAll("[^a-zA-Z0-9.]","-"));
		itemName = itemName.replace("%NUMBER%", nDigits(contador+1, maxNumber));
		if (itemName.length() > item.maxLength) {
			String itemExtension = extension(itemName);
			if (itemExtension.length() > 0) {
				itemName = itemName.substring(0, item.maxLength - ".".length() - extension.length());
				itemName = itemName + "." + itemExtension;
			} else {
				itemName = itemName.substring(0, item.maxLength);
			}
		}
		return itemName;
	}
	
	private String getMetadataValue(CatalogInfo catalogInfo, String name) {
		int i = 0;
		for (String metadataName: catalogInfo.metadataNames) {
			if (name.equals(metadataName)) {
				if (log.isDebugEnabled()) {
					log.debug("getMetadataValue: name=" + name + ";value=" + catalogInfo.metadataValues[i]);
				}
				return catalogInfo.metadataValues[i];
			}
			i++;
		}
		if (log.isDebugEnabled()) {
			log.debug("getMetadataValue: name=" + name + ";value=null");
		}
		return null;
	}

	@Override
	public int getPackerCaps() {
		return PK_CAPS_HIDE | /* PK_CAPS_NEW | */ PK_CAPS_MULTIPLE | PK_CAPS_MEMPACK;
	}

	private int save (CatalogInfo cinfo, final File dest, Throwable t, final boolean overwrite) {
		if (log.isWarnEnabled()) {
			log.warn("saving throwable",t);
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		return save(cinfo, dest, sw.toString(), overwrite);
	}
	
	private int save (CatalogInfo cinfo, final File dest, String contents, final boolean overwrite) {
		if (overwrite) {
			dest.delete();
		}
		if (dest.exists()) {
			return E_ECREATE;
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(dest);
			out.print(contents);
		} catch (FileNotFoundException fnfe) {
			return E_EOPEN;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return SUCCESS;
	}
	
	private int copyStream (final InputStream is, final File dest, final boolean overwrite) {
		if (log.isDebugEnabled()) {
			log.debug("copyStream: inputStream=[" + is+ ";dest=[" + dest + "]");
		}
		if (overwrite) {
			dest.delete();
		}
		if (dest.exists()) {
			return E_ECREATE;
		}
		OutputStream outStream = null;
		try {
			outStream = new FileOutputStream(dest);
			int bytesRead;
			byte[] buffer = new byte[8 * 1024];
		    while ((bytesRead = is.read(buffer)) != -1) {
		    	outStream.write(buffer, 0, bytesRead);
			}
			
		} catch (FileNotFoundException fnfe) {
			return E_EOPEN;
		} catch (IOException ioe) {
			return E_EWRITE;
		} finally {
			try {
				if (outStream != null) {
					outStream.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException ioe) {
				return E_EWRITE;
			}
		}
		return SUCCESS;
	}
}

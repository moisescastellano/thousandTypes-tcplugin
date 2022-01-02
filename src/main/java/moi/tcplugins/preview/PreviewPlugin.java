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

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import plugins.wcx.HeaderData;
import plugins.wcx.OpenArchiveData;
import plugins.wcx.WCXPluginAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Moises Castellano 2021
 * https://github.com/moisescastellano/preview-plugin
 */


public class PreviewPlugin extends WCXPluginAdapter {
	
	final Logger log = LoggerFactory.getLogger(PreviewPlugin.class); 

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
		
		public int importantMetadataCounter;
		public String[] importantMetadataNames;
		public String[] importantMetadataValues;

		public boolean everythingRead;
		public String contents;

	}

	@Override
	public Object openArchive(OpenArchiveData archiveData) {
		if (log.isDebugEnabled()) {
			log.debug(this.getClass().getName() + ".openArchive(archiveData)");
		}
		File path = new File(archiveData.getArcName());
		CatalogInfo catalogInfo = new CatalogInfo();
		catalogInfo.arcName = archiveData.getArcName();			
		try {
			parse(catalogInfo, path, 10000);
		} catch (Throwable e) {
			catalogInfo.throwable = e;
		}
		try {
			catalogInfo.linesAsFiles = linesAsFiles(catalogInfo.contents, 60, 75, 15);
		} catch (Throwable e) {
			catalogInfo.nonBlockingthrowable = e;
		}
		return catalogInfo;
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
   	
   	private String twoDigits(int number) {
   		return (number < 10) ? "0"+number : ""+number;
   	}

	private void extractMetada(CatalogInfo catalogInfo, Metadata metadata) {
		String[] metadataNames = metadata.names();
		String[] values = new String[metadataNames.length];
		catalogInfo.metadataNames = metadataNames;
		catalogInfo.metadataValues = values;
		catalogInfo.sbMetadata = new StringBuffer();
		
		List<String> importantMetadataNames = new ArrayList<>();
		List<String> importantMetadataValues = new ArrayList<>();
		
		int i=0;
		for (String name: metadataNames) {
			String value = metadata.get(name);
			values[i++] = value;
			String ne = name + "=" + value;
			catalogInfo.sbMetadata.append(ne).append("\n");
			String importantName = isImportantMetadata(name);
			if (importantName != null) {
				importantMetadataNames.add(importantName);
				importantMetadataValues.add(value);
			}
		}
		catalogInfo.importantMetadataNames =  importantMetadataNames.toArray(new String[0]);
		catalogInfo.importantMetadataValues =  importantMetadataValues.toArray(new String[0]);
		
		if (log.isDebugEnabled()) {
		      log.debug("Metadata of the PDF:");
		      for(String name : metadataNames) {
		         log.debug(name+ " : " + metadata.get(name));
		      }
		}
	}
	
	private final String IMPORTANT_PREFIX = "pdf:docinfo:";
	
	private String isImportantMetadata(String name) {
		if (name.startsWith(IMPORTANT_PREFIX)) {
			return name.substring(IMPORTANT_PREFIX.length());
		}
		return null;
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
		try {
			if (operation == PK_EXTRACT) {
				String fullDestName = (destPath==null?"":destPath) + destName;
				if (log.isDebugEnabled()) {
					log.debug(this.getClass().getName() + ".processFile() EXTRACT from:[" + catalogInfo.arcName + "] to: [" + fullDestName + "]");
				}
				String ext = extension(fullDestName);
				if (ext.equals("exception")) {
					return save(catalogInfo, new File(fullDestName), 
							catalogInfo.throwable == null ? catalogInfo.nonBlockingthrowable : catalogInfo.throwable, false);
				} else if (ext.equals("md")) {
					InputStream is = PreviewPlugin.class.getResourceAsStream("/README.md");
					return copyStream(is, new File(fullDestName), false);
				} else if (ext.equals("metadata")) {
					return save(catalogInfo, new File(fullDestName), catalogInfo.sbMetadata.toString(), false);
				} else if (ext.equals("preview") || catalogInfo.everythingRead){
					return save(catalogInfo, new File(fullDestName), catalogInfo.contents, false);
				} else {
					try {
						File path = new File(catalogInfo.arcName);
						parse(catalogInfo, path, -1); // no writer limit
						return save(catalogInfo, new File(fullDestName), catalogInfo.contents, false);
					} catch (Throwable e) {
						if (log.isErrorEnabled()) {
							log.error(this.getClass().getName() + ".processFile() TEST " + (destPath==null?"":destPath) + destName);
						}
						String s = "Error parsing file: " + e.getMessage();
						return save(catalogInfo, new File(fullDestName), s, false);
					}
				}
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
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
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
		try {
			catalogInfo = (CatalogInfo) archiveData;
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
				if (catalogInfo.everythingRead) {
					createMainFile(headerData, catalogInfo.arcName, "txt", catalogInfo.contents.length());
					catalogInfo.msgCount+=2;
				} else {
					createMainFile(headerData, catalogInfo.arcName, "txt", 0);
					catalogInfo.msgCount++;
				}
				log.debug("SUCCESS on main file" + catalogInfo.msgCount);
				return SUCCESS;
			case 1:
				createMainFile(headerData, catalogInfo.arcName, "preview", catalogInfo.contents.length());
				catalogInfo.msgCount++;
				return SUCCESS;
			case 2:
				createMainFile(headerData, catalogInfo.arcName, "metadata", catalogInfo.sbMetadata.length());
				catalogInfo.msgCount++;
				return SUCCESS;
			case 3:
				createHelpFile(headerData, catalogInfo.arcName, "README.md", 1000);
				catalogInfo.msgCount++;
				return SUCCESS;
			case 4:
				if (getImportantMetadata(catalogInfo, headerData)) {
					log.debug("SUCCESS 10");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			case 5:
				if (getMetadata(catalogInfo, headerData)) {
					log.debug("SUCCESS 11");
					return SUCCESS;
				} else {
					catalogInfo.msgCount++;
					// do not break, let it continue to next case
				}
			case 6:
				if (getLineAsFile(catalogInfo, headerData)) {
					log.debug("SUCCESS 12");
					return SUCCESS;
				}
				// do not break, let it continue to next case
			default:
				return E_END_ARCHIVE;
			}

		} catch (Throwable e) {
			if (log.isErrorEnabled()) {
				log.error(e.getMessage(), e);
			}
			if (catalogInfo.msgCount < 100) {
				headerData.setFileName(e.getMessage() + ".exception");
				catalogInfo.nonBlockingthrowable = e;
				catalogInfo.msgCount++;
				log.debug("SUCCESS after throwable 2",e);
				return SUCCESS;
			} else {
				log.error("E_BAD_DATA",e);
				return E_BAD_DATA;
			}
		}
	}

	private void createMainFile(HeaderData headerData, String arcName, String ext, int size) {
		if (log.isDebugEnabled()) {
			log.debug("createMainFile: arcName=" + arcName + "ext=" + ext + ";size=" + size);
		}
		File f = new File(arcName);
		int index = f.getName().lastIndexOf(".");
		String name = f.getName().substring(0,index);
		f = new File(f.getParent(), name + "." + ext);
		headerData.setFileName(f.getName());
		headerData.setUnpSize(size);
	}
	
	private void createHelpFile(HeaderData headerData, String arcName, String name, int size) {
		if (log.isDebugEnabled()) {
			log.debug("createHelpFile: name=" + name);
		}
		headerData.setFileName(name);
		headerData.setUnpSize(size);
	}

	private boolean getMetadata(CatalogInfo catalogInfo, HeaderData headerData) {
		String[] metadataNames = catalogInfo.metadataNames;
		int contador = catalogInfo.metadataCounter++;
		if (contador < metadataNames.length) {
			String name = metadataNames[contador];
			String value = catalogInfo.metadataValues[contador].replaceAll("[^a-zA-Z0-9]","-");
			String ne = name + "=" + value;
			headerData.setFileName("all-metadata\\" + ne  + ".metadata");
			headerData.setUnpSize(catalogInfo.sbMetadata.length());
			return true;
		} else {
			return false;
		}
	}
	
	private boolean getImportantMetadata(CatalogInfo catalogInfo, HeaderData headerData) {
		String[] metadataNames = catalogInfo.importantMetadataNames;
		int contador = catalogInfo.importantMetadataCounter++;
		if (contador < metadataNames.length) {
			String name = metadataNames[contador];
			String value = catalogInfo.importantMetadataValues[contador];
			String ne = name + "=" + value.replaceAll("[^a-zA-Z0-9 ]","-");
			headerData.setFileName("_" + ne  + ".metadata");
			headerData.setUnpSize(catalogInfo.sbMetadata.length());
			return true;
		} else {
			return false;
		}
	}

	private boolean getLineAsFile(CatalogInfo catalogInfo, HeaderData headerData) {
		int contador = catalogInfo.linesAsFilesCounter++;
		if (contador < catalogInfo.linesAsFiles.size()) {
			headerData.setFileName(twoDigits(contador) + "." + catalogInfo.linesAsFiles.get(contador)  + ".line");
			headerData.setUnpSize(catalogInfo.linesAsFiles.get(contador).length());
			return true;
		} else {
			return false;
		}
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

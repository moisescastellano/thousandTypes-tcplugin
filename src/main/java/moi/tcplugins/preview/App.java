package moi.tcplugins.preview;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class App 
{
   public static void main(final String[] args) throws IOException,TikaException, SAXException {

	      BodyContentHandler handler = new BodyContentHandler(10000);
	      Metadata metadata = new Metadata();
	      FileInputStream inputstream = new FileInputStream(new File("document.pdf" ));
	      ParseContext pcontext = new ParseContext();
	      
	      //parsing the document using PDF parser
	      PDFParser pdfparser = new PDFParser();
	      long before = System.currentTimeMillis();
	      try {
	    	  pdfparser.parse(inputstream, handler, metadata,pcontext);
	      } catch (org.apache.tika.exception.WriteLimitReachedException e) {
	    	  // do nothihng;
	      }
	      long after = System.currentTimeMillis();
	      System.out.println("time:" + (after - before) + " millisecs");
	     
	      //getting the content of the document
	      // System.out.println("Contents of the PDF :" + handler.toString());
	      System.out.println("Contents of the PDF :" + handler.toString().length() + " bytes length" );
	      
	      //getting metadata of the document
	      System.out.println("Metadata of the PDF:");
	      String[] metadataNames = metadata.names();
	      
	      for(String name : metadataNames) {
	         System.out.println(name+ " : " + metadata.get(name));
	      }
	  }
}

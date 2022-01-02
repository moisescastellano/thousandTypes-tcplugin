package moi.tcplugins.preview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class ConfigTest {
	
	final static String CONFIG_FILE = "resources/config.yaml";

	@Test
	void testWriter() throws StreamWriteException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		
		Config config = new Config();
		
		Config.ShowableHelpFile hfr = new Config.ShowableHelpFile(true, "readme.txt", "README.md");
		
		config.helpFiles = new ArrayList<>();
		config.helpFiles.add(hfr);
		config.helpFiles.add(new Config.ShowableHelpFile(false, "changes.txt", "changes.md"));
		
		Config.Format defaultFormat = new Config.Format();
		defaultFormat.allMetadataDir.itemName = "every-metadata/%NAME%=%VALUE%.metadata";
		config.defaultFormat = defaultFormat;

		Config.SpecificFormat pdfFormat = new Config.SpecificFormat();
		pdfFormat.extension = "pdf";
		pdfFormat.specificMetadata = new ArrayList<>();
		pdfFormat.specificMetadata.add(new Config.SpecificMetadata(true, "title", "pdf:docinfo:title"));
		pdfFormat.specificMetadata.add(new Config.SpecificMetadata(true, "creator", "pdf:docinfo:creator"));

		Config.SpecificFormat wordFormat = new Config.SpecificFormat();
		wordFormat.extension = "docx";

		config.specificFormats = new ArrayList<>();
		config.specificFormats.add(pdfFormat);
		config.specificFormats.add(wordFormat);
		
		File f = new File(CONFIG_FILE);
		mapper.writeValue(f, config);
		assert(f.exists());			
	}

	@Test
	void testReader() throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		
		File f = new File(CONFIG_FILE);
		Config config = mapper.readValue(f, Config.class);
		// assert(config.contents.equals("contra"));
	}

}

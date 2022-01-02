package moi.tcplugins.preview;

import java.util.List;

public class Config {
	
	public static class ShowableItem {
		public boolean show = true;
		public String itemName;
		public ShowableItem() {			
		}
		public ShowableItem(boolean show, String itemName) {
			this.show = show;
			this.itemName = itemName;
		}
	}

	public static class ShowableMaxSizeFile extends ShowableItem {
		public long maxSize = -1;
		public ShowableMaxSizeFile() {
			super();
		}
		public ShowableMaxSizeFile(boolean show, String itemName, long maxSize) {
			super(show, itemName);
			this.maxSize = maxSize;
		}
	}

	public static class ShowableHelpFile extends ShowableItem {
		public String originFileName;
		public ShowableHelpFile(boolean show, String itemName, String originFileName) {
			super(show, itemName);
			this.originFileName = originFileName;
		}
	}
	
	public static class Format {
		public ShowableItem allMetadataDir = new ShowableItem (true, "all-metadata/%NAME%=%VALUE%.metadata");
		public ShowableItem allMetadataFile = new ShowableItem (true, "%DOCUMENT%.metadata");
		public ShowableMaxSizeFile preview = new ShowableMaxSizeFile(true, "%DOCUMENT%.preview", 10000);
		public ShowableMaxSizeFile contents = new ShowableMaxSizeFile(true, "%DOCUMENT%.txt", -1);
		public LinesAsFiles linesAsfiles = new LinesAsFiles();
	}

	public static class SpecificFormat extends Format {
		public String extension;
		public List<SpecificMetadata> specificMetadata;
	}
	
	public static class SpecificMetadata extends ShowableItem {
		public int maxLength = 200;
		public String metadataName;
		public SpecificMetadata(String metadataName) {
			super(true, "_%NAME%=%VALUE%.metadata");
			this.metadataName = metadataName;
		}
		public SpecificMetadata(boolean show, String itemName, String metadataName) {
			super(show, itemName);
			this.metadataName = metadataName;
		}
	}
	
	public static class LinesAsFiles extends ShowableItem {
		public int maxLength = 70;
		public int minLength = 50;
		public int numberOfLines = 20;
		public LinesAsFiles() {
			super(true,"%NUMBER% %LINE%.line");
		}		 
	}

	public List<ShowableHelpFile> helpFiles;
	public Format defaultFormat;
	public List<SpecificFormat> specificFormats;

	
}

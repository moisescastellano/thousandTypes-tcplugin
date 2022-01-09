package moi.tcplugins.preview;

import java.util.List;

public class Config {
	
	public static class ShowableItem {
		public boolean show = true;
		public int maxLength = 200;
		public String itemName;
		public ShowableItem() {			
		}
		public ShowableItem(boolean show, int maxLength, String itemName) {
			this.show = show;
			this.maxLength = maxLength;
			this.itemName = itemName;
		}
	}

	public static class ShowableMaxSizeFile extends ShowableItem {
		public int maxSize = -1;
		public ShowableMaxSizeFile() {
			super();
		}
		public ShowableMaxSizeFile(boolean show, int maxLength, String itemName, int maxSize) {
			super(show, maxLength, itemName);
			this.maxSize = maxSize;
		}
	}

	public static class ShowableHelpFile extends ShowableItem {
		public String originFileName;
		public ShowableHelpFile() {			
		}
		public ShowableHelpFile(boolean show, int maxLength, String itemName, String originFileName) {
			super(show, maxLength, itemName);
			this.originFileName = originFileName;
		}
	}
	
	public static class Format {
		public ShowableItem allMetadataDir = new ShowableItem (true, 200, "metadata\\%NUMBER%. %NAME%=%VALUE%.metadata");
		public ShowableItem allMetadataFile = new ShowableItem (true, 200, "%DOCUMENT%.metadata");
		public ShowableMaxSizeFile preview = new ShowableMaxSizeFile(true, 200, "%DOCUMENT%.preview", 10000);
		public ShowableMaxSizeFile contents = new ShowableMaxSizeFile(true, 200, "%DOCUMENT%.txt", 50000000);
		public LinesAsFiles linesAsfiles = new LinesAsFiles();
	}

	public static class SpecificFormat extends Format {
		public String extension;
		public List<SpecificMetadata> specificMetadata;
	}
	
	public static class SpecificMetadata extends ShowableItem {
		public String metadataName;
		public SpecificMetadata() {			
		}
		public SpecificMetadata(boolean show, int maxLength, String itemName, String metadataName) {
			super(show, maxLength, itemName);
			this.metadataName = metadataName;
		}
	}
	
	public static class LinesAsFiles extends ShowableItem {
		public int minLength = 60;
		public int numberOfLines = 20;
		public LinesAsFiles() {
			super(true,75,"%NUMBER% %LINE%.line");
		}		 
	}

	public List<ShowableHelpFile> helpFiles;
	public Format defaultFormat;
	public List<SpecificFormat> specificFormats;

	
}

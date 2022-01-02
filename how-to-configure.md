How to configure this preview plugin
==================================================================================

## Configuring how information is presented

### (In next version, coming soon)

You can [easily configure](how-to-configure.md) the plugin to show or hide every element in the "simulated folder", and how is it presented:

 - Document metadata shown as file (that can be extracted) or sub-directory (metadata shown as file names)
 - Specific important metadata for each doc format, shown as file names
 - Document contents preview text file, size configurable (just parsing up to that size on opening makes it quicker)
 - Whole document contents text file
 - Document contents shown as file names, so that you dont even have to open any file in the folder. Max and min length.
 - Help documentation
 - (Upcoming versions) Translation to other languages

This configuration can be done globally or per specific format.

### To be written...

**(Coming soon)**
Here is how can you do it

## Configuring a thousand doc formats

By default the plugin comes just associated to PDF files, for two reasons:
 - AFAIK, the Total Commander packer plugin **installation** process only let it associate to one extension
 - All Apache Tika parsers size is over 50 MBs. So the plugin is distributed just with the PDF parsers and Tika core libraries
 
Don't panic! TC lets you associate more extensions to the plugin and you can easily [download and configure all the Tika parsers](how-to-configure.md).

### (In next version, coming soon)


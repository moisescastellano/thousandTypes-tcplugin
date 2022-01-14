The Thousand File Types Preview [Total Commander](https://www.ghisler.com/) plugin
==================================================================================

Have you ever being checking what's inside of a lot of documents such as PDFs or .doc files, 
spending a lot of time waiting for a new Acrobat Reader, MS-Word or Whatever-program to open, just to close it and continuing the process?

This plugin comes in two versions: packer and lister plugin.

Packer plugin allows Total Commander to **very quickly (*)** "enter" (ctrl + pgDown) docs as if they were archives or folders. 
In that "simulated folder" you can see at a glance:
 - a plain-text preview (or whole contents) of the document, that you can then view(F3) or extract (F5)
 - document metadata: author, version, creator tool...
 - first lines preview of the contents shown as file names, so that you dont even have to open any file in the folder

This info is shown as file names, so that you can have a very quick preview, and then if needed extract to a file or view the complete document's metadata or contents.

Lister plugin coming soon.

(Image)

(*) The first time you open a doc it will take a couple seconds as the JVM loads into memory; then the preview is as quick as entering a folder in the local system

### A thousand formats is an exaggeration, right?

No, it is for real. 
The plugin is based on [Apache Tika](https://tika.apache.org/), a toolkit that detects and extracts metadata and text from over a thousand different file types.
Tika has also translation capabilities, to be incorporated in upcoming versions of the plugin.

### Configure it based on your preferences

**(Configuration coming soon)**
You can [easily configure](how-to-configure.md) the plugin to show or hide every element in the "simulated folder", and how is it presented.

This configuration can be done globally or per specific format.

### Why can I only see PDF files associated to the plugin?

By default the plugin comes just associated to PDF files, for two reasons:
 - AFAIK, the Total Commander packer plugin **installation** process only let it associate to one extension
 - All Apache Tika parsers size is over 50 MBs. So the plugin is distributed just with the PDF parsers and Tika core libraries
 
Don't panic! TC lets you associate more extensions to the plugin and you can easily [download and configure all the Tika parsers](how-to-configure.md).
**(How to configure thousand types coming soon)**


Download and resources
----------------------
- Download the [latest release in this project](https://github.com/moisescastellano/thousand-preview-tcplugin/blob/main/releases)
- [Plugin page at totalcmd.net](http://totalcmd.net/plugring/thousand-preview.html)
- [Github page](https://moisescastellano.github.io/thousand-preview-plugin/)
- [Github project](https://github.com/moisescastellano/thousand-preview-plugin)
- TO BE UPDATED: [Thread for discussing this plugin](https://www.ghisler.ch/board/viewtopic.php?t=75793) at the TC forum
- This is a work in progress, you can help with [things to do](https://moisescastellano.github.io/thousand-preview-plugin/to-do)

Troubleshooting guide
-----------------------------------

This interface and all derived plugins are written in Java, so you need to have installed a [Java Runtime Environment (JRE)](https://www.java.com/en/download/manual.jsp). The Java plugin interface and derived plugins were tested on **Oracle (Sun) JRE 1.8**  (jre-8u311-windows-x64.exe).

In case you have any of the following issues, refer to the [Troubleshooting guide](https://moisescastellano.github.io/tcmd-java-plugin/troubleshooting)
- In case you have more than one Java plugin installed
- Be sure you use the same (32/64) platform for JVM and TC
- In case you have both TCx64 and TCx32 installed
- Error *Java Runtime Environment is not installed on this Computer*
- Error *LoadLibrary Failed*
- Error *Starting Java Virtual Machine failed*
- Error *Class not found class='tcclassloader/PluginClassLoader'*
- Error *Initialization failed in class...*
- Error *Exception in class 'tcclassloader/PluginClassLoader'*
- Error *Access violation at address...*
- Error *Crash in plugin ... Access violation at address...*]

For other issues check the next paragraph.

Issues and things to-do
----------------------
This is a work in progress. **Help wanted!** - in particular with Visual C++ issues.
 - Refer to [things to do](https://github.com/moisescastellano/thousand-preview-plugin/blob/main/to-do.md) for work in progress.
 - Check also the [issues page](https://github.com/moisescastellano/thousand-preview-plugin/issues) for this plugin.
 - Java Plugin Interface's [issues page](https://github.com/moisescastellano/tcmd-java-plugin/issues).

Contact
----------------------

If you have any comment, suggestion or problem regarding this java plugin,
you can contact me at:
 - email: moises.castellano (at) gmail.com
 
 - [Github project issues page](https://github.com/moisescastellano/thousand-preview-tcplugin/issues)

Please detail the specific version of: Java plugin interface, Total Commander and JRE that you are using.

Disclaimer
----------------------
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


License
----------------------
Licensed under under the GNU General Public License v3.0, a strong copyleft license:
https://github.com/moisescastellano/thousand-preview-plugin/blob/main/LICENSE





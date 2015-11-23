# Archivo
Archivo is a cross-platform app for archiving recordings from a TiVo to your computer. It is designed to "just work" and require very little user input or configuration.

## Features
Archivo is still in development and may have some rough edges. It currently supports the following features:

* Automatically find TiVos on your local network
* List the recordings available on each TiVo
* Copy recordings from a TiVo to a computer
* Convert recordings to more convenient video formats
* Remove commercials from recordings
* Delete recordings from the TiVo

## Requirements
* Java 8u40 or higher
* A TiVo Bolt, Roamio, or Premiere
* For best performance, your computer should have at least a dual-core processor and 1 GB of memory
* Plenty of disk space: archiving a one-hour HD recording may temporarily use up to 10 GB of disk space
* Supported operating systems:
  * Mac OS X 10.7.3 and higher
  * Windows Vista and higher
  * Any Linux system with Java 8, FFmpeg 2.8 or higher, HandBrake CLI (command-line interface) 0.10.2 or higher, and Comskip 0.81.89 or higher. The FFmpeg, HandBrake CLI, and Comskip executables must all be in the same directory (e.g., /usr/local/bin) with the names "ffmpeg", "handbrake", and "comskip", and Archivo needs to be run with the `-tooldir` parameter specifying this directory (e.g., `java -jar /path/to/archivo.jar -tooldir /usr/local/bin`).

## Downloads
The latest release of Archivo can be found at https://github.com/fflewddur/archivo/releases. We provide packages for Windows and Mac OS X. Archivo also runs on Linux, but we do not currently provide pre-compiled Linux packages.

## Known Issues
If you find a problem with Archivo, please help us fix it by reporting it at https://github.com/fflewddur/archivo/issues.

## Development
Archivo is Free Software, so everyone is welcome to view, modify, and redistribute its source code. The only requirement is that you must in turn allow everyone to view, modify, and redistribute your own modifications (learn more about this topic at http://www.gnu.org/licenses/quick-guide-gplv3.en.html). If you're interested in joining the development, check us out on GitHub at https://github.com/fflewddur/archivo/.

## Acknowledgments

Development of this project was made substantially easier by the existence of the excellent KMTTG project (https://sourceforge.net/projects/kmttg/). If you want more control over your TiVo than Archivo gives you, KMTTG is probably the tool you're looking for.

Archivo also makes use of the following Free Software projects:
* **FFmpeg** - For repairing video files and trimming commercials (https://www.ffmpeg.org/)
* **Comskip** - For detecting commercials (http://www.kaashoek.com/comskip/)
* **HandBrake** - For converting video files to more convenient formats (https://handbrake.fr/)
* **TivoLibre** - For decoding TiVo recordings into standard MPEG-TS files (https://github.com/fflewddur/tivolibre)

Thanks to Freepik (http://www.freepik.com/) for providing the icon the Archivo logo is based upon.
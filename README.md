# Archivo
Archivo is a cross-platform app for archiving recordings from a TiVo to your computer. It is designed to "just work" and require very little user input or configuration.

## Features
Archivo is currently in development. It will eventually support the following features:

* Automatically find TiVos on your local network (*complete*)
* List the recordings available on each TiVo (*complete*)
* Copy recordings from a TiVo to a computer (*complete*)
* Convert recordings to more convenient video formats (**in progress**)
* Remove commercials from recordings (**in progress**)
* Delete recordings from the TiVo (*complete*)

## Requirements
* Java 8u40 or higher
* A TiVo Roamio or Premiere (Series 4 and Series 5)
* For best performance, your computer should have at least a dual-core processor and 1 GB of memory
* Plenty of disk space: archiving a one-hour HD recording may temporarily use up to 10 GB of disk space

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
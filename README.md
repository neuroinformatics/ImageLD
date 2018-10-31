# ImageLD - Light/Dark transition test plugin for ImageJ

ImageLD is an ImageJ plugin for the Light/dark transition test.
This program can work with Image J 1.46 or above on Windows 7 or 10 (32-bit).


## How to build

### prepare
Download java inteface for LabJack U12

```shellscript
 $ curl -LO https://labjack.com/sites/default/files/2012/08/LabJackJavaV40.zip 
 $ unzip -p LabJackJavaV40.zip labjack.jar > lib/labjack.jar
```

Download java package for Scion Frame Grabber FG-7

```shellscript
 $ curl -LO http://imagej.nih.gov/ij/download/tools/ScionDrivers.zip
 $ unzip -p ScionDrivers.zip ScionImageJDrivers/FG-7/ImageJ/SFG_ImageJ_Update64.exe > SFG_ImageJ_Update64.exe
 $ cabextract -p -F scion.jar SFG_ImageJ_Update64.exe > lib/scion.jar 
```

### compile
Build jar package using Maven

```shcellscript
 $ mvn package
 $ file target/behavior_LD130403.jar
```

## Binary distribution

https://cbsn.neuroinf.jp/modules/xoonips/detail.php?id=ImageLD

Distributed by Keizo Takao and Tsuyoshi Miyakawa.

In publication of data that is analyzed with ImageLD, cite the following article that describes the method of the Light/dark transition test using this software.

Takao, K., Miyakawa, T. Light/dark Transition Test for Mice. J. Vis. Exp. (1), e104, doi:10.3791/104 (2006).

To get revisions info about ImageLD, please see http://www.mouse-phenotype.org/software.html.

In this version, the size/shape of the light and dark compartments should be identical to each other.
The dark compartment should located the left side at the image.


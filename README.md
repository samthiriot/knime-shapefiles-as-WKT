## knime-shapefiles-as-WKT

### what is it?

This collection of nodes for the [KNIME scientific workflow engine](https://en.wikipedia.org/wiki/KNIME)
offer to manipulate spatial data in KNIME. 
Geometries are decoded and manipulated as their [Well-Known Text representation](https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry) (WKT), which are stored as native KNIME Strings. 

The collection offers nodes to read shapefiles as KNIME data tables and store KNIME data tables as shapefiles.

![Screenshot of the spatial nodes](docs/capture_main.png)

### Install it from update site 

In KNIME, add as an external repository: https://knime.res-ear.ch .

### how does it work?

All the smart work is done by the wonderful [geotools](https://en.wikipedia.org/wiki/GeoTools) and [JTS](https://en.wikipedia.org/wiki/JTS_Topology_Suite).[opensource](https://en.wikipedia.org/wiki/Open_source) libraries.
We only do provide the integration of these features inside KNIME. 
We currently integrated the library in its snapshot version 22, because we had to submit small changes to the library to integrate it smoothly.



## Documentation

See [the documentation and examples](https://samthiriot.github.io/knime-shapefiles-as-WKT/).


## License

These nodes were developed for the [European Institute for Energy Research (EIFER)](https://www.eifer.kit.edu/).
They are notably used for Generation of Synthetic Populations (GoSP), in order to read spatial populations. 
These nodes are released as GPL v3; see the [Free Software Foundation presentation](https://www.gnu.org/licenses/quick-guide-gplv3.en.html) if you're not familiar with open-source licenses.


## Developers

### Development environment

In order to create a development environment, follow the [instructions to create a KNIME development environment](https://github.com/knime/knime-sdk-setup).

### Build

The plugin includes a Maven pom file in order to package the dependancies of geotools into the lib directory. 
This only has to be done by hand from time to time (to upgrade the geotools version, or to add novel geotools dependancies).
Else the build is pure Java/RCP. 

### Testing 

These nodes are tested automatically by [Jenkins in the KNIME servers](https://community.knime.org/jenkins/job/ch.res_ear.samthiriot.knime.shapefilesaswkt.update-trunk/). 
These tests run test workflows and ensure the nodes under test work as expected.

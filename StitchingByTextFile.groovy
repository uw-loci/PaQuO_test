// Further modified by Michael Nelson
// Expected directory structure is within a project folder, there will be a Tiles/imageName/*.tif and TileConfiguration.txt
// For example, C:/QuPathProject/Tiles/Sample1/TileConfiguration.txt

//Modified by Colt Egelston and Jian Ye to do a directory of folders.
//Change Line 34 to reflect folder locations. 

/**
 * Convert TIFF fields of view to a pyramidal OME-TIFF.
 *
 * Locations are parsed from the baseline TIFF tags, therefore these need to be set.
 *
 * One application of this script is to combine spectrally-unmixed images.
 * Be sure to read the script and see where default settings could be changed, e.g.
 *   - Prompting the user to select files (or using the one currently open the viewer)
 *   - Using lossy or lossless compression
 *
 * @author Pete Bankhead
 */
//For a FULL SIZE IMAGE baseDownsample = 1
double baseDownsample = 1


//*Change the below Folder Location!*
tileDirectory = buildPathInProject("Tiles")
def qupath = getQuPath()
def rootdir = Paths.get(tileDirectory)
String arg = "20x" // Replace with the desired string to search for

Logger logger = LoggerFactory.getLogger(QuPathGUI.class);
def subdir = []
try {
    Files.newDirectoryStream(rootdir).each { path ->
        if (Files.isDirectory(path) && path.fileName.toString().contains(arg)) {
            subdir.add(path)
        }
    }
} catch (IOException e) {
    e.printStackTrace()
}

if (subdir.isEmpty()) {
    throw new RuntimeException("No folders found containing the string '$arg'")
}

// At this point, we should have a list of folder locations in subdir
for (folderOfTilesPath in subdir) {
    Path dir = Paths.get(folderOfTilesPath.toString())

    println "Processing slide in folder $dir"

    
    // Check for TileConfiguration.txt
    Path tileConfigPath = dir.resolve("TileConfiguration.txt")
    if (!Files.exists(tileConfigPath)) {
        println "Skipping folder as TileConfiguration.txt is missing: $dir"
        continue
    }
    def tileConfig = parseTileConfiguration(tileConfigPath.toString())

    // Collect only .tif files
    def files = []
    Files.newDirectoryStream(dir, "*.tif*").each { path ->
        files.add(path.toFile())
    }
    
    File fileOutput
    String filename = dir.getFileName().toString()
    def outputPath
    if (baseDownsample == 1){
        outputPath = rootdir.resolve(filename + '.ome.tif')
    } else {
        outputPath = rootdir.resolve(filename + '_' + (int) baseDownsample + 'x_downsample.ome.tif')
        print outputPath
    }
        
    fileOutput = outputPath.toFile()
    println "Output stitched file will be $fileOutput"
    
    
    // Parse image regions & create a sparse server
    print 'Parsing regions from ' + files.size() + ' files...'

    def builder = new SparseImageServer.Builder()
    files.parallelStream().forEach { f ->
        def region = parseRegion(f, tileConfig)
        if (region == null) {
            print 'WARN: Could not parse region for ' + f
            return
        }
        def serverBuilder = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, f.toURI().toString()).getBuilders().get(0)
        builder.jsonRegion(region, 1.0, serverBuilder)
    }
    print 'Building server...'
    def server = builder.build()
    server = ImageServers.pyramidalize(server)
    
    long startTime = System.currentTimeMillis()
    String pathOutput = fileOutput.getAbsolutePath()
    new OMEPyramidWriter.Builder(server)
        .downsamples(server.getPreferredDownsamples()) // Use pyramid levels calculated in the ImageServers.pyramidalize(server) method
        .tileSize(512)      // Requested tile size
        .channelsInterleaved()      // Because SparseImageServer returns all channels in a BufferedImage, it's more efficient to write them interleaved
        .parallelize(true)              // Attempt to parallelize requesting tiles (need to write sequentially)
        //.losslessCompression()      // Use lossless compression (often best for fluorescence, by lossy compression may be ok for brightfield)
        .lossyCompression()
        .scaledDownsampling(baseDownsample, 4)
        .build()
        .writePyramid(pathOutput)
    long endTime = System.currentTimeMillis()
    print('Image written to ' + pathOutput + ' in ' + GeneralTools.formatNumber((endTime - startTime)/1000.0, 1) + ' s')
    server.close()
}     


static ImageRegion parseRegion(File file,List<Map> tileConfig, int z = 0, int t = 0) {

    try {
        //return parseRegionFromTIFF(file, z, t)
        return parseRegionFromTileConfig(file, tileConfig)
    } catch (Exception e) {
            logger.info(e.getLocalizedMessage())
    }
}



/**
 * Parse an ImageRegion from the TileConfiguration.txt data and TIFF file dimensions.
 * @param imageName Name of the image file for which to get the region.
 * @param tileConfig List of tile configurations parsed from TileConfiguration.txt.
 * @param z index of z plane.
 * @param t index of timepoint.
 * @return An ImageRegion object representing the specified region of the image.
 */
static ImageRegion parseRegionFromTileConfig(File file, List<Map> tileConfig, int z = 0, int t = 0) {
    String imageName = file.getName()
    def config = tileConfig.find { it.imageName == imageName }

    if (config) {
        int x = config.x as int
        int y = config.y as int
        def dimensions = getTiffDimensions(file)
        if (dimensions == null) {
            logger.info(  "Could not retrieve dimensions for image $imageName")
            return null
        }
        int width = dimensions.width
        int height = dimensions.height
        //logger.info( x+" "+y+" "+ width+ " " + height)
        return ImageRegion.createInstance(x, y, width, height, z, t)
    } else {
        logger.info(  "No configuration found for image $imageName")
        return null
    }
}



static Map<String, Integer> getTiffDimensions(filePath) {

    if (!filePath.exists()) {
        logger.info("File not found: $filePath")
        return null
    } 

    try {
        def image = ImageIO.read(filePath)
        if (image == null) {
            logger.info("ImageIO returned null for file: $filePath")
            return null
        }
        return [width: image.getWidth(), height: image.getHeight()]
    } catch (IOException e) {
        logger.info("Error reading the image file $filePath: ${e.message}")
        return null
    }
}


/**
 * Parses the 'TileConfiguration.txt' file to extract image names and their coordinates.
 * The function reads each line of the file, ignoring comments and blank lines.
 * It extracts the image name and coordinates, then stores them in a list.
 *
 * @param filePath The path to the 'TileConfiguration.txt' file.
 * @return A list of maps, each containing the image name and its coordinates (x, y).
 */
def parseTileConfiguration(String filePath) {
    def lines = Files.readAllLines(Paths.get(filePath))
    def images = []

    lines.each { line ->
        if (!line.startsWith("#") && !line.trim().isEmpty()) {
            def parts = line.split(";")
            if (parts.length >= 3) {
                def imageName = parts[0].trim()
                def coordinates = parts[2].trim().replaceAll("[()]", "").split(",")
                images << [imageName: imageName, x: Double.parseDouble(coordinates[0]), y: Double.parseDouble(coordinates[1])]
            }
        }
    }

    return images
}


import qupath.lib.common.GeneralTools
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.images.servers.ImageServers
import qupath.lib.images.servers.SparseImageServer
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.regions.ImageRegion

import javax.imageio.ImageIO
import javax.imageio.plugins.tiff.BaselineTIFFTagSet
import javax.imageio.plugins.tiff.TIFFDirectory
import java.awt.image.BufferedImage

import static qupath.lib.gui.scripting.QPEx.*

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;


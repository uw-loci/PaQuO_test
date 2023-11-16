pythonVirtualEnvironmentExecutable = "C:/Anaconda/envs/paquo/python.exe"
pythonScriptsFolder = "D:/Python/paquo/PaQuO_test/"


// a unique identifier to tag the items we will add to the toolbar
def btnId = "custom"

// Convert a basic image into a .gif file using Fiji, then converted to base64 using 
imageStringLocation = buildFilePath(pythonScriptsFolder, "pycromanagerIcon.txt")
String imageString = new File(imageStringLocation).getText('UTF-8')

Image iconImg = imageStringToIcon(imageString)

// Remove all the additions made to the toolbar based on the id above
def RemoveToolItems(toolbar, id) {
    while(1) {
        hasElements = false
        for (var tbItem : toolbar.getItems()) {
            if (tbItem.getId() == id) {
                toolbar.getItems().remove(tbItem)
                hasElements = true
                break
            }
        }
        if (!hasElements) break
    }
}

Platform.runLater {
    gui = QuPathGUI.getInstance()
    toolbar = gui.getToolBar()

    // First we remove the items already in place    
    RemoveToolItems(toolbar,btnId)

    // Here we add a separator
    sepCustom = new Separator(Orientation.VERTICAL)
    sepCustom.setId(btnId)
    toolbar.getItems().add(sepCustom)
    ////////////////////////////////////////////////////////////////////
    Button saveButton = new Button("Export Annotations")
    saveButton.setId(btnId)
    toolbar.getItems().add(saveButton)
    ImageView imageView = new ImageView(iconImg)
    saveButton.setGraphic(imageView)
    saveButton.setTooltip(new Tooltip("Save the current annotations to a JSON file"))
    saveButton.setOnAction {e ->
        //Set up the name and location for the export
        folderLocation = buildFilePath(PROJECT_BASE_DIR, 'ExportedAnnotations')
        mkdirs(folderLocation)
        name = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())+".json"
        fileLocation = buildFilePath(folderLocation, name)
        resetSelection()
        selectAnnotations()
        exportSelectedObjectsToGeoJson(fileLocation, "PRETTY_JSON", "FEATURE_COLLECTION")
     }
     ///////////////////////////////////////////////////////////////////////////////
     //Should not be necessary with new breakdown of commands in context menu
     //////////////////////////////////////////////////////////////////////////////////
     
    // Here we add a Context menu (right click)
    ContextMenu contextMenu = new ContextMenu()
    // Creating the menu Items for the context menu (Option 2 is check menu item)
    
    MenuItem initialize = new MenuItem("Initialize")
    MenuItem autofocusAndAcquire = new MenuItem("Autofocus and acquire full tissue")
    MenuItem stitchAndLoad = new MenuItem("Stitch and load image")
    MenuItem scanWithModality = new MenuItem("Scan annotated areas")
    MenuItem help = new MenuItem("Get help!")
    
    //CheckMenuItem item2 = new CheckMenuItem("Option 2")
    contextMenu.getItems().addAll(initialize, autofocusAndAcquire, stitchAndLoad, scanWithModality, help)
    // Setting the ContextMenuItem to the button
    saveButton.setContextMenu(contextMenu)
    // Setting action to the context menu item
    initialize.setOnAction{e->
        args = ["hello world", getQuPath().getProject().getName().split('/')[0]]
        callPythonCode(pythonScriptsFolder,pythonVirtualEnvironmentExecutable, "initialize_qupath.py", args)
        //1 Verify existence of text files, and create project specific text files?
        //2 Dialog that requests the name of the sample, metadata fields (PDAC, pancreatitis), overall project name
        //3 python script that calls paquo environment (checks for existence of) to create a blank project
        
    }
    autofocusAndAcquire.setOnAction{e->
        //1 python script that calls paquo environment (checks for existence of) to create a blank project
        //2 User verifies that autofocus works. Ability to focus manually, move stage?
        
        //This line ASSUMES you have only one image, which is the design of this particular workflow.
        
        ProjectImageEntry = getQuPath().getProject().getImageList()[0]
        
        getQuPath().openImageEntry(ProjectImageEntry)
        getQuPath().refreshProject()
    }
    stitchAndLoad.setOnAction{e->
        //1 python script that calls paquo environment (checks for existence of) to stitch a target folder of tiff images
        //2 upon successful completion of first script, second script uses paquo to add image to environment
        //3 Create popup requesting that user draw annotations or run a script that creates annotations for further scanning
    } 
    scanWithModality.setOnAction{e->
        //1 Select a modality from a list generated from the json
        //2 python function that performs a scan within the indicated annotation areas
        //3 Same steps as stitch and load into project, include metadata
    } 
    
    
    help.setOnAction{e->
        if (Desktop.isDesktopSupported()){
            Desktop.getDesktop().browse(new URI("https://forum.image.sc/t/rarecellfetcher-a-tool-for-annotating-rare-cells-in-qupath/33654"))
        }
    }



}
def imageStringToIcon(String imageString) {

    ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageString.decodeBase64())
    return new Image(imageInputStream,QuPathGUI.TOOLBAR_ICON_SIZE, QuPathGUI.TOOLBAR_ICON_SIZE, true, true)
}

def callPythonCode(String pythonScriptsFolder, String pythonVirtualEnvironmentExecutable, String pythonFile, args) {
        initialization_location = buildFilePath(pythonScriptsFolder, pythonFile)
        def cmdArray = [pythonVirtualEnvironmentExecutable, initialization_location]+args
        processName = cmdArray.execute()
        // Wait for the process to complete
        processName.waitFor()
        // Read the output stream (standard output)
        def outputStream = processName.getInputStream()
        sout = new StringBuilder()
        outputStream.eachLine { line -> sout.append(line).append("\n") }
        
        // Read the error stream (standard error)
        def errorStream = processName.getErrorStream()
        serr = new StringBuilder()
        errorStream.eachLine { line -> serr.append(line).append("\n") }
        // Print the output and error (if any)
        print("Output:\n" + sout.toString())
        print("Error (if any):\n" + serr.toString())
        
}

import javafx.application.Platform
import javafx.stage.Stage
import javafx.scene.Scene
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.input.MouseEvent
import javafx.beans.value.ChangeListener
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import qupath.lib.gui.QuPathGUI
import java.awt.Desktop
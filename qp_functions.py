from pathlib import Path
from paquo.projects import QuPathProject
from paquo.images import QuPathImageType
import os
import glob
import time

# qpproj_dir_path = 'D:\Python\paquo\qppaquo2'

image_dir = 'D:/Python/paquo/images'

metadata = {
    "PDAC_MetroHealth_362-4x-bf.ome.tif": {
        "acquisition": "4x",
        "diagnosis": "PDAC",
    },
    "PDAC_MetroHealth_N342L31-4x-bf.ome.tif": {
        "acquisition": "4x",
        "diagnosis": "PDAC",
    },
    "PDAC_MetroHealth_N362L124-20x--5505_1777.ome.tif": {
        "acquisition": "20x",
        "diagnosis": "normal",
    },
    "PDAC_MetroHealth_N362L124-20x--6094_3630.ome.tif": {
        "acquisition": "20x",
        "diagnosis": "normal",
    },
}

def create_metadata_list(file_names, acquisition, diagnosis=None):
    """
    Create a metadata dictionary for a list of file names or a directory containing image files
    with a common acquisition setting and diagnosis.

    Parameters:
    file_names (list or str): List of file names or directory path containing image files.
    acquisition (str): Acquisition setting to apply to all files.
    diagnosis (str, optional): Diagnosis to apply to all files. Defaults to None.

    Returns:
    dict: A dictionary containing the metadata for each file.
    """
    if isinstance(file_names, str) and os.path.isdir(file_names):
        # List all image files in the given directory
        image_extensions = ['*.svs', '*.ndpi', '*.ome.tif']
        all_files = []  # Use a different variable name to store the file names
        for ext in image_extensions:
            all_files.extend(glob.glob(os.path.join(file_names, ext)))
        # Extract just the file names
        file_names = [os.path.basename(f) for f in all_files]

    return {
        file_name: {"acquisition": acquisition, "diagnosis": diagnosis}
        for file_name in file_names
    }



def ensure_directories_exist(dir_path, create=False):
    """
    Ensure that all directories in the given path exist and are empty. If they do not exist,
    this function attempts to create them. If they exist but are not empty, it raises an error.

    Parameters:
    dir_path (str): The file system path for which to ensure directory existence.

    Returns:
    bool: True if the directory exists (and is empty) or was successfully created, False otherwise.
    """
    # Normalize the path to be compatible with the current OS
    normalized_path = os.path.normpath(dir_path)

    if os.path.isdir(normalized_path):
        # Check if directory is empty
        if os.listdir(normalized_path):  # This returns a non-empty list if the directory is not empty
            raise ValueError(f"The target directory {normalized_path} MUST BE EMPTY!")
        return True

    if create:
        try:
            os.makedirs(normalized_path, exist_ok=True)
            return True
        except Exception as e:
            print(f"Error creating directory {normalized_path}: {e}")
            return False



# def validate_class_list(class_list):
#     """
#     Validates the structure and content of the class_list. Each item in the list
#     should be a tuple of (class name, color code).

#     Parameters:
#     class_list (list): A list of tuples, each containing a class name and a color code.

#     Returns:
#     bool: True if the class_list is valid, False otherwise.
#     """
#     if not isinstance(class_list, list):
#         return False

#     for item in class_list:
#         if not isinstance(item, tuple) or len(item) != 2:
#             return False
#         class_name, class_color = item
#         if not isinstance(class_name, str) or not isinstance(class_color, str):
#             return False
#         # Additional checks can be added here (e.g., regex for color code)

#     return True



def add_images_to_project(qp, image_sources, metadata=None):
    """
    Adds images to a QuPath project, with or without metadata.
    CALL THIS FUNCTION TWICE IF YOU HAVE A MIX OF METADATA/NO METADATA
    All images should either have or not have metadata for any individual add.

    Parameters:
    qp (QuPathProject): The QuPath project object to which images will be added.
    image_sources (str or list): The directory path where image files are located, 
                                 or a list of image file paths.
    metadata (dict, optional): A dictionary mapping image filenames to their metadata.

    Returns:
    str: A message indicating the result of the operation.
    """
    # Check if qp is a valid QuPath project object
    if not isinstance(qp, QuPathProject):
        return "Error: Invalid QuPath project object."

    # Determine and validate image files
    image_files = []
    if metadata:
        print("metadata found")
        # If metadata is provided, use its keys as the image file names
        image_files = [os.path.join(image_sources, fname) for fname in metadata.keys()]
    elif isinstance(image_sources, str) and os.path.isdir(image_sources):
        for ext in ('*.svs', '*.ndpi', '*.ome.tif'):
            image_files.extend(glob.glob(os.path.join(image_sources, ext)))
    elif isinstance(image_sources, list) and all(os.path.isfile(f) for f in image_sources):
        image_files = image_sources
    else:
        return "Error: Invalid image source provided."

    # Check if there are any images to add
    if not image_files:
        return "Error: No images found to add to the project."
    # Mapping from acquisition types to QuPathImageTypes
    acquisition_to_image_type = {
        "4x": QuPathImageType.BRIGHTFIELD_H_E,
        "20x": QuPathImageType.BRIGHTFIELD_H_E,
        "LSM": QuPathImageType.FLUORESCENCE
    }
    # Add images to the project
    for image_path in image_files:
        image_basename = os.path.basename(image_path)
        try:
            if metadata and image_basename in metadata:
                image_metadata = metadata[image_basename]

                # Determine image type based on acquisition
                acquisition = image_metadata.get("acquisition", "default")  # Default if acquisition is not specified
                image_type = acquisition_to_image_type.get(acquisition, QuPathImageType.BRIGHTFIELD_H_E)  # Default type

                # Add image with determined type
                entry = qp.add_image(
                    image_path,
                    image_type=image_type
                )
                #print(image_basename)
                #print(image_metadata)
                #entry.metadata = image_metadata #this works the same as below, but doesn't fix the missing metadata issue
                entry.metadata.update(image_metadata)

            else:
                # Default action if metadata is not available
                print(f"Metadata not found for {image_basename}, adding as default image type.")
                qp.add_image(
                    image_path,
                    image_type=QuPathImageType.OTHER
                )
        except FileExistsError:
            print(f"Image already exists in the project: {image_path}")

    return "Images added to the QuPath project successfully."



def create_qp_project(qpproj_dir_path, image_dir=None, metadata=None):
    """
    Creates a QuPath project in the specified directory. Adds images from the image
    directory either with provided metadata or by searching for image files.

    Parameters:
    qpproj_dir_path (str): The directory path where the QuPath project will be created.
    image_dir (str, optional): The directory path where image files are located.
    metadata (dict, optional): A dictionary mapping image filenames to their metadata.

    Returns:
    str: A success message or an error message if the directory cannot be created.
    """

    # Ensure the project directory exists or can be created
    if not ensure_directories_exist(qpproj_dir_path, create=True):
        return f"Error: Unable to create or access the directory {qpproj_dir_path} to create the QuPath project."

    # Create the new project
    with QuPathProject(qpproj_dir_path, mode='x') as qp:
        print("created", qp.name)

        add_images_to_project(qp, image_dir, metadata)
    print(f"QuPath project created successfully in {qpproj_dir_path}")
    return qp


#how do we determine the image to get the annotation from?
#Two ways
# 1 save json from QuPath, Python needs to know where that is - look in active project folder
# 2 Save image in QuPath, Python reads the active project for image entries and pulls annotations out of that

# 2 involves the least additional file creation - but what can we do with shapely 
def get_shapely_annotations(qp):
    with QuPathProject(current_project, mode='r') as qp:
        print("opened", qp.name)
        # TODO REPLACE WITH SEARCH FOR SPECIFIC IMAGE??
        for image in qp.images:
            # annotations are accessible via the hierarchy
            annotations = image.hierarchy.annotations

            print("Image", image.image_name, "has", len(annotations), "annotations.")
            shapely_annotations = []
            for annotation in annotations:
                # annotations are paquo.pathobjects.QuPathPathAnnotationObject instances
                # their ROIs are accessible as shapely geometries via the .roi property
                shapely_annotations.add(annotation.roi)
                print("> class:", annotation.path_class.name, "roi:", annotation.roi)
    return shapely_annotations
        
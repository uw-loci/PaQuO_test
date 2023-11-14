#Try making this into a python function
from paquo._config import settings, to_kwargs
from paquo.jpype_backend import start_jvm, JClass

args = to_kwargs(settings)
qupath_version = start_jvm(finder_kwargs=args)
print(f"QuPath version: {qupath_version}")

import jpype
import jpype.imports
import java
import javax
from javax.swing import *

qupathGUI = JClass('qupath.lib.gui.QuPathGUI')

@jpype.JImplements(java.lang.Runnable)
class launchQuPath:
    @jpype.JOverride
    def run(self):
        qupathGUI.launchQuPath()
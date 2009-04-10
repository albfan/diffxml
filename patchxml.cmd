@rem Utility script to call patchxml with the given arguments
@echo off

java -cp lib\diffxml.jar org.diffxml.patchxml.PatchXML %*

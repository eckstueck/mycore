@echo off
rem ######################################################################
rem #
rem # This script sets some basic environment variables and calls Ant to
rem # build the MyCoRe system from sources. Use on Windows systems.
rem # 
rem # $Revision$ $Date$
rem #
rem #######################################################################

rem #######################################################################
rem #
rem # Change the following variables as needed for your local system:
rem #
rem #######################################################################

rem Directory where JDK is installed on your machine, if not already set
if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Java\j2sdk1.4.0_01

rem Directory where Ant is installed on your machine, if not already set
if "%ANT_HOME%"=="" set ANT_HOME=C:\Java\apache-ant-1.5.3-1

rem #######################################################################
rem #
rem # Do not change the rest of this script!
rem #
rem #######################################################################

%ANT_HOME%\bin\ant -logger org.apache.tools.ant.NoBannerLogger -find build.xml %1 %2 %3 %4 %5 %6 %7 %8 %9

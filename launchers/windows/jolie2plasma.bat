@echo off
java -cp %JOLIE_HOME%\jolie.jar;%JOLIE_HOME%\lib\libjolie.jar:%JOLIE_HOME%\tools\jolie2plasma.jar joliex.plasma.Jolie2Plasma -l %JOLIE_HOME%\lib;%JOLIE_HOME%\javaServices\*;%JOLIE_HOME%\extensions\* -i %JOLIE_HOME%\include -p %JOLIE_HOME%\packages %*

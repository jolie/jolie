@echo off
java -ea:jolie... -ea:joliex... -cp %JOLIE_HOME%\jolie-cli.jar;%JOLIE_HOME%\jolie.jar;%JOLIE_HOME%\lib\libjolie.jar;%JOLIE_HOME%\tools\jolie2surface.jar joliex.surface.GetSurface -l %JOLIE_HOME%\lib;%JOLIE_HOME%\javaServices\*;%JOLIE_HOME%\extensions\* -i %JOLIE_HOME%\include -p %JOLIE_HOME%\packages %*
		
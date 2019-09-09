@echo off
java -ea:jolie... -ea:joliex... -jar %JOLIE_HOME%\jolie.jar -l .\lib\*;%JOLIE_HOME%\lib;%JOLIE_HOME%\javaServices\*;%JOLIE_HOME%\extensions\* -i %JOLIE_HOME%\include %*
		
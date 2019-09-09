@echo off
java -ea:jolie... -ea:joliex... -cp %JOLIE_HOME%\jolie.jar;%JOLIE_HOME%\lib\libjolie.jar;%JOLIE_HOME%\tools\jolie2java.jar;%JOLIE_HOME%\lib\jolie-gwt.jar joliex.java.Jolie2Java -l %JOLIE_HOME%\lib;%JOLIE_HOME%\javaServices\*;%JOLIE_HOME%\extensions\* -i %JOLIE_HOME%\include %*
		
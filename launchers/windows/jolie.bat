@echo off
java -ea:jolie... -ea:joliex... -cp %JOLIE_HOME%\lib\libjolie.jar;%JOLIE_HOME%\lib\automaton.jar;%JOLIE_HOME%\jolie.jar jolie.Jolie -l .\lib\*;%JOLIE_HOME%\lib;%JOLIE_HOME%\javaServices\*;%JOLIE_HOME%\extensions\* -i %JOLIE_HOME%\include %*

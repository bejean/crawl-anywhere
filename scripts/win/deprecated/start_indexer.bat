set INDEXER=E:\Projets\Huridocs\prod
@cls
@echo off
@mkdir %INDEXER%\log
@cd %INDEXER%\log
@call %INDEXER%\scripts\getclasspath.bat
@echo Starting Indexer
set JVMARGS=-Duser.timezone=Europe/Paris -Xms64m -Xmx256m -Dcom.sun.management.jmxremote
@java %JVMARGS% fr.eolya.indexer.Indexer >> %INDEXER%/log/indexer.output 2>&1
@cd %INDEXER%\scripts

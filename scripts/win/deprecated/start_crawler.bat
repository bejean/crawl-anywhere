set CRAWLER=E:\Projets\Huridocs\prod
@cls
@echo off
@mkdir %CRAWLER%\log
@cd %CRAWLER%\log
@call %CRAWLER%\scripts\getclasspath.bat
@echo Starting Crawler
set JVMARGS=-Duser.timezone=Europe/Paris -Xms64m -Xmx256m -Dcom.sun.management.jmxremote
@java %JVMARGS% fr.eolya.crawler.crawler.Crawler >> %CRAWLER%/log/crawler.output 2>&1
@cd %CRAWLER%\scripts

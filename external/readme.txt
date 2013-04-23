Externals tools are organized by OS.
According to your OS copy all tools directly in this directory.
Check your configuration files in order to specify the correct path to these tools.
On Linux and MacOS, check the +x flag of these tools.

for instance, if your are on a 64bits Linux server

cd /opt/crawler/external
cp -r linux64/* .
chmod +x catdoc catppt swf2html xls2csv

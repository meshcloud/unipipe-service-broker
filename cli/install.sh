#!/bin/bash
set -o errexit
set -o errtrace
set -o pipefail
set -o nounset
# Check if user has all required commands installed
REQ_COMMANDS=(curl)
for i in $REQ_COMMANDS
do
  command -v "$i" >/dev/null && continue || { echo "$i command not found. You need to install if before running this script"; exit 1; }
done
# Download binary
case "$(uname -s)" in
   Darwin)
     url=$(curl -s https://api.github.com/repos/meshcloud/unipipe-service-broker/releases/latest | grep "browser_download_url.*apple" | cut -d : -f 2,3 | tr -d \" | tr -d \ )
     ;;
   Linux)
     url=$(curl -s https://api.github.com/repos/meshcloud/unipipe-service-broker/releases/latest | grep "browser_download_url.*linux" | cut -d : -f 2,3 | tr -d \" | tr -d \ )
     ;;
   CYGWIN*|MINGW32*|MSYS*|MINGW*)
     echo 'Please execute the install.ps1 script as mentioned in our README.'
     exit 1
     ;;
   *)
     echo 'Not compatible.'
     exit 1
     ;;
esac
echo "Downloading the binary... (${url})"
curl "${url}" -L -o unipipe-cli --silent 
# Copy binary and set permissions
echo "Copying the binary..."
mv unipipe-cli /usr/local/bin/unipipe
if [ "$(uname -s)" == "Linux" ]; then
  chown 0:0 /usr/local/bin/unipipe;
  chmod 755 /usr/local/bin/unipipe;
fi;
chmod +x /usr/local/bin/unipipe
# Mention command line completions
echo "
We recommend to enable shell completions for unipipe cli. 
To enable shell completions add the following line to your ~/.bashrc or similar: 
	source <(unipipe completions [shell]>
"
echo "Finished installing. Please restart your terminal and run \"unipipe\" to get started!"
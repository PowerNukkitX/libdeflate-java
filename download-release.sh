mkdir -p "target"
cd "target" || exit 1

# check whether FrostRender.jar exists
if [ ! -f "libdeflate-java-0.0.2-PNX" ]; then
    echo "libdeflate-java-0.0.2-PNX not found, downloading from GitHub..."

    download_url=$(curl -Ls "https://api.github.com/repos/PowerNukkitX/libdeflate-java/releases/latest" \
      | grep -Eo '"browser_download_url": "https:.+?"' \
      | head -n 1 \
      | grep -Eo 'https:.+?\.jar')

    rm -f "libdeflate-java-0.0.2-PNX"
    echo "Downloading libdeflate-java-0.0.2-PNX from $download_url..."

    if [ -z "$HTTP_PROXY" ]; then
        echo "No proxy detected. You can set proxy via HTTP_PROXY environment variable."
        curl -Ls -o "libdeflate-java-0.0.2-PNX" "$download_url"
    else
        echo "Using proxy $HTTP_PROXY"
        curl -Ls -x "$HTTP_PROXY" -o "libdeflate-java-0.0.2-PNX" "$download_url"
    fi
fi

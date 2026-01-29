<h1 align="center">Grok Viewer</h1>

<div align="center" style="display: grid; justify-content: center;">

|                                                                  🌟                                                                   |                  Support this project                   |               
|:-------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------:|
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/bitcoin.png" alt="Bitcoin (BTC)" width="32"/>  | <code>bc1qs6qq0fkqqhp4whwq8u8zc5egprakvqxewr5pmx</code> | 
| <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/ethereum.png" alt="Ethereum (ETH)" width="32"/> | <code>0x3147bEE3179Df0f6a0852044BFe3C59086072e12</code> |
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/tether.png" alt="USDT (TRC-20)" width="32"/>   |     <code>TKznmR65yhPt5qmYCML4tNSWFeeUkgYSEV</code>     |

</div>

<p align="center">An application for viewing, filtering, and exporting binary data from Grok AI (xAI) archives into human-readable
formats</p>

<p align="center"><img src="./media/preview.png" alt="preview"></p>

<br>

> [!NOTE]
> The application was designed using the [Reduce & Conquer](https://github.com/numq/reduce-and-conquer) architectural
> pattern

## Installation

GrokViewer is distributed as a portable application. No installation is required.

1. Navigate to the [Releases](https://github.com/numq/grok-viewer/releases) page.

2. Download the archive for your operating system:

    - Windows: `grok-viewer-win-x64.zip`

    - macOS: `grok-viewer-mac-x64.zip`

    - Linux: `grok-viewer-linux-x64.tar.gz`

3. Extract the archive and run the executable file found inside the `app` folder.

## Build

To build the distributable binaries locally, ensure you have JDK 17+ and run:

```bash
./gradlew :desktop:createDistributable
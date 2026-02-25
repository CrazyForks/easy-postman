<div align="center">

<img src="docs/icon.png" alt="EasyPostman Logo" width="100" />

# EasyPostman

**An open-source API debugging & performance testing desktop app**  
*Inspired by Postman · Powered by Java · Built for developers*

[![GitHub license](https://img.shields.io/github/license/lakernote/easy-postman?style=flat-square)](https://github.com/lakernote/easy-postman/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/lakernote/easy-postman?style=flat-square&color=brightgreen)](https://github.com/lakernote/easy-postman/releases)
[![GitHub stars](https://img.shields.io/github/stars/lakernote/easy-postman?style=flat-square&color=yellow)](https://github.com/lakernote/easy-postman/stargazers)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-0078D4?style=flat-square&logo=windows&logoColor=white)](https://github.com/lakernote/easy-postman/releases)

[简体中文](README_zh.md) · [English](README.md) · [📦 Download](https://github.com/lakernote/easy-postman/releases) · [📖 Docs](docs/FEATURES.md) · [💬 Discuss](https://github.com/lakernote/easy-postman/discussions)

</div>

---

## 📖 Table of Contents

- [💡 About](#-about)
- [✨ Features](#-features)
- [📦 Download](#-download)
- [🚀 Quick Start](#-quick-start)
- [🖼️ Screenshots](#️-screenshots)
- [🤝 Contributing](#-contributing)
- [📚 Documentation](#-documentation)
- [❓ FAQ](#-faq)
- [💖 Support](#-support)

---

## 💡 About

EasyPostman provides developers with a **local, privacy-first** API debugging experience comparable to Postman, plus simplified JMeter-style performance testing. Built with Java Swing for cross-platform support, it works completely offline and includes built-in Git workspace support for team collaboration and version control.

| 🎯 Focus on Core | 🔒 Privacy First | 🚀 Performance |
|:---:|:---:|:---:|
| Simple yet powerful, rich features without bloat | 100% local storage, no cloud sync, your data stays private | Native Java app, fast startup, smooth experience |

---

## ✨ Features

### 🏢 Workspace & Collaboration
- **Local Workspace** - Personal projects with local storage
- **Git Workspace** - Version control and team collaboration
- **Multi-device Sync** - Share API data via Git repositories
- **Project Isolation** - Each workspace manages its own collections and environments

### 🔌 API Testing
- **HTTP/HTTPS** - Full REST API support (GET, POST, PUT, DELETE, etc.)
- **WebSocket & SSE** - Real-time protocol support
- **Multiple Body Types** - Form Data, JSON, XML, Binary
- **File Upload/Download** - Drag & drop support
- **Environment Variables** - Multi-environment management with dynamic variables

### ⚡ Performance Testing
- **Thread Group Modes** - Fixed, Ramp-up, Stair-step, Spike
- **Real-time Monitoring** - TPS, response time, error rate
- **Visual Reports** - Performance trend charts and result trees
- **Batch Requests** - Simplified JMeter-style testing

### 📝 Advanced Features
- **Pre-request Scripts** - JavaScript execution before requests
- **Test Scripts** - Assertions and response validation
- **Request Chaining** - Extract data and pass to next request
- **Network Event Log** - Detailed request/response analysis
- **Import/Export** - Postman v2.1, cURL, HAR (in progress)

### 🎨 User Experience
- **Light & Dark Mode** - Comfortable viewing in any lighting
- **Multi-language** - English, 简体中文
- **Syntax Highlighting** - JSON, XML, JavaScript
- **Cross-platform** - Windows, macOS, Linux

📖 **[View All Features →](docs/FEATURES.md)**

---

## 📦 Download

### Latest Release

🔗 **[GitHub Releases](https://github.com/lakernote/easy-postman/releases)** | **[Gitee Mirror (China)](https://gitee.com/lakernote/easy-postman/releases)**

### Platform Downloads

| Platform | Package | Notes |
|----------|---------|-------|
| 🍎 **macOS (Apple Silicon)** | `EasyPostman-{version}-macos-arm64.dmg` | M1/M2/M3/M4 |
| 🍏 **macOS (Intel)** | `EasyPostman-{version}-macos-x86_64.dmg` | Intel-based Mac |
| 🪟 **Windows (Installer)** | `EasyPostman-{version}-windows-x64.exe` | Auto-update support |
| 🪟 **Windows (Portable)** | `EasyPostman-{version}-windows-x64-portable.zip` | No install needed |
| 🐧 **Ubuntu / Debian** | `easypostman_{version}_amd64.deb` | DEB package |
| ☕ **Cross-platform JAR** | `easy-postman-{version}.jar` | Requires Java 17+ |

> ⚠️ **First Run Notice**
>
> - **Windows**: SmartScreen warning → "More info" → "Run anyway"
> - **macOS**: "Cannot be opened" → Right-click → "Open" → "Open"
>
> The app is 100% open-source. Warnings appear because we don't purchase code signing certificates.

> 🌏 **Gitee Mirror** only provides macOS (ARM) DMG and Windows packages. For other platforms, use GitHub Releases.

---

## 🚀 Quick Start

### Option 1: Download Pre-built Release

1. Grab the package for your platform from [Releases](https://github.com/lakernote/easy-postman/releases)
2. Install and run:

| Platform | Command / Action |
|----------|-----------------|
| macOS | Open DMG → drag to Applications |
| Windows Installer | Run `.exe`, follow wizard |
| Windows Portable | Extract ZIP → run `EasyPostman.exe` |
| Linux DEB | `sudo dpkg -i easypostman_{version}_amd64.deb` |
| JAR | `java -jar easy-postman-{version}.jar` |

### Option 2: Build from Source

```bash
git clone https://github.com/lakernote/easy-postman.git
cd easy-postman
mvn clean package
java -jar target/easy-postman-*.jar
```

📖 **[Build Guide →](docs/BUILD.md)**

### First Steps

1. **Create a Workspace** — Local (personal) or Git (team)
2. **Create a Collection** — Organize your API requests
3. **Send Your First Request** — Enter URL, configure params, click Send
4. **Set Up Environments** — Switch between dev / test / prod easily

---

## 🖼️ Screenshots

<div align="center">

| Home | Workspaces |
|:----:|:----------:|
| ![Home](docs/home-en.png) | ![Workspaces](docs/workspaces.png) |

| Collections & API Testing | Performance Testing |
|:-------------------------:|:-------------------:|
| ![Collections](docs/collections.png) | ![Performance](docs/performance.png) |

</div>

📸 **[View All Screenshots →](docs/SCREENSHOTS.md)**

---

## 🤝 Contributing

We welcome all forms of contribution — bug reports, feature requests, code, or docs!

| Type | How |
|------|-----|
| 🐛 Bug Report | [Open an issue](https://github.com/lakernote/easy-postman/issues/new/choose) |
| ✨ Feature Request | [Share your idea](https://github.com/lakernote/easy-postman/issues/new/choose) |
| 💻 Code | Fork → branch → PR |
| 📝 Docs | Fix typos, add examples, translate |

Every PR triggers automated checks: build, tests, code quality, and format validation.

📖 **[Contributing Guide →](.github/CONTRIBUTING.md)**

---

## 📚 Documentation

| Doc | Description |
|-----|-------------|
| 📖 [Features](docs/FEATURES.md) | Comprehensive feature documentation |
| 🏗️ [Architecture](docs/ARCHITECTURE.md) | Technical stack and design |
| 🚀 [Build Guide](docs/BUILD.md) | Build from source & generate installers |
| 🖼️ [Screenshots](docs/SCREENSHOTS.md) | All application screenshots |
| 📝 [Script API Reference](docs/SCRIPT_API_REFERENCE_zh.md) | Pre-request & test script API |
| 📝 [Script Snippets](docs/SCRIPT_SNIPPETS_QUICK_REFERENCE.md) | Built-in code snippet reference |
| 🔐 [Client Certificates](docs/CLIENT_CERTIFICATES.md) | mTLS configuration |
| 🐧 [Linux Build](docs/LINUX_BUILD.md) | Building on Linux |
| ❓ [FAQ](docs/FQA.MD) | Frequently asked questions |

---

## ❓ FAQ

<details>
<summary><b>Q: Why local storage instead of cloud sync?</b></summary>

We value developer privacy. Local storage ensures your API data is never leaked to third parties. Use Git workspace for team collaboration while maintaining full control over your data.
</details>

<details>
<summary><b>Q: How to import Postman data?</b></summary>

In the Collections view, click **Import** and select a Postman v2.1 JSON file. Collections, requests, and environments are converted automatically.
</details>

<details>
<summary><b>Q: Why does Windows/macOS show security warnings?</b></summary>

- **Windows SmartScreen**: No code signing cert (~$100–400/year). → Click "More info" → "Run anyway". Warnings decrease as download count grows.
- **macOS Gatekeeper**: No Apple Developer cert ($99/year). → Right-click → "Open", or run: `sudo xattr -rd com.apple.quarantine /Applications/EasyPostman.app`

This project is **fully open-source** and auditable on GitHub.
</details>

<details>
<summary><b>Q: Does it support team collaboration?</b></summary>

✅ Yes — use **Git workspace** to share collections & environments, track changes (commit/push/pull), and collaborate across devices without any cloud service.
</details>

<details>
<summary><b>Q: Are workspaces isolated?</b></summary>

Yes. Each workspace has its own collections, environments, and history. Switching workspaces provides complete data isolation.
</details>

<details>
<summary><b>Q: Which Git platforms are supported?</b></summary>

All standard Git platforms: GitHub, Gitee, GitLab, Bitbucket, and self-hosted Git servers (HTTPS or SSH).
</details>

---

## 💖 Support the Project

If EasyPostman helps you, consider:

- ⭐ **Star this repo** — it means a lot!
- 🍴 **Fork & contribute** — help make it better
- 📢 **Share with friends** — spread the word
- 💬 **WeChat group** — add **lakernote** for direct communication
- 💬 **GitHub Discussions** — [ask questions & share ideas](https://github.com/lakernote/easy-postman/discussions)

---

## ⭐ Star History

<div align="center">

[![Star History Chart](https://api.star-history.com/svg?repos=lakernote/easy-postman&type=date&legend=top-left)](https://www.star-history.com/#lakernote/easy-postman&type=date&legend=top-left)

</div>

---

## 🙏 Acknowledgements

Thanks to these awesome open-source projects:

| Project | Role |
|---------|------|
| [FlatLaf](https://github.com/JFormDesigner/FlatLaf) | Modern Swing theme |
| [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) | Syntax highlighting editor |
| [OkHttp](https://github.com/square/okhttp) | HTTP client |
| [Termora](https://github.com/TermoraDev/termora) | Terminal emulator inspiration |

---

<div align="center">

**Make API debugging easier. Make performance testing more intuitive.**

[![GitHub](https://img.shields.io/badge/GitHub-lakernote-181717?style=flat-square&logo=github)](https://github.com/lakernote)
&nbsp;
[![Gitee](https://img.shields.io/badge/Gitee-lakernote-C71D23?style=flat-square&logo=gitee)](https://gitee.com/lakernote)

Made with ❤️ by [laker](https://github.com/lakernote)

</div>

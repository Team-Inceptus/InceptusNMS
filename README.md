# InceptusNMS
> JavaDocs for Mojang-Mapped Minecraft NMS and CraftBukkit for the latest Minecraft Version

InceptusNMS is a repository of documented Net-Minecraft-Server (NMS) and CraftBukkit Code for Minecraft: Java Edution. It utilizes a custom parser made in Kotlin from an input of JSON files to generate a JavaDocs-style browser.

## 📕 Background
<details>
    <summary>Click to Expand</summary>

The goal of this project is to attempt to emulate a JDK 17 JavaDocs-style Browser for the Mojang-Mapped NMS Mappings and CraftBukkit Server, in order to make NMS and CraftBukkit Development easier and more accessible.
</details>

### ❓How Does it Work?

<details>
    <summary>Click to Expand</summary>

We use a custom parser to parse JSON files stored in our `docs` folder to emulate a JavaDocs browser. Developed in Kotlin and Java, the parser is able to parse the JSON files and generate a JavaDocs-style browser for the NMS and CraftBukkit Server. Things like schemas, examples, and contributing guidelines are available in [CONTRIBUTING.md](CONTRIBUTING.md).
</details>

## 💻 Using with an IDE

<details>
    <summary>IntelliJ</summary>

1. File -> Project Structure -> Libraries
Locate your NMS Dependency

![Step 1](https://proxy.spigotmc.org/58c4035953801bd2cf399bd0af587b66a6a42fc5?url=https%3A%2F%2Fmedia.discordapp.net%2Fattachments%2F894254760075603980%2F1161000215348785213%2Fimage.png%3Fex%3D6536b4b5%26is%3D65243fb5%26hm%3Da7b65f14e3cc13d53ad7301a3ac68a83ae3a7432fdb5da27bb4ae2c5fc31b601%26%3D%26width%3D854%26height%3D702)

2. Click on "Specify Documentation URL" (Plus with the Earth Icon)

![Step 2](https://proxy.spigotmc.org/f426b0f664207033e95f5ff87c3ecf1c0361b314?url=https%3A%2F%2Fmedia.discordapp.net%2Fattachments%2F894254760075603980%2F1161000429023408269%2Fimage.png%3Fex%3D6536b4e8%26is%3D65243fe8%26hm%3D9a58f136e94a3b63445f7b137a9336e42a36f264ad569332b75bab334bb6cde2%26%3D)

3. Enter the Browser URL

![Step 3](https://proxy.spigotmc.org/d332d79120cc6eb95f7808eb5111faa99bd10335?url=https%3A%2F%2Fmedia.discordapp.net%2Fattachments%2F894254760075603980%2F1161000611572097114%2Fimage.png%3Fex%3D6536b514%26is%3D65244014%26hm%3Da8d54f9a71fd82cc8c1eb4a92efc2cfb7531f686bbc9e2d0035e3289afbdf188%26%3D)

4. Done!

![Step 4](https://proxy.spigotmc.org/7f560e2d345a50a5721981a5d705a232180c0f2d?url=https%3A%2F%2Fmedia.discordapp.net%2Fattachments%2F894254760075603980%2F1161001055740510278%2Fimage.png%3Fex%3D6536b57e%26is%3D6524407e%26hm%3D6ccb32cb51584e3c517408198fbc7a1eb5413c6d887174787fa53fa86c7660a0%26%3D)
</details>

<details>
    <summary>Eclipse</summary>

Credit to [@SkytAsul](https://www.spigotmc.org/members/373955/)

1. Right click on project -> Properties -> Java Build Path -> Libraries   
Locate your NMS Dependency

![Step 1](https://media.discordapp.net/attachments/894254760075603980/1172009961048518816/upload_2023-10-9_20-54-45.png?ex=655ec255&is=654c4d55&hm=f485cff5179620213ee174a2a2fc6cb3a43540b2cd3334da7accbffa406115ff)

2. Expand Dependency, select "JavaDoc Location" and click on "Edit..."

![Step 2](https://media.discordapp.net/attachments/894254760075603980/1172009961333719081/upload_2023-10-9_20-57-32.png?ex=655ec255&is=654c4d55&hm=fdceb0033f1091d8acc509bd8dbd83b8bce471801db3061666beaad60ac3ec41)

3. Enter the Browser URL

![Step 3](https://media.discordapp.net/attachments/894254760075603980/1172009961568608328/upload_2023-10-9_20-58-33.png?ex=655ec255&is=654c4d55&hm=a69e92b65f9731d8b55a9e5a6a87f69a3298d6e1d7d4d4beac805babc5b7f68e)

4. Done!
</details>

## 📝 Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more information.
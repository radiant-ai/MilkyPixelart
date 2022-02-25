# MilkyPixelart
![CodeQL](https://github.com/radiant-ai/MilkyPixelart/actions/workflows/codeql.yml/badge.svg)
[![CodeFactor](https://www.codefactor.io/repository/github/radiant-ai/milkypixelart/badge)](https://www.codefactor.io/repository/github/radiant-ai/milkypixelart)
[![Maintainability](https://api.codeclimate.com/v1/badges/4dfb4edd6f9a4ace6b9e/maintainability)](https://codeclimate.com/github/radiant-ai/MilkyPixelart/maintainability)

Protect your unique banners and art maps from illegal copying:

![Demo](https://i.imgur.com/c5OmyVa.gif)

Preview art maps directly from the virtual markets:

![Demo](https://i.imgur.com/oZbuB3y.gif)

Bypass the limit of 6 patterns on banners using GUI-based banner editor. You can also erase the top pattern on the go:

![Demo](https://i.imgur.com/RyRVolG.gif)

Instantly search duplicated art maps to enforce copyright rules on your game server:

![Demo](https://i.imgur.com/aXpxVgf.gif)

Add the desired art maps to blacklist so they instantly get deleted on your server:

![Demo](https://i.imgur.com/MGqk2mM.png)

## Dependencies
- PaperMC 1.18.1+
- ProtocolLib
- Vault + any Economy plugin
## Configuration
```
common:
  # the first lore line shown on a protected item
  copyrightText: "<#FFFF99>Protected from duplication"
  
  # the second lore line shown can start or finish with author's name (<arg1>)
  copyrightAuthorName: "<#FFFF99>© <#9AFF0F><arg1>"
  
  # any lines we should remove from the lore when item is duplicated
  copyrightLegacyStrings:
    - "Copyrighted by"
banners:
  # banner protection price
  copyrightPrice: 20
pixelarts:
  # art map protection price
  copyrightPrice: 100
  # these lines should be contained by GUI's title to enable art map preview
  previewInventories:
    - "Auction"
    - "Market"
    - "Shop"
```

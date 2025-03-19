# MilkyPixelart
[![CodeFactor](https://www.codefactor.io/repository/github/radiant-ai/milkypixelart/badge)](https://www.codefactor.io/repository/github/radiant-ai/milkypixelart)
[![Maintainability](https://api.codeclimate.com/v1/badges/4dfb4edd6f9a4ace6b9e/maintainability)](https://codeclimate.com/github/radiant-ai/MilkyPixelart/maintainability)
![Build](https://github.com/radiant-ai/MilkyPixelart/actions/workflows/build.yml/badge.svg)

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
- Latest Paper
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
commands:
  # add custom aliases to the default /pixelart command
  aliases:
    - art
    - pxt
banners:
  # banner protection price
  copyrightPrice: 20
pixelarts:
  # cooldown for show and showall
  showArtCooldown: 300
  # message for show and showall <arg1> - nickname and <arg2> - art list
  showMessage: "<#9AFF0F><arg1> <#FFFF99>shows off their arts: <arg2>"
  # art map protection price
  copyrightPrice: 100
  # ignore inventory title names and preview arts everywhere
  previewEverywhere: false
  # these lines should be contained by GUI's title to enable art map preview
  previewInventories:
    - "Auction"
    - "Market"
    - "Shop"
  # disallow map creation if player has no access to block under their feet
  respectRegionProtection: false
```
## Commands

`/pixelart protect` - copy protect the art/banner

`/pixelart show` - show the art in the main hand

`/pixelart showall` - show all arts in the toolbar

`/pixelart findduplicated <map id>` - find other maps with the same picture as the given map id

`/pixelart reload` - mostly safe reload of the entire plugin

`/pixelart blacklist add <map id> <uuid>` - add the map to the blacklist with the specified legit owner

`/pixelart blacklist remove <map id>` - remove the map from the blacklist

`/pixelart blacklist list [page]` - show the blacklist

## Permissions

For commands:

`pixelart.protect`

`pixelart.show`

`pixelart.showall`

`pixelart.preview` - required to preview maps in show/showall

`pixelart.findduplicated`

`pixelart.blacklist`

`pixelart.reload`

Other:

`pixelart.bypassloom` - bypass loom requirements

`pixelart.show.cooldownbypass` - bypass show/showall cooldown

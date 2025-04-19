# AutoFarmRegions

A Minecraft plugin that adds automatic farming functionality to WorldGuard regions.

## Features

- Automatic harvesting and replanting of crops in specified WorldGuard regions
- Faster crop growth within enabled regions
- Support for multiple regions
- Fully customizable messages using MiniMessage formatting
- Configurable notification system (action bar and title messages)
- Immature crops cannot be broken
- Automatic inventory management
- Supported Crops: Wheat, Carrots, Potatoes, Beetroots, Nether Wart

## Requirements

- WorldGuard
- Java 17 or higher

## Installation

1. Download the latest version of the plugin
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the `config.yml` file

## Configuration

The plugin's configuration file (`config.yml`) allows you to:

- Enable/disable regions for automatic farming
- Adjust crop growth speed
- Configure notification settings
- Customize all messages using MiniMessage formatting

### Example Configuration

```yaml
enabled-regions:
  - farm1
  - farm2

settings:
  crop-growth-speed: 2
  action-bar-enabled: true
  title-enabled: false
```

## Commands

- `/autofarm reload` - Reload the configuration
- `/autofarm list` - List enabled regions
- `/autofarm add <region>` - Add a region
- `/autofarm remove <region>` - Remove a region

## Permissions

- `autofarmregions.admin` - Access to all plugin commands

## License

This project is licensed under the MIT License - see the LICENSE file for details. 

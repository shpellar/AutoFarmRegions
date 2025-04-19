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

- Minecraft 1.21.4
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

<details>
<summary>Click to expand configuration example</summary>

```yaml
# AutoFarmRegions Configuration

# List of enabled WorldGuard regions where automatic farming will work
enabled-regions:
  - farm1
  - farm2

# Auto-replant settings
auto-replant:
  # Whether to automatically replant crops when harvested
  enabled: true
  # Whether to add drops directly to inventory
  add-to-inventory: true
  # Whether to drop items on the ground if inventory is full
  drop-if-full: false
  # Inventory full notification settings
  inventory-full-notification:
    # Show a title when inventory is full
    show-title: false
    # Show action bar when inventory is full
    show-action-bar: true
    # Duration of title in ticks (20 ticks = 1 second)
    title-duration: 40

# Crop growth settings
growth:
  # Whether to enable growth in regions
  enabled: true
  # How often to start a new growth chain (in seconds)
  interval: 60
  # How long to wait between each crop in the chain (in ticks, 20 ticks = 1 second)
  chain-delay: 2
  # Whether to show particles when crops grow
  show-particles: true
  # Which particle to show when crops grow
  particle: VILLAGER_HAPPY

# Crop protection settings
protection:
  # Whether to prevent breaking immature crops
  prevent-immature-break: true
  # Whether to show a message when breaking immature crops
  show-immature-message: false
  # Whether to allow breaking crops with specific tools
  require-tools: false
  # List of tools that can break crops (if require-tools is true)
  allowed-tools:
  - WOODEN_HOE
  - STONE_HOE
  - IRON_HOE
  - GOLDEN_HOE
  - DIAMOND_HOE
  - NETHERITE_HOE
```
</details>

## Commands

- `/autofarm reload` - Reload the configuration
- `/autofarm list` - List enabled regions
- `/autofarm add <region>` - Add a region
- `/autofarm remove <region>` - Remove a region

## Permissions

- `autofarmregions.admin` - Access to all plugin commands

## Building from Source

1. Clone the repository
2. Run `mvn clean package`
3. The compiled JAR will be in the `target` directory

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
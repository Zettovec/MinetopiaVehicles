package nl.mtvehicles.core.infrastructure.dataconfig;

import nl.mtvehicles.core.infrastructure.enums.*;
import nl.mtvehicles.core.infrastructure.helpers.TextUtils;
import nl.mtvehicles.core.infrastructure.models.Config;
import nl.mtvehicles.core.infrastructure.modules.ConfigModule;
import nl.mtvehicles.core.infrastructure.modules.DependencyModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultConfig extends Config {
    public DefaultConfig() {
        super(ConfigType.DEFAULT);
    }

    @Deprecated
    public String getMessage(String key) {
        return TextUtils.colorize(this.getConfiguration().getString(key));
    }

    /**
     * Get a value of an option from config.yml
     *
     * @param configOption Config.yml option
     * @return Value of the option (as Object)
     */
    public Object get(Option configOption){
        return this.getConfiguration().get(configOption.getPath());
    }

    public boolean hasOldVersionChecking(){
        return this.getConfiguration().get("Config-Versie") != null;
    }

    //--- DriveUp ---
    public DriveUp driveUpSlabs(){
        DriveUp returns = DriveUp.BOTH; //default value
        try {
            switch (Objects.requireNonNull(get(Option.DRIVE_UP).toString().toLowerCase())){
                case "blocks": case "block":
                    returns = DriveUp.BLOCKS; break;
                case "slabs": case "slab":
                    returns = DriveUp.SLABS; break;
            }
        } catch (NullPointerException e){
            //driveUp was not set up correctly in config and thus it's using the default = both.
        }
        return returns;
    }

    //--- Gas stations ---

    GasStationConfig gasStations = new GasStationConfig();

    public boolean canUseJerryCan(Location loc){
        if (!gasStations.areGasStationsEnabled()) return true;

        //If a player is in region with 'mtvgasstation=deny', they can't use jerrycans.
        if (DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-gasstation", false)) return false;

        if (gasStations.canUseJerryCanOutsideOfGasStation()) return true;

        return gasStations.isInsideGasStation(loc);
    }

    public boolean canUseJerryCan(Player p){
        return canUseJerryCan(p.getLocation());
    }

    public boolean canFillJerryCans(Player p, Location loc){
        if (!gasStations.areGasStationsEnabled()) return false;
        if (!gasStations.isFillJerryCansEnabled()) return false;
        if (!gasStations.isInsideGasStation(loc)) return false;

        if (!gasStations.hasFillJerryCansPermission(p)){
            p.sendMessage(TextUtils.colorize(ConfigModule.messagesConfig.getMessage(Message.NO_PERMISSION)));
            return false;
        } return true;
    }

    public boolean jerryCanPlaySound(){
        return (boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_PLAY_SOUND);
    }

    private class GasStationConfig {

        private boolean areGasStationsEnabled(){
            if (!DependencyModule.isDependencyEnabled(SoftDependency.WORLD_GUARD)) return false; //If WorldGuard isn't installed, say it's not enabled.

            return (boolean) get(Option.GAS_STATIONS_ENABLED);
        }

        private boolean canUseJerryCanOutsideOfGasStation(){
            return (boolean) get(Option.GAS_STATIONS_CAN_USE_JERRYCAN_OUTSIDE_OF_GAS_STATION);
        }

        private boolean isInsideGasStation(Location loc){
            return DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-gasstation", true);
        }

        private boolean isFillJerryCansEnabled(){
            return (boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_ENABLED);
        }

        private boolean hasFillJerryCansPermission(Player p){
            if (!(boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_NEED_PERMISSION)) return true; //if there's set that they don't need the permission, just pretend they have it
            return p.hasPermission("mtvehicles.filljerrycans");
        }

    }

    public boolean isFillJerryCansLeverEnabled(){
        return (boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_LEVER);
    }

    public boolean isFillJerryCansTripwireHookEnabled(){
        return (boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_TRIPWIRE_HOOK);
    }

    public boolean isFillJerryCanPriceEnabled(){
        if (!gasStations.areGasStationsEnabled()) return false;
        if (!gasStations.isFillJerryCansEnabled()) return false;
        if (!DependencyModule.isDependencyEnabled(SoftDependency.VAULT)) return false; //If Vault isn't installed, say it's not enabled.
        if (!DependencyModule.vault.isEconomySetUp()) return false; //There is no Vault Economy plugin, disable it.

        return (boolean) get(Option.GAS_STATIONS_FILL_JERRYCANS_PRICE_ENABLED);
    }

    public double getFillJerryCanPrice(){
        if ((double) get(Option.GAS_STATIONS_FILL_JERRYCANS_PRICE_PER_LITRE) <= 0) return 30.0; //Default, if it's not greater than 0
        else return (double) get(Option.GAS_STATIONS_FILL_JERRYCANS_PRICE_PER_LITRE);
    }

    //--- Disabled Worlds ---
    public boolean isWorldDisabled(String worldName){
        if (getDisabledWorlds().isEmpty()) return false;
        return getDisabledWorlds().contains(worldName);
    }

    private List<String> getDisabledWorlds(){
        return getConfiguration().getStringList("disabledWorlds");
    }

    //--- Block Whitelist ---
    public boolean isBlockWhitelistEnabled() {
        return (boolean) get(Option.BLOCK_WHITELIST_ENABLED);
    }

    public List<Material> blockWhiteList() {
        return getConfiguration().getStringList("blockWhitelist.list").stream().map(Material::getMaterial).collect(Collectors.toList());
    }

    //--- Region Actions ---

    private RegionAction.ListType getRegionActionListType(RegionAction action){
        String configOption = "disabled"; //Default
        switch (action){
            case PLACE:
                configOption = get(Option.REGION_ACTIONS_PLACE).toString().toLowerCase(Locale.ROOT);
                break;
            case PICKUP:
                configOption = get(Option.REGION_ACTIONS_PICKUP).toString().toLowerCase(Locale.ROOT);
                break;
            case ENTER:
                configOption = get(Option.REGION_ACTIONS_ENTER).toString().toLowerCase(Locale.ROOT);
                break;
        }
        if (configOption.equalsIgnoreCase("whitelist")) return RegionAction.ListType.WHITELIST;
        else if (configOption.equalsIgnoreCase("blacklist")) return RegionAction.ListType.BLACKLIST;
        else return RegionAction.ListType.DISABLED;
    }

    public boolean canProceedWithAction(RegionAction action, Location loc){
        if (isWorldDisabled(loc.getWorld().getName())) return false;

        if (!DependencyModule.isDependencyEnabled(SoftDependency.WORLD_GUARD)) return true;

        boolean returns = true;
        RegionAction.ListType listType = getRegionActionListType(action);
        if (!listType.isEnabled()) return true;

        boolean isWhitelist = listType.isWhitelist();
        boolean isBlacklist = listType.isBlacklist();
        switch (action){
            case PLACE:
                if (isWhitelist)
                    returns = DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-place", true);
                else if (isBlacklist)
                    returns = !DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-place", false);
                break;
            case PICKUP:
                if (isWhitelist)
                    returns = DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-pickup", true);
                else if (isBlacklist)
                    returns = !DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-pickup", false);
                break;
            case ENTER:
                if (isWhitelist)
                    returns = DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-enter", true);
                else if (isBlacklist)
                    returns = !DependencyModule.worldGuard.isInRegionWithFlag(loc, "mtv-enter", false);
                break;
        }
        return returns;
    }

    /**
     * Options available in MTV's default configuration file (config.yml)
     */
    public enum Option {
        AUTO_UPDATE("auto-update", true),
        VEHICLE_MENU_SIZE("vehicleMenuSize", 3),
        /**
         * Can be found as 'wiekens-always-on' in config.yml
         */
        HELICOPTER_BLADES_ALWAYS_ON("wiekens-always-on", true),
        /**
         * Can be found as 'anwb' in config.yml
         */
        PICKUP_FROM_WATER("anwb", false),
        /**
         * Can be found as 'kofferbakEnabled' in config.yml
         */
        TRUNK_ENABLED("kofferbakEnabled", true),
        /**
         * Can be found as 'spelerSetOwner' in config.yml
         */
        PUT_ONESELF_AS_OWNER("spelerSetOwner", false),
        MAX_FLYING_HEIGHT("maxFlyingHeight", 150),
        TAKE_OFF_SPEED("takeOffSpeed", 0.4),
        CAR_PICKUP("carPickup", false),
        /**
         * Can be found as 'benzine' in config.yml
         */
        FUEL_ENABLED("benzine", true),
        FUEL_MULTIPLIER("fuelMultiplier", 1),
        JERRYCANS("jerrycans", new ArrayList<>(Arrays.asList(25, 50, 75))),
        DAMAGE_ENABLED("damageEnabled", false),
        DAMAGE_MULTIPLIER("damageMultiplier", 0.5),
        HORN_COOLDOWN("hornCooldown", 5),
        HORN_TYPE("hornType", "minetopiaclassic.horn1"),
        TANK_TNT("tankTNT", false),
        TANK_COOLDOWN("tankCooldown", 10),
        DRIVE_UP("driveUp", "both"),
        DRIVE_ON_CARPETS("driveOnCarpets", true),
        BLOCK_WHITELIST_ENABLED("blockWhitelist.enabled", false),
        BLOCK_WHITELIST_LIST("blockWhitelist.list", new ArrayList<>().add("GRAY_CONCRETE")),
        DISABLED_WORLDS("disabledWorlds", new ArrayList<>()),
        GAS_STATIONS_ENABLED("gasStations.enabled", false),
        GAS_STATIONS_CAN_USE_JERRYCAN_OUTSIDE_OF_GAS_STATION("gasStations.canUseJerryCanOutsideOfGasStation", true),
        GAS_STATIONS_FILL_JERRYCANS_ENABLED("gasStations.fillJerryCans.enabled", true),
        GAS_STATIONS_FILL_JERRYCANS_NEED_PERMISSION("gasStations.fillJerryCans.needPermission", false),
        GAS_STATIONS_FILL_JERRYCANS_PLAY_SOUND("gasStations.fillJerryCans.playSound", true),
        GAS_STATIONS_FILL_JERRYCANS_LEVER("gasStations.fillJerryCans.lever", true),
        GAS_STATIONS_FILL_JERRYCANS_TRIPWIRE_HOOK("gasStations.fillJerryCans.tripwireHook", false),
        GAS_STATIONS_FILL_JERRYCANS_PRICE_ENABLED("gasStations.fillJerryCans.price.enabled", true),
        GAS_STATIONS_FILL_JERRYCANS_PRICE_PER_LITRE("gasStations.fillJerryCans.price.pricePerLitre", 30.0),
        REGION_ACTIONS_PLACE("regionActions.place", "disabled"),
        REGION_ACTIONS_ENTER("regionActions.enter", "disabled"),
        REGION_ACTIONS_PICKUP("regionActions.pickup", "disabled");

        final private String path;
        final private Object defaultValue;

        private Option(String path, Object defaultValue){
            this.path = path;
            this.defaultValue = defaultValue;
        }

        /**
         * Get default value of option
         * @return Default value of option
         */
        public Object getDefaultValue() {
            return defaultValue;
        }

        /**
         * Get string path of option
         * @return Path of option
         */
        public String getPath() {
            return path;
        }

        /**
         * Get (default) type of option
         * @return Default type of option
         */
        public Type getValueType() {
            return this.getDefaultValue().getClass();
        }
    }

}

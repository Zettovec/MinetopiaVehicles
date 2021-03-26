package nl.mtvehicles.core.Infrastructure.Helpers;

import nl.mtvehicles.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class BossbarUtils {


    public static BossBar Benzine;

    public static double Teller;

    public static void setbossbarvalue(double counter, String ken) {
        if (Main.defaultConfig.getConfig().getBoolean("benzine") == true && Main.vehicleDataConfig.getConfig().getBoolean("vehicle."+ken+".benzineEnabled") == true) {
            Benzine.setProgress(counter);
            Benzine.setTitle(Math.round(counter * 100.0D) + "% " + TextUtils.colorize("&6Brandstofmeter"));
            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 30) {
                Benzine.setColor(BarColor.RED);
                return;
            }
            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 60) {
                Benzine.setColor(BarColor.YELLOW);
                return;
            }
            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 100) {
                Benzine.setColor(BarColor.GREEN);
                return;
            }
        }

    }

    public static double getbossbarvalue() {
        return Teller;
    }

    public static void removeBossbar(Player player, String ken) {
        if (Main.defaultConfig.getConfig().getBoolean("benzine") == true && Main.vehicleDataConfig.getConfig().getBoolean("vehicle."+ken+".benzineEnabled") == true) {
            Benzine.removePlayer(player);
        }
    }

    public static void addBossbar(Player player, String ken) {
        if (Main.defaultConfig.getConfig().getBoolean("benzine") == true && Main.vehicleDataConfig.getConfig().getBoolean("vehicle."+ken+".benzineEnabled") == true) {
            String benzineValue = String.valueOf(Main.vehicleDataConfig.getConfig().getDouble("vehicle." + player.getVehicle().getCustomName().replace("MTVEHICLES_MAINSEAT_", "") + ".benzine"));
            Benzine = Bukkit.createBossBar(Math.round(Double.parseDouble(benzineValue)) + "% " + TextUtils.colorize("&6Brandstofmeter"), BarColor.GREEN, BarStyle.SOLID);

            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 30) {
                Benzine.setColor(BarColor.RED);
            }
            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 60) {
                Benzine.setColor(BarColor.YELLOW);
            }
            if (Main.vehicleDataConfig.getConfig().getDouble("vehicle." + ken + ".benzine") < 100) {
                Benzine.setColor(BarColor.GREEN);
            }

            Benzine.addPlayer(player);
        }



    }

}

package com.wheezygold.happyserver.account;

import com.wheezygold.happyserver.account.commands.RankCommand;
import com.wheezygold.happyserver.account.commands.SubRankCommand;
import com.wheezygold.happyserver.common.Rank;
import com.wheezygold.happyserver.common.SQLManager;
import com.wheezygold.happyserver.common.SmallPlugin;
import com.wheezygold.happyserver.common.SubRank;
import com.wheezygold.happyserver.managers.HubManager;
import com.wheezygold.happyserver.util.Color;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.HashMap;

public class AccountManager extends SmallPlugin {

    private static AccountManager instance;
    private SQLManager sqlManager;
    private HubManager hubManager;
    private HashMap<String, HappyPlayer> activePlayers = new HashMap<>();

    public AccountManager(HubManager hubManager, SQLManager sqlManager, JavaPlugin plugin) {
        super("Account Manager", plugin);
        this.sqlManager = sqlManager;
        this.hubManager = hubManager;
        instance = this;
    }

    @Override
    public void addCommands() {
        addCommand(new RankCommand(this));
        addCommand(new SubRankCommand(this));
    }

    public boolean addPlayer(Player player) {
        if (activePlayers.containsKey(player.getName())) {
            activePlayers.remove(player.getName());
        }
        HappyPlayer newPlayer = new HappyPlayer(player, this);
        if (!sqlManager.isActive(player.getUniqueId().toString())) {
            if (!sqlManager.createUser(player.getUniqueId().toString(), player.getName())) {
                log("Not able to create user!");
                return false;
            }
        }
        try {
            newPlayer.setRank(Rank.valueOf(sqlManager.getRank(player.getUniqueId().toString())));
            newPlayer.setSubRank(SubRank.valueOf(sqlManager.getSubRank(player.getUniqueId().toString())));
        } catch (IllegalArgumentException | NullPointerException e) {
            log("Invalid (sub) rank at user: " + player.getName());
            return false;
        }
        newPlayer.setTokens(sqlManager.getTokens(player.getUniqueId().toString()));
        activePlayers.put(player.getName(), newPlayer);
        return true;
    }

    public boolean dropPlayer(String name) {
        if (!activePlayers.containsKey(name))
            return false;
        activePlayers.remove(name);
        return true;
    }

    public boolean contains(String playerName) {
        return activePlayers.containsKey(playerName);
    }

    public HappyPlayer fetch(String playerName) {
        return activePlayers.get(playerName);
    }

    public void update(HappyPlayer target) {
        if (target.getRank() == Rank.NONE) {
            target.getPlayer().setPlayerListName(Color.cWhite + target.getPlayer().getName());
        } else {
            target.getPlayer().setPlayerListName(target.getRank().getPrefix() + Color.cWhite + " " + target.getPlayer().getName());
        }
        sqlManager.updateUser(target);
        hubManager.updateSb(target);

    }

    public Connection getConnection() {
        return sqlManager.connection;
    }

    public static AccountManager getInstance() {
        return instance;
    }

}

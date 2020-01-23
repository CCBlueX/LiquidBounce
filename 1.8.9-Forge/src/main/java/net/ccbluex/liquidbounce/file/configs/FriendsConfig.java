/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import net.ccbluex.liquidbounce.file.FileConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FriendsConfig extends FileConfig {

    private final List<Friend> friends = new ArrayList<>();

    /**
     * Constructor of config
     *
     * @param file of config
     */
    public FriendsConfig(final File file) {
        super(file);
    }

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Override
    protected void loadConfig() throws IOException {
        clearFriends();

        final BufferedReader bufferedReader = new BufferedReader(new FileReader(getFile()));
        String line;
        while((line = bufferedReader.readLine()) != null) {
            if(!line.contains("{") && !line.contains("}")) {
                line = line.replace(" ", "").replace("\"", "").replace(",", "");

                if(line.contains(":")) {
                    String[] data = line.split(":");
                    addFriend(data[0], data[1]);
                }else
                    addFriend(line);
            }
        }
        bufferedReader.close();
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Override
    protected void saveConfig() throws IOException {
        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        for(final Friend friend : getFriends())
            printWriter.append(friend.getPlayerName()).append(":").append(friend.getAlias()).append("\n");
        printWriter.close();
    }

    /**
     * Add friend to config
     *
     * @param playerName of friend
     * @return of successfully added friend
     */
    public boolean addFriend(final String playerName) {
        return addFriend(playerName, playerName);
    }

    /**
     * Add friend to config
     *
     * @param playerName of friend
     * @param alias      of friend
     * @return of successfully added friend
     */
    public boolean addFriend(final String playerName, final String alias) {
        if(isFriend(playerName))
            return false;

        friends.add(new Friend(playerName, alias));
        return true;
    }

    /**
     * Remove friend from config
     *
     * @param playerName of friend
     */
    public boolean removeFriend(final String playerName) {
        if(!isFriend(playerName))
            return false;

        friends.removeIf(friend -> friend.getPlayerName().equals(playerName));
        return true;
    }

    /**
     * Check is friend
     *
     * @param playerName of friend
     * @return is friend
     */
    public boolean isFriend(final String playerName) {
        for(final Friend friend : friends)
            if(friend.getPlayerName().equals(playerName))
                return true;
        return false;
    }

    /**
     * Clear all friends from config
     */
    public void clearFriends() {
        friends.clear();
    }

    /**
     * Get friends
     *
     * @return list of friends
     */
    public List<Friend> getFriends() {
        return friends;
    }

    public class Friend {

        private final String playerName;
        private final String alias;

        /**
         * @param playerName of friend
         * @param alias      of friend
         */
        Friend(final String playerName, final String alias) {
            this.playerName = playerName;
            this.alias = alias;
        }

        /**
         * @return name of friend
         */
        public String getPlayerName() {
            return playerName;
        }

        /**
         * @return alias of friend
         */
        public String getAlias() {
            return alias;
        }
    }
}

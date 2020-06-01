/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils;

public class GitHubContributor {
    private String login;
    private long id;
    private int contributions;

    public String getLogin() {
        return login;
    }

    public long getId() {
        return id;
    }

    public int getContributions() {
        return contributions;
    }

    @Override
    public String toString() {
        return "GitHubContributor{" +
                "login='" + login + '\'' +
                ", id=" + id +
                ", contributions=" + contributions +
                '}';
    }
}

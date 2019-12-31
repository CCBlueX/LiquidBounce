package net.ccbluex.liquidbounce.utils.login;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public final class MinecraftAccount {

    private final String username;
    private String password;
    private String inGameName;

    public MinecraftAccount(final String username) {
        this.username = username;
    }

    public MinecraftAccount(final String name, final String password) {
        this.username = name;
        this.password = password;
    }

    public MinecraftAccount(final String name, final String password, final String inGameName) {
        this.username = name;
        this.password = password;
        this.inGameName = inGameName;
    }

    public boolean isCracked() {
        return password == null;
    }

    public String getName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAccountName() {
        return inGameName;
    }

    public void setAccountName(final String accountName) {
        this.inGameName = accountName;
    }
}

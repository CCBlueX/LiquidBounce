package net.mcleaks;

public class RedeemResponse {

    private final String username;
    private final String token;

    RedeemResponse(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return this.username;
    }

    public String getToken() {
        return this.token;
    }
}
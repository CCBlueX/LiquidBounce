package net.mcleaks;

public class Session {

    private final String username;
    private final String token;

    public Session(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}

public class User {
    public int id;
    public String login;
    public String password;   // В БД хранится SHA-256 хэш
    public String role;       // owner | user | job
    public String fullName;

    public User() {}

    public User(int id, String login, String password, String role, String fullName) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
    }

    public boolean isOwner() { return "owner".equalsIgnoreCase(role); }
    public boolean isJob()   { return "job".equalsIgnoreCase(role); }

    @Override
    public String toString() { return fullName + " (" + role + ")"; }
}
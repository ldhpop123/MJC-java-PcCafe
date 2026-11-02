package pccafe.dto;

public class Member {
    private String id;
    private String password;
    private String name;
    private String phone;
    private int remainSeconds;
    private boolean admin;

    public Member() {
    }

    public Member(String id, String password, String name, String phone) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.phone = phone;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getRemainSeconds() { return remainSeconds; }
    public void setRemainSeconds(int remainSeconds) { this.remainSeconds = remainSeconds; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}

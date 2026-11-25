package pccafe.dto;

public class Member {
    // 회원가입 때 고르는 비밀번호 찾기 질문
    public static final String[] PW_QUESTIONS = {
            "태어난 도시는?",
            "처음 키운 반려동물 이름은?",
            "가장 기억에 남는 선생님 이름은?",
            "가장 좋아하는 음식은?"
    };

    private String id;
    private String password;
    private String name;
    private String phone;
    private int remainSeconds;
    private boolean admin;
    private int point;
    private String pwQuestion;   // 관리자·예전 회원은 null
    private String pwAnswer;

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

    public int getPoint() { return point; }
    public void setPoint(int point) { this.point = point; }

    public String getPwQuestion() { return pwQuestion; }
    public void setPwQuestion(String pwQuestion) { this.pwQuestion = pwQuestion; }

    public String getPwAnswer() { return pwAnswer; }
    public void setPwAnswer(String pwAnswer) { this.pwAnswer = pwAnswer; }
}

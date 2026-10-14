public class order {
    int id;
    String name;
    String phone;
    String model;
    String issue;
    String urgency;
    String desc;
    String date;
    int progress;         // 0..100
    String clientLogin;   // логин клиента, который создал заявку
    int rating;           // 1..5 (0 = нет оценки)
    String feedback;      // комментарий клиента

    public order() {}

    public order(int id, String name, String phone, String model,
                 String issue, String urgency, String desc, String date) {
        this(id, name, phone, model, issue, urgency, desc, date, 0, "", 0, "");
    }

    public order(int id, String name, String phone, String model,
                 String issue, String urgency, String desc, String date,
                 int progress, String clientLogin, int rating, String feedback) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.model = model;
        this.issue = issue;
        this.urgency = urgency;
        this.desc = desc;
        this.date = date;
        this.progress = progress;
        this.clientLogin = clientLogin;
        this.rating = rating;
        this.feedback = feedback;
    }
}
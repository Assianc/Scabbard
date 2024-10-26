package com.example.memo;

public class Memo {
    private int id;
    private String title;
    private String content;
    private String timestamp;

    public Memo(int id, String title, String content, String timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    // 添加get和set方法
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

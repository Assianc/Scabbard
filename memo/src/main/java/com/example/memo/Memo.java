package com.example.memo;

public class Memo {
    private int id;
    private String title;
    private String content;
    private String timestamp;
    private String updateTime; // 新增字段

    // 修改构造方法，增加 updateTime 参数
    public Memo(int id, String title, String content, String timestamp, String updateTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.updateTime = updateTime; // 初始化
    }

    // Getter 和 Setter 方法
    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
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

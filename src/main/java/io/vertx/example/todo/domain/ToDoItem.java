package io.vertx.example.todo.domain;

import io.vertx.core.json.JsonObject;

/**
 * POJO class to store to-do item information
 */
public class ToDoItem {
    private String title;
    private boolean completed;
    private String url;
    private Integer order;

    public ToDoItem() {

    }

    public ToDoItem(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public static JsonObject toJsonObject(ToDoItem item) {
        return new JsonObject()
            .put("title", item.getTitle())
            .put("completed", item.completed)
            .put("url", item.getUrl())
            .put("order", item.getOrder());

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ToDoItem toDoItem = (ToDoItem) o;

        if (completed != toDoItem.completed) return false;
        if (title != null ? !title.equals(toDoItem.title) : toDoItem.title != null) return false;
        if (url != null ? !url.equals(toDoItem.url) : toDoItem.url != null) return false;
        return order != null ? order.equals(toDoItem.order) : toDoItem.order == null;

    }

}
